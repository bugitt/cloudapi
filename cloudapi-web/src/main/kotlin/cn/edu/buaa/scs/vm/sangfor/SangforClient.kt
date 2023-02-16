package cn.edu.buaa.scs.vm.sangfor

import cn.edu.buaa.scs.application
import cn.edu.buaa.scs.cache.authRedis
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
import org.ktorm.jackson.KtormModule
import java.security.cert.X509Certificate
import java.util.concurrent.locks.ReentrantLock
import javax.net.ssl.X509TrustManager

object SangforClient : IVMClient {
    val username = application.getConfigString("vm.sangfor.username")
    val password = application.getConfigString("vm.sangfor.password")
    private val tokenLock = ReentrantLock()
    private val createLock = ReentrantLock()

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

    suspend fun connect(): String {
        val response = client.post("openstack/identity/v2.0/tokens") {
            contentType(ContentType.Application.Json)
            setBody(
                "{\n" +
                "    \"auth\": {\n" +
                "        \"tenantName\": \"$username\",\n" +
                "        \"passwordCredentials\": {\n" +
                "            \"username\": \"$username\",\n" +
                "            \"password\": \"$password\"\n" +
                "        }\n" +
                "    }\n" +
                "}"
            )
        }
        val body: String = response.body()
        return jsonMapper.readTree(body).get("access").get("token").get("id").toString().split('"')[1]
    }

    suspend fun fetchTicket(token: String): Token {
        val resBody: String = client.get("summary") {
            header("Cookie", "aCMPAuthToken=$token")
        }.body()
        val ticket = jsonMapper.readTree(resBody)["data"]["ticket"].toString().split('"')[1]
        val sid = jsonMapper.readTree(resBody)["data"]["user"]["id"].toString().split('"')[1]
        return Token(token, ticket, sid)
    }

    suspend fun getToken(): Token {
        tokenLock.lock()
        var token = authRedis.getValueByKey("sangfor_token")
        var ticket = authRedis.getValueByKey("sangfor_ticket") ?: ""
        var sid = authRedis.getValueByKey("sangfor_sid") ?: ""
        if (token == null) {
            token = connect()
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
        client.post("openstack/compute/v2/servers/$uuid/action") {
            contentType(ContentType.Application.Json)
            header("X-Auth-Token", token)
            setBody(
                "{\n" +
                "    \"os-start\": null\n" +
                "}"
            )
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
        client.post("openstack/compute/v2/servers/$uuid/action") {
            contentType(ContentType.Application.Json)
            header("X-Auth-Token", token)
            setBody(
                "{\n" +
                "    \"os-stop\": null\n" +
                "}"
            )
        }
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
        val oldSetting = OldSetting(
            vmJSON["name"].toString().split('"')[1],
            vmJSON["description"].toString().split('"')[1],
            vmJSON["memory_mb"].intValue(),
            vmJSON["cores"].intValue(),
            vmJSON["data"]["disks"].map {
                it["size_mb"].longValue()
            }.reduce { s, s1 -> s + s1 },
            vmJSON["networks"][0]["mac"].toString().split('"')[1].lowercase(),
            vmJSON["os_type"].toString().split('"')[1],
        )
        var owner = "default"
        teacherId?.let {
            if (it != "default") owner = it
        }
        studentId?.let {
            if (it != "default") owner = it
        }
        val info = oldSetting.description.split(',')
        var eid = experimentId
        if (eid == null) {
            eid = if (info.size == 4) info[2].toInt() else -1
        }
        var applyId = ""
        if (info.size == 4) applyId = info[3]
        val description = "$owner,false,$eid,$applyId"
        initSettings(
            uuid,
            oldSetting.name,
            description,
            oldSetting.memory,
            oldSetting.cores,
            oldSetting.disk,
            oldSetting
        )
        return getVM(uuid)
    }

    override suspend fun createVM(options: CreateVmOptions): Result<VirtualMachine> {
        createLock.lock()
        // Send clone vm request.
        val owner = if (options.teacherId != "default") options.teacherId
                    else if (options.studentId != "default") options.studentId
                    else "default"
        val description = "$owner,false,${options.experimentId},${options.applyId}"
        clone(
            options.name,
            options.templateUuid,
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
//                println(vmJSON.toString())
                if (vmJSON["OS-EXT-STS:task_state"].toString() == "\"creating\"") {
                    uuid = vmJSON["id"].toString().split('"')[1]
                }
            }
            uuid != ""
        }
//        println("uuid is $uuid")
        waitForDone(300000L, 1000L) {
            token = getToken()
            val vmRes: String = client.get("openstack/compute/v2/servers/$uuid") {
                header("X-Auth-Token", token.id)
            }.body()
//            println(vmRes)
//            println(jsonMapper.readTree(vmRes)["server"]["OS-EXT-STS:task_state"].toString())
            jsonMapper.readTree(vmRes)["server"]["OS-EXT-STS:task_state"].toString() == "\"\""
        }
        createLock.unlock()
        // Initialize settings of the new virtual machine.
        token = getToken()
        val vmRes: String = client.get("admin/view/server-info?id=$uuid") {
            header("Cookie", "aCMPAuthToken=${token.id}")
        }.body()
        val vmJSON = jsonMapper.readTree(vmRes)["data"]
        println(vmJSON.toString())
        val oldSetting = OldSetting(
            vmJSON["name"].toString().split('"')[1],
            vmJSON["description"].toString().split('"')[1],
            vmJSON["memory_mb"].intValue(),
            vmJSON["cores"].intValue(),
            vmJSON["disks"].map {
                it["size_mb"].longValue()
            }.reduce { s, s1 -> s + s1 },
            vmJSON["networks"][0]["mac"].toString().split('"')[1].lowercase(),
            vmJSON["os_type"].toString().split('"')[1],
        )
        initSettings(
            uuid,
            oldSetting.name,
            oldSetting.description,
            options.memory,
            options.cpu,
            options.diskSize / 1048576L,
            oldSetting
        )
        if(options.powerOn) powerOnAsync(uuid)
        return getVM(uuid)
    }

    /* The virtual machine must be powered off. */
    override suspend fun deleteVM(uuid: String): Result<Unit> {
        /* 成功：204，失败：409 */
        val token = getToken()
        val resCode = client.delete("openstack/compute/v2/servers/$uuid") {
            contentType(ContentType.Application.Json)
            header("X-Auth-Token", token.id)
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
        val oldSetting = OldSetting(
            vmJSON["name"].toString().split('"')[1],
            vmJSON["description"].toString().split('"')[1],
            vmJSON["memory_mb"].intValue(),
            vmJSON["cores"].intValue(),
            vmJSON["data"]["disks"].map {
                it["size_mb"].longValue()
            }.reduce { s, s1 -> s + s1 },
            vmJSON["networks"][0]["mac"].toString().split('"')[1].lowercase(),
            vmJSON["os_type"].toString().split('"')[1],
        )
        val info = vmJSON["description"].toString().split('"')[1].split(',')
        var description = "default,true,-1,"
        if (info.size == 4) description = "${info[0]},true,${info[2]},${info[3]}"
        initSettings(
            uuid,
            oldSetting.name,
            description,
            oldSetting.memory,
            oldSetting.cores,
            oldSetting.disk,
            oldSetting
        )
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
                "{\n" +
                "    \"batch_server_info\": {\n" +
                "        \"name\": \"$name\",\n" +
                "        \"description\": \"$description\",\n" +
                "        \"location_type\": \"storage_tag\",\n" +
                "        \"location\": {\n" +
                "            \"storage_tag_id\": \"11111111-1111-1111-1111-111111111111\"\n" +
                "        },\n" +
                "        \"power_on\": 0,\n" +
                "        \"hci_param\": {},\n" +
                "        \"count\": 1\n" +
                "    }\n" +
                "}"
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
                "{\n" +
                "    \"server\": {\n" +
                "        \"hci_param\": {\n" +
                "            \"schedopt\": 0,\n" +
                "            \"hugepage_memory\": 0,\n" +
                "            \"use_vblk\": 1,\n" +
                "            \"boot_order\": \"dc\",\n" +
                "            \"cpu_hotplug\": 0,\n" +
                "            \"balloon_memory\": 0,\n" +
                "            \"use_uuid\": 0,\n" +
                "            \"abnormal_recovery\": 1,\n" +
                "            \"mem_hotplug\": 0,\n" +
                "            \"cpu_type\": \"core2duo\",\n" +
                "            \"real_use_vblk\": 1,\n" +
                "            \"onboot\": 0,\n" +
                "            \"dir\": \"71dc87680938\",\n" +
                "            \"boot_disk\": \"ide0\",\n" +
                "            \"regen_uuid\": 0\n" +
                "        },\n" +
                "        \"name\": \"$name\",\n" +
                "        \"description\": \"$description\",\n" +
                "        \"memory_mb\": $memory,\n" +
                "        \"cores\": $cores,\n" +
                "        \"sockets\": 1,\n" +
                "        \"disks\": [\n" +
                "            {\n" +
                "                \"id\": \"ide0\",\n" +
                "                \"type\": \"new_disk\",\n" +
                "                \"preallocate\": \"metadata\",\n" +
                "                \"size_mb\": $disk,\n" +
                "                \"is_old_disk\": 1,\n" +
                "                \"storage_file\": \"3600d0231000859694803abfa3b686284:vm-disk-1.qcow2\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"networks\": [\n" +
                "            {\n" +
                "                \"network\": \"dvs66d81f0\",\n" +
                "                \"name\": \"默认经典网络出口1-出口交换机\",\n" +
                "                \"id\": \"net0\",\n" +
                "                \"mac\": \"${oldSetting.mac}\",\n" +
                "                \"connect\": 1,\n" +
                "                \"model\": \"virtio\",\n" +
                "                \"port\": \"12345678\",\n" +
                "                \"host_tso\": 0\n" +
                "            }\n" +
                "        ],\n" +
                "        \"usbs\": [],\n" +
                "        \"os_type\": \"${oldSetting.osType}\",\n" +
                "        \"compute_location\": {\n" +
                "            \"id\": \"cluster\",\n" +
                "            \"location\": 0\n" +
                "        },\n" +
                "        \"storage_location\": \"3600d0231000859694803abfa3b686284\",\n" +
                "        \"cdroms\": []\n" +
                "    },\n" +
                "    \"old_server\": {\n" +
                "        \"hci_param\": {\n" +
                "            \"schedopt\": 0,\n" +
                "            \"hugepage_memory\": 0,\n" +
                "            \"use_vblk\": 1,\n" +
                "            \"boot_order\": \"dc\",\n" +
                "            \"cpu_hotplug\": 0,\n" +
                "            \"balloon_memory\": 0,\n" +
                "            \"use_uuid\": 0,\n" +
                "            \"abnormal_recovery\": 1,\n" +
                "            \"mem_hotplug\": 0,\n" +
                "            \"cpu_type\": \"core2duo\",\n" +
                "            \"real_use_vblk\": 1,\n" +
                "            \"onboot\": 0,\n" +
                "            \"dir\": \"71dc87680938\",\n" +
                "            \"boot_disk\": \"ide0\"\n" +
                "        },\n" +
                "        \"name\": \"${oldSetting.name}\",\n" +
                "        \"group_id\": null,\n" +
                "        \"description\": \"${oldSetting.description}\",\n" +
                "        \"memory_mb\": ${oldSetting.memory},\n" +
                "        \"cores\": ${oldSetting.cores},\n" +
                "        \"sockets\": 1,\n" +
                "        \"disks\": [\n" +
                "            {\n" +
                "                \"preallocate\": \"metadata\",\n" +
                "                \"storage_name\": \"iscsi\",\n" +
                "                \"id\": \"ide0\",\n" +
                "                \"size_mb\": ${oldSetting.disk},\n" +
                "                \"use_virtio\": 1,\n" +
                "                \"storage_file\": \"3600d0231000859694803abfa3b686284:vm-disk-1.qcow2\",\n" +
                "                \"type\": \"new_disk\",\n" +
                "                \"is_old_disk\": 1\n" +
                "            }\n" +
                "        ],\n" +
                "        \"networks\": [\n" +
                "            {\n" +
                "                \"id\": \"net0\",\n" +
                "                \"host_tso\": 0,\n" +
                "                \"mac\": \"${oldSetting.mac}\",\n" +
                "                \"model\": \"virtio\",\n" +
                "                \"connect\": 0\n" +
                "            }\n" +
                "        ],\n" +
                "        \"usbs\": [],\n" +
                "        \"os_type\": \"${oldSetting.osType}\",\n" +
                "        \"compute_location\": {\n" +
                "            \"location\": 0,\n" +
                "            \"policy_type\": \"\",\n" +
                "            \"id\": \"cluster\"\n" +
                "        },\n" +
                "        \"storage_location\": \"3600d0231000859694803abfa3b686284\",\n" +
                "        \"cdroms\": []\n" +
                "    }\n" +
                "}"
            )
        }.status.value
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
