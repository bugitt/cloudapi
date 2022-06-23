package cn.edu.buaa.scs.vm.vcenter

import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachines
import cn.edu.buaa.scs.model.virtualMachines
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.jsonMapper
import cn.edu.buaa.scs.utils.logger
import cn.edu.buaa.scs.vm.IVMClient
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.BasicConnection
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.Connection
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.GetMoRef
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.WaitForValues
import com.vmware.vim25.*
import io.ktor.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.ktorm.dsl.batchInsert
import org.ktorm.dsl.batchUpdate
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import org.ktorm.entity.map
import java.net.URI
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

typealias TaskFunc = suspend (Connection) -> Unit

object VCenterClient : IVMClient {

    private lateinit var vcenterConnect: () -> Connection

    private const val detention = 500L

    fun initialize(application: Application) {
        val entrypoint = application.getConfigString("vm.vcenter.entrypoint")
        val username = application.getConfigString("vm.vcenter.username")
        val password = application.getConfigString("vm.vcenter.password")
        vcenterConnect = {
            val res = BasicConnection()
            res.uri = URI(entrypoint)
            res.password = password
            res.isIgnoreSslErrors = true
            res.trustManager = TrustAllTrustManager()
            res.username = username
            res.setRequestTimeout(1000000, TimeUnit.MILLISECONDS)
            res.connect()
            res
        }
        start()
    }

    private val taskChannel = Channel<TaskFunc>(100)

    private fun start() {
        Thread() {
            runBlocking {
                withContext(Dispatchers.IO) {
                    // launch 20 workers to receive and handle task
                    repeat(20) {
                        launch {
                            for (taskFunc in taskChannel) {
                                var connection: Connection? = null
                                try {
                                    connection = vcenterConnect()
                                    taskFunc(connection)
                                } catch (e: Exception) {
                                    logger("vm-worker-$it")().error { e.stackTraceToString() }
                                } finally {
                                    connection?.close()
                                }
                            }
                        }
                    }

                    // launch a coroutine to update database
                    launch {
                        val connection = vcenterConnect()
                        while (true) {
                            // update vmList to database
                            try {
                                updateVMsToDB(getAllVmsFromVCenter(connection))
                            } catch (e: Exception) {
                                logger("vm-worker-update-db")().error { e.stackTraceToString() }
                            }
                            delay(500L)
                        }
                    }
                }
            }
        }.start()
    }

    private fun updateVMsToDB(vmList: List<VirtualMachine>) {
        val existedVmUUIDList = mysql.virtualMachines.map { it.uuid }.toSet()
        mysql.useTransaction {
            // update
            mysql.batchUpdate(VirtualMachines) {
                vmList.filter { it.uuid in existedVmUUIDList }.forEach { vm ->
                    item {
                        set(it.platform, vm.platform)
                        set(it.name, vm.name)
                        set(it.isTemplate, vm.isTemplate)
                        set(it.host, vm.host)
                        set(it.adminId, vm.adminId)
                        set(it.studentId, vm.studentId)
                        set(it.teacherId, vm.teacherId)
                        set(it.isExperimental, vm.isExperimental)
                        set(it.applyId, vm.applyId)
                        set(it.memory, vm.memory)
                        set(it.cpu, vm.cpu)
                        set(it.osFullName, vm.osFullName)
                        set(it.diskNum, vm.diskNum)
                        set(it.diskSize, vm.diskSize)
                        set(it.powerState, vm.powerState)
                        set(it.overallStatus, vm.overallStatus)
                        set(it.netInfos, vm.netInfos)
                        where { it.uuid eq vm.uuid }
                    }
                }
            }

            // create
            mysql.batchInsert(VirtualMachines) {
                vmList.filterNot { it.uuid in existedVmUUIDList }.forEach { vm ->
                    item {
                        set(it.uuid, vm.uuid)
                        set(it.platform, vm.platform)
                        set(it.name, vm.name)
                        set(it.isTemplate, vm.isTemplate)
                        set(it.host, vm.host)
                        set(it.adminId, vm.adminId)
                        set(it.studentId, vm.studentId)
                        set(it.teacherId, vm.teacherId)
                        set(it.isExperimental, vm.isExperimental)
                        set(it.applyId, vm.applyId)
                        set(it.memory, vm.memory)
                        set(it.cpu, vm.cpu)
                        set(it.osFullName, vm.osFullName)
                        set(it.diskNum, vm.diskNum)
                        set(it.diskSize, vm.diskSize)
                        set(it.powerState, vm.powerState)
                        set(it.overallStatus, vm.overallStatus)
                        set(it.netInfos, vm.netInfos)
                    }
                }
            }
        }
    }

    override suspend fun getAllVMs(): Result<List<VirtualMachine>> {
        return baseSyncTask { connection ->
            getAllVmsFromVCenter(connection)
        }
    }

    override suspend fun getVM(uuid: String): Result<VirtualMachine> {
        var vm = mysql.virtualMachines.find { it.uuid eq uuid }
        if (vm == null) {
            delay(detention)
            vm = mysql.virtualMachines.find { it.uuid eq uuid }
        }
        return if (vm == null) Result.failure(NotFoundException("virtualMachine($uuid) not found"))
        else Result.success(vm)
    }

    override suspend fun powerOnSync(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val vimPort = connection.vimPort
            val vmRef = vimPort.findByUuid(
                connection.serviceContent.searchIndex,
                getDatacenter(connection),
                uuid,
                true,
                false
            )
            val task = vimPort.powerOnVMTask(vmRef, null)
            waitForTaskResult(connection, task)
        }
    }

    override suspend fun powerOnAsync(uuid: String) {
        TODO("Not yet implemented")
    }

    override suspend fun powerOffSync(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val vimPort = connection.vimPort
            val vmRef = vimPort.findByUuid(
                connection.serviceContent.searchIndex,
                getDatacenter(connection),
                uuid,
                true,
                false
            )
            val task = vimPort.powerOffVMTask(vmRef)
            waitForTaskResult(connection, task)
        }
    }

    override suspend fun powerOffAsync(uuid: String) {
        TODO("Not yet implemented")
    }

    private fun getDatacenter(connection: Connection): ManagedObjectReference {
        return connection.vimPort.findByInventoryPath(connection.serviceContent.searchIndex, "datacenter")
    }

    private fun getAllVmsFromVCenter(connection: Connection): List<VirtualMachine> {
        val datacenterRef = getDatacenter(connection)
        val getMoRef = GetMoRef(connection)
        val hostList =
            getMoRef.inContainerByType(datacenterRef, "HostSystem", arrayOf("name"), RetrieveOptions())
        val finalVirtualMachineList = mutableListOf<VirtualMachine>()
        hostList.forEach { (hostRef, hostProps) ->
            try {
                val hostName = hostProps["name"]!! as String
                val vmList = getMoRef.inContainerByType(
                    hostRef,
                    "VirtualMachine",
                    arrayOf("summary", "config.hardware.device", "guest.net"),
                    RetrieveOptions()
                )
                vmList?.forEach { (_, vmProps) ->
                    val vmSummary = vmProps["summary"]!! as VirtualMachineSummary
                    val guestNicInfoList = vmProps["guest.net"]!! as ArrayOfGuestNicInfo
                    val devices = vmProps["config.hardware.device"]!! as ArrayOfVirtualDevice
                    val vm = convertVMModel(hostName, vmSummary, guestNicInfoList, devices)
                    finalVirtualMachineList.add(vm)
                }
            } catch (e: Exception) {
                logger("get-all-vms")().error { e.stackTraceToString() }
            }
        }
        return finalVirtualMachineList
    }

    private suspend fun <T> baseSyncTask(action: suspend (Connection) -> T): Result<T> {
        val resultChannel = Channel<Result<T>>()
        taskChannel.send { connection ->
            try {
                resultChannel.send(Result.success(action(connection)))
            } catch (e: Exception) {
                resultChannel.send(Result.failure(e))
            } finally {
                resultChannel.close()
            }
        }
        return resultChannel.receive()
    }

    private fun waitForTaskResult(connection: Connection, task: ManagedObjectReference): Result<Unit> {
        val waitForValues = WaitForValues(connection)
        val result: Array<Any> = waitForValues.wait(
            task,
            arrayOf("info.state", "info.error"),
            arrayOf("info.state"),
            arrayOf(arrayOf<Any>(TaskInfoState.SUCCESS, TaskInfoState.ERROR))
        )
        return if (result[0] == TaskInfoState.SUCCESS) {
            return Result.success(Unit)
        } else {
            when (val fault = result[1]) {
                is LocalizedMethodFault -> Result.failure(RuntimeException(fault.localizedMessage))
                else -> Result.failure(RuntimeException("unknown error"))
            }
        }
    }

    class TrustAllTrustManager : TrustManager, X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }

    }
}

fun convertVMModel(
    host: String,
    vmSummary: VirtualMachineSummary,
    guestNicInfoList: ArrayOfGuestNicInfo,
    devices: ArrayOfVirtualDevice
): VirtualMachine {
    val vm = VirtualMachine()
    vm.platform = "vcenter"

    // vm summary
    val vmConfig = vmSummary.config
    vm.name = vmConfig.name
    vm.uuid = vmConfig.uuid
    vm.isTemplate = vmConfig.isTemplate
    vm.host = host
    vm.memory = vmConfig.memorySizeMB
    vm.cpu = vmConfig.numCpu
    vm.osFullName = vmConfig.guestFullName
    vm.diskNum = vmConfig.numVirtualDisks

    vm.diskSize = devices.virtualDevice.map {
        if (it is VirtualDisk) {
            it.capacityInBytes.toLong()
        } else {
            0L
        }
    }.reduce { acc, l -> acc + l }

    vm.powerState = vmSummary.runtime.powerState
    vm.overallStatus = vmSummary.overallStatus

    vm.netInfos = guestNicInfoList.guestNicInfo.map {
        VirtualMachine.NetInfo(it.macAddress, it.ipAddress)
    }

    val extraInfo = vmConfig.annotation?.let { jsonMapper.readTree(it) }
    vm.adminId = extraInfo?.get("adminID")?.asText() ?: "default"
    vm.studentId = extraInfo?.get("studentID")?.asText() ?: "default"
    vm.teacherId = extraInfo?.get("teacherID")?.asText() ?: "default"
    vm.isExperimental = extraInfo?.get("isExperimental")?.asBoolean() ?: false
    vm.experimentId = extraInfo?.get("experimentID")?.asInt() ?: 0
    vm.applyId = extraInfo?.get("applyID")?.asText() ?: "default"

    return vm
}