package cn.edu.buaa.scs.vm.sangfor

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.cache.authRedis

import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.Host
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.applySangforExtraInfo
import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.getValueByKey
import cn.edu.buaa.scs.utils.jsonMapper
import cn.edu.buaa.scs.utils.schedule.waitForDone
import cn.edu.buaa.scs.utils.setExpireKey
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.vm.IVMClient
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import org.ktorm.jackson.KtormModule
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager

object SangforClient : IVMClient {
    val username = application.getConfigString("vm.sangfor.username")
    val password = application.getConfigString("vm.sangfor.password")
    val storageId = application.getConfigString("vm.sangfor.storage_id")
    val netDevice = application.getConfigString("vm.sangfor.net_device")
    private val tokenLock = Mutex()
    private val createLock = Mutex()

    internal val client by lazy {
        HttpClient(CIO) {
            defaultRequest {
                url(application.getConfigString("vm.sangfor.url"))
            }
            install(ContentNegotiation) {
                jackson {
                    registerModule(KtormModule())
                }
            }
            engine {
                https {
                    trustManager = object: X509TrustManager {
                        override fun checkClientTrusted(p0: Array<out X509Certificate>?, p1: String?) { }

                        override fun checkServerTrusted(p0: Array<out X509Certificate>?, p1: String?) { }

                        override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                    }
                }
            }
        }
    }

    private suspend fun connect(user: String, password: String): String {
        val response = client.post("openstack/identity/v2.0/tokens") {
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                    "auth": {
                        "tenantName": "$user",
                        "passwordCredentials": {
                            "username": "$user",
                            "password": "$password"
                        }
                    }
                }
                """.trimIndent()
            )
        }
        val body: String = response.body()
        return jsonMapper.readTree(body).get("access").get("token").get("id").toString().split('"')[1]
    }

    private suspend fun fetchTicket(token: String): Token {
        val resBody: String = client.get("summary") {
            header("Cookie", "aCMPAuthToken=$token")
        }.body()
        val ticket = jsonMapper.readTree(resBody)["data"]["ticket"].toString().split('"')[1]
        val sid = jsonMapper.readTree(resBody)["data"]["user"]["id"].toString().split('"')[1]
        return Token(token, ticket, sid)
    }

    private suspend fun getToken(): Token {
        tokenLock.lock()
        var token = authRedis.getValueByKey("sangfor_token")
        var ticket = authRedis.getValueByKey("sangfor_ticket") ?: ""
        var sid = authRedis.getValueByKey("sangfor_sid") ?: ""
        if (token == null) {
            token = connect(username, password)
            authRedis.setExpireKey("sangfor_token", token, 3500)
            val tokenBody = fetchTicket(token)
            ticket = tokenBody.ticket
            sid = tokenBody.sid
            authRedis.setExpireKey("sangfor_ticket", ticket, 4000)
            authRedis.setExpireKey("sangfor_sid", sid, 4000)
        }
        tokenLock.unlock()
        return Token(token, ticket, sid)
    }

    override suspend fun getHosts(): Result<List<Host>> {
        val token = getToken().id
        val clusterRes: String = client.get("admin/view/cluster-list") {
            header("Cookie", "aCMPAuthToken=${token}")
        }.body()
        val clusters = jsonMapper.readTree(clusterRes)["data"]
        val hostList = mutableListOf<Host>()
        for (cluster in clusters) {
            val cid = cluster["id"].toString().split('"')[1]
            val hostRes: String = client.get("admin/view/host-list?cluster_id=$cid") {
                header("Cookie", "aCMPAuthToken=${token}")
            }.body()
            val hosts = jsonMapper.readTree(hostRes)["data"]
            for (hostJSON in hosts) {
                val host = Host(
                    ip = hostJSON["ip"].toString().split('"')[1],
                    status = hostJSON["status"].toString().split('"')[1],
                    totalMem = hostJSON["memory"]["total_mb"].doubleValue(),
                    usedMem = hostJSON["memory"]["used_mb"].doubleValue(),
                    totalCPU = hostJSON["cpu"]["total_mhz"].doubleValue(),
                    usedCPU = hostJSON["cpu"]["used_mhz"].doubleValue(),
                    totalStorage = if (hostJSON["disks"].size() != 0) {
                        hostJSON["disks"].map {
                            it["disk_size_byte"].longValue()
                        }.reduce { s, s1 -> s + s1 }
                    } else { 0 },
                    usedStorage = 0L,
                    count = hostJSON["count"].intValue(),
                )
                hostList.add(host)
            }
        }
        return Result.success(hostList)
    }

    override suspend fun getAllVMs(): Result<List<VirtualMachine>> {
        // Get all virtual machines
        val token = getToken().id
        val vmsRes: String = client.get("openstack/compute/v2/servers") {
            header("X-Auth-Token", token)
        }.body()
        val vmList = mutableListOf<VirtualMachine>()
        val vms = jsonMapper.readTree(vmsRes).get("servers")
        runBlocking {
            val jobs = mutableListOf<Deferred<VirtualMachine>>()
            for (vmJSON in vms) {
                val job = async {
                    return@async getVM(vmJSON["id"].toString().split('"')[1]).getOrThrow()
                }
                jobs.add(job)
            }
            for (job in jobs) {
                vmList.add(job.await())
            }
        }
        return Result.success(vmList)
    }

    override suspend fun getVM(uuid: String): Result<VirtualMachine> {
        val token = getToken()
        val vmRes: String = client.get("admin/view/server-info?id=$uuid") {
            header("Cookie", "aCMPAuthToken=${token.id}")
        }.body()
        val vmJSON = jsonMapper.readTree(vmRes)
        val vm = VirtualMachine()
        vm.uuid = uuid
        vm.platform = "sangfor"
        vm.name = vmJSON["data"]["name"].toString().split('"')[1]
        vm.host = vmJSON["data"]["host_name"].toString().split('"')[1]
        vm.applySangforExtraInfo(vmJSON["data"]["description"].toString().split('"')[1])
        vm.memory = vmJSON["data"]["memory_mb"].intValue()
        vm.cpu = vmJSON["data"]["cores"].intValue()
        vm.osFullName = vmJSON["data"]["os_name"].toString().split('"')[1]
        vm.diskNum = vmJSON["data"]["disks"].size()
        vm.diskSize = vmJSON["data"]["disks"].map {
            it["size_mb"].longValue()
        }.reduce { s, s1 -> s + s1 } * 1048576L
        vm.powerState = VirtualMachine.PowerState.from(if (vmJSON["data"]["power_state"].toString().split('"')[1] == "on") "poweredon" else "poweredoff")
        vm.overallStatus = VirtualMachine.OverallStatus.from("green")
        vm.netInfos = vmJSON["data"]["networks"].map {
            VirtualMachine.NetInfo(it["mac"].toString().split('"')[1], listOf(it["ip"].toString().split('"')[1]))
        }
        return Result.success(vm)
    }
    
    override suspend fun getVMByName(name: String, applyId: String): Result<VirtualMachine> = runCatching {
        getAllVMs().getOrElse { listOf() }.find { vm ->
            vm.name == name && vm.applyId == applyId
        } ?: throw NotFoundException("virtualMachine($name) not found")
    }

    override suspend fun powerOnSync(uuid: String): Result<Unit> {
        powerOnAsync(uuid)
        return waitForDone(50000L, 500L) {
            val token = getToken()
            val vmRes: String = client.get("admin/view/server-info?id=$uuid") {
                header("Cookie", "aCMPAuthToken=${token.id}")
            }.body()
            val vmJSON = jsonMapper.readTree(vmRes)
            vmJSON["data"]["power_state"].toString() == "\"on\""
        }
    }

    override suspend fun powerOnAsync(uuid: String) {
        val token = getToken().id
        /* 成功：202，失败：409 */
        client.post("janus/20180725/servers/$uuid/start") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token $token")
        }
    }

    override suspend fun powerOffSync(uuid: String): Result<Unit> {
        powerOffAsync(uuid)
        return waitForDone(50000L, 500L) {
            val token = getToken()
            val vmRes: String = client.get("admin/view/server-info?id=$uuid") {
                header("Cookie", "aCMPAuthToken=${token.id}")
            }.body()
            val vmJSON = jsonMapper.readTree(vmRes)
            vmJSON["data"]["power_state"].toString() == "\"off\""
        }
    }

    override suspend fun powerOffAsync(uuid: String) {
        val token = getToken().id
        /* 成功：202，失败：409 */
        client.post("janus/20180725/servers/$uuid/stop") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token $token")
        }
    }

    suspend fun createVNCConsole(uuid: String): Result<String> {
        val token = getToken().id
        /* 成功：202，失败：409 */
        val res: String = client.post("janus/20180725/servers/$uuid/remote-consoles") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token $token")
            setBody(
                """
                {
                    "remote_console": {
                        "protocol": "vnc",
                        "type": "novnc"
                    }
                }
                """.trimIndent()
            )
        }.body()
        val json = jsonMapper.readTree(res)
        return Result.success(json["remote_console"]["url"].toString())
    }

    override suspend fun configVM(
        uuid: String,
        experimentId: Int?,
        adminId: String?,
        teacherId: String?,
        studentId: String?
    ): Result<VirtualMachine> {
        val token = getToken()
        val vmRes: String = client.get("admin/view/server-info?id=$uuid") {
            header("Cookie", "aCMPAuthToken=${token.id}")
        }.body()
        val vmJSON = jsonMapper.readTree(vmRes)["data"]
        val oldDescription = vmJSON["description"].toString().split('"')[1]
        var owner = "default"
        teacherId?.let {
            if (it != "default") owner = it
        }
        studentId?.let {
            if (it != "default") owner = it
        }
        val info = oldDescription.split(',')
        var eid = experimentId
        if (eid == null) {
            eid = if (info.size == 4) info[2].toInt() else -1
        }
        var applyId = ""
        if (info.size == 4) applyId = info[3]
        val description = "$owner,false,$eid,$applyId"
        client.put("janus/20180725/servers/$uuid") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${token.id}")
            setBody(
                """
                {
                    "server_id": "$uuid",
                    "description": "$description"
                }
                """.trimIndent()
            )
        }
        return getVM(uuid)
    }

    override suspend fun createVM(options: CreateVmOptions): Result<VirtualMachine> {
        createLock.lock()
        // Send clone vm request.
        val owner = if (options.extraInfo.teacherId != "default") options.extraInfo.teacherId
                    else if (options.extraInfo.studentId != "default") options.extraInfo.studentId
                    else "default"
        val description = "$owner,false,${options.extraInfo.experimentId},${options.extraInfo.applyId}"
        clone(
            options.name,
            options.extraInfo.templateUuid,
            description
        )
        // Wait the creation be done.
        var token: Token
        var uuid = ""
        waitForDone(20000L, 500L) {
            token = getToken()
            val vmsRes: String = client.get("openstack/compute/v2/servers/detail") {
                header("X-Auth-Token", token.id)
            }.body()
            val vms = jsonMapper.readTree(vmsRes)["servers"]
            for (vmJSON in vms) {
                if (vmJSON["OS-EXT-STS:task_state"].toString() == "\"creating\"") {
                    uuid = vmJSON["id"].toString().split('"')[1]
                }
            }
            uuid != ""
        }
        waitForDone(300000L, 1000L) {
            token = getToken()
            val vmsRes: String = client.get("openstack/compute/v2/servers/detail") {
                header("X-Auth-Token", token.id)
            }.body()
            val vms = jsonMapper.readTree(vmsRes)["servers"]
            var done = false
            for (vmJSON in vms) {
                if (vmJSON["id"].toString() == "\"$uuid\"") {
                    if (vmJSON["OS-EXT-STS:task_state"].toString() == "\"\"") {
                        done = true
                    }
                    break
                }
            }
            done
        }
        createLock.unlock()
        // Initialize settings of the new virtual machine.
        changeSettings(
            uuid,
            options.name,
            description,
            options.memory,
            options.cpu,
            options.diskSize / 1048576L,
        )
        if(options.powerOn) powerOnAsync(uuid)
        return getVM(uuid)
    }

    /* The virtual machine must be powered off. */
    override suspend fun deleteVM(uuid: String): Result<Unit> {
        /* 成功：200，失败：409 */
        val token = getToken()
        val resCode = client.delete("janus/20180725/servers/$uuid") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${token.id}")
            setBody(
                """
                {
                    "force": 1
                }
                """.trimIndent()
            )
        }.status.value
        return Result.success(Unit)
    }

    /* The virtual machine must be powered off. */
    override suspend fun convertVMToTemplate(uuid: String): Result<VirtualMachine> {
        val token = getToken()
        val vmRes: String = client.get("admin/view/server-info?id=$uuid") {
            header("Cookie", "aCMPAuthToken=${token.id}")
        }.body()
        val vmJSON = jsonMapper.readTree(vmRes)["data"]
        val info = vmJSON["description"].toString().split('"')[1].split(',')
        var description = "default,true,-1,"
        if (info.size == 4) description = "${info[0]},true,${info[2]},${info[3]}"
        client.put("janus/20180725/servers/$uuid") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${token.id}")
            setBody(
                """
                {
                    "server_id": "$uuid",
                    "description": "$description"
                }
                """.trimIndent()
            )
        }
        return getVM(uuid)
    }

    suspend fun convertVMTemplateWithOwner(uuid: String, owner: String, isTemplate: Boolean): Result<VirtualMachine> {
        val token = getToken()
        val vmRes: String = client.get("admin/view/server-info?id=$uuid") {
            header("Cookie", "aCMPAuthToken=${token.id}")
        }.body()
        val vmJSON = jsonMapper.readTree(vmRes)["data"]
        val info = vmJSON["description"].toString().split('"')[1].split(',')
        var description = "default,$isTemplate,-1,"
        if (info.size == 4) description = "$owner,$isTemplate,${info[2]},${info[3]}"
        client.put("janus/20180725/servers/$uuid") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${token.id}")
            setBody(
                """
                {
                    "server_id": "$uuid",
                    "description": "$description"
                }
                """.trimIndent()
            )
        }
        return getVM(uuid)
    }

    suspend fun clone(name: String,
                      templateUuid: String,
                      description: String
    ) {
        val token = getToken()
        val resCode = client.post("admin/servers/$templateUuid/clone-servers") {
            contentType(ContentType.Application.Json)
            header("Cookie", "aCMPAuthToken=${token.id}")
            header("CSRFPreventionToken", token.ticket)
            header("sid", token.sid)
            setBody(
                """
                {
                    "batch_server_info": {
                        "name": "$name",
                        "description": "$description",
                        "location_type": "storage_tag",
                        "location": {
                            "storage_tag_id": "11111111-1111-1111-1111-111111111111"
                        },
                        "power_on": 0,
                        "hci_param": {},
                        "count": 1
                    }
                }
                """.trimIndent()
            )
        }.status.value
    }

    /* The virtual machine must be powered off. */
    suspend fun initSettings(uuid: String,
                               name: String,
                               description: String,
                               memory: Int,
                               cores: Int,
                               disk: Long,
                               oldSetting: OldSetting
    ) {
        val token = getToken()
        val resCode = client.put("admin/servers/$uuid") {
            contentType(ContentType.Application.Json)
            header("Cookie", "aCMPAuthToken=${token.id}")
            header("CSRFPreventionToken", token.ticket)
            header("sid", token.sid)
            setBody(
                """
                {
                    "server": {
                        "hci_param": {
                            "schedopt": 0,
                            "hugepage_memory": 0,
                            "use_vblk": 1,
                            "boot_order": "dc",
                            "cpu_hotplug": 0,
                            "balloon_memory": 0,
                            "use_uuid": 0,
                            "abnormal_recovery": 1,
                            "mem_hotplug": 0,
                            "cpu_type": "core2duo",
                            "real_use_vblk": 1,
                            "onboot": 0,
                            "dir": "71dc87680938",
                            "boot_disk": "ide0",
                            "regen_uuid": 0
                        },
                        "name": "$name",
                        "description": "$description",
                        "memory_mb": $memory,
                        "cores": $cores,
                        "sockets": 1,
                        "disks": [
                            {
                                "id": "ide0",
                                "type": "new_disk",
                                "preallocate": "metadata",
                                "size_mb": $disk,
                                "is_old_disk": 1,
                                "storage_file": "3600d0231000859694803abfa3b686284:vm-disk-1.qcow2"
                            }
                        ],
                        "networks": [
                            {
                                "network": "dvs66d81f0",
                                "name": "默认经典网络出口1-出口交换机",
                                "id": "net0",
                                "mac": "${oldSetting.mac}",
                                "connect": 1,
                                "model": "virtio",
                                "port": "12345678",
                                "host_tso": 0
                            }
                        ],
                        "usbs": [],
                        "os_type": "${oldSetting.osType}",
                        "compute_location": {
                            "id": "cluster",
                            "location": 0
                        },
                        "storage_location": "3600d0231000859694803abfa3b686284",
                        "cdroms": []
                    },
                    "old_server": {
                        "hci_param": {
                            "schedopt": 0,
                            "hugepage_memory": 0,
                            "use_vblk": 1,
                            "boot_order": "dc",
                            "cpu_hotplug": 0,
                            "balloon_memory": 0,
                            "use_uuid": 0,
                            "abnormal_recovery": 1,
                            "mem_hotplug": 0,
                            "cpu_type": "core2duo",
                            "real_use_vblk": 1,
                            "onboot": 0,
                            "dir": "71dc87680938",
                            "boot_disk": "ide0"
                        },
                        "name": "${oldSetting.name}",
                        "group_id": null,
                        "description": "${oldSetting.description}",
                        "memory_mb": ${oldSetting.memory},
                        "cores": ${oldSetting.cores},
                        "sockets": 1,
                        "disks": [
                            {
                                "preallocate": "metadata",
                                "storage_name": "iscsi",
                                "id": "ide0",
                                "size_mb": ${oldSetting.disk},
                                "use_virtio": 1,
                                "storage_file": "3600d0231000859694803abfa3b686284:vm-disk-1.qcow2",
                                "type": "new_disk",
                                "is_old_disk": 1
                            }
                        ],
                        "networks": [
                            {
                                "id": "net0",
                                "host_tso": 0,
                                "mac": "${oldSetting.mac}",
                                "model": "virtio",
                                "connect": 0
                            }
                        ],
                        "usbs": [],
                        "os_type": "${oldSetting.osType}",
                        "compute_location": {
                            "location": 0,
                            "policy_type": "",
                            "id": "cluster"
                        },
                        "storage_location": "3600d0231000859694803abfa3b686284",
                        "cdroms": []
                    }
                }
                """.trimIndent()
            )
        }.status.value
    }

    suspend fun changeSettings(uuid: String,
                               name: String,
                               description: String,
                               memory: Int,
                               cores: Int,
                               disk: Long
    ) {
        val token = getToken()
        val resCode = client.put("janus/20180725/servers/$uuid") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Token ${token.id}")
            setBody(
                """
                {
                    "server_id": "$uuid",
                    "name": "$name",
                    "description": "$description",
                    "cores": $cores,
                    "memory_mb": $memory,
                    "disks": [
                        {
                            "id": "ide0",
                            "type": "derive_disk",
                            "is_old_disk": 1,
                            "storage_file": "$storageId:vm-disk-1.qcow2",
                            "preallocate": 0,
                            "size_mb": $disk
                        }
                    ],
                    "networks": [
                        {
                            "vif_id": "net0",
                            "connect": 1,
                            "device_id": "$netDevice",
                            "name": "默认经典网络出口1-出口交换机",
                            "model": "virtio",
                            "port_id": "12345678"
                        }
                    ]
                }
                """.trimIndent()
            )
        }
    }
}

data class Token(
    val id: String,
    val ticket: String,
    val sid: String
)

data class OldSetting(
    val name: String,
    val description: String,
    val memory: Int,
    val cores: Int,
    val disk: Long,
    val mac: String,
    val osType: String
)
