package cn.edu.buaa.scs.vcenter

import cn.edu.buaa.scs.config.globalConfig
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachineExtraInfo
import cn.edu.buaa.scs.model.applyExtraInfo
import cn.edu.buaa.scs.model.TicketResponse
import cn.edu.buaa.scs.utils.jsonMapper
import cn.edu.buaa.scs.utils.logger
import cn.edu.buaa.scs.vm.ConfigVmOptions
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.model.Host
import com.fasterxml.jackson.module.kotlin.readValue
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.BasicConnection
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.Connection
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.WaitForValues
import com.vmware.vim25.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.net.URI
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

typealias TaskFunc = suspend (Connection) -> Unit

object VCenterWrapper {
    class TrustAllTrustManager : TrustManager, X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> {
            return arrayOf()
        }
    }

    private lateinit var vcenterConnect: () -> Connection

    fun initialize() {
        vcenterConnect = {
            val res = BasicConnection()
            res.uri = URI(globalConfig.vcenter.entrypoint)
            res.password = globalConfig.vcenter.password
            res.isIgnoreSslErrors = true
            res.trustManager = TrustAllTrustManager()
            res.username = globalConfig.vcenter.username
            res.setRequestTimeout(1000000, TimeUnit.MILLISECONDS)
            res.connect()
            res
        }
        start()
    }

    private val taskChannel = Channel<TaskFunc>(1000)

    private fun start() {
        Thread {
            val connectionPool = mutableMapOf<Int, Connection>()
            runBlocking {
                withContext(Dispatchers.IO) {
                    // launch 30 workers to receive and handle task
                    repeat(30) { channelNum ->
                        connectionPool[channelNum] = vcenterConnect()
                        launch {
                            for (taskFunc in taskChannel) {
                                val connection = connectionPool[channelNum] ?: vcenterConnect()
                                try {
                                    taskFunc(connection)
                                } catch (e: Throwable) {
                                    logger("vm-worker-$channelNum")().error { e.stackTraceToString() }
                                } finally {
                                    connectionPool[channelNum] = connection
                                }
                            }
                        }
                    }
                }
            }
        }.start()
    }

    private var getAllVmsConnection: Connection? = null
    fun getAllVms(): Result<List<VirtualMachine>> {
        if (getAllVmsConnection == null) {
            getAllVmsConnection = vcenterConnect()
        }
        var vms: List<VirtualMachine>? = null
        try {
            vms = getAllVmsFromVCenter(getAllVmsConnection!!)
        } catch (_: Throwable) {
            getAllVmsConnection = vcenterConnect()
            try {
                getAllVmsConnection = vcenterConnect()
                vms = getAllVmsFromVCenter(getAllVmsConnection!!)
            } catch (e: Throwable) {
                logger("vm-worker-$getAllVmsConnection")().error { e.stackTraceToString() }
            }
        }
        return Result.success(vms ?: listOf())
    }

    suspend fun powerOn(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val task = connection.vimPort.powerOnVMTask(connection.getVmRefByUuid(uuid), null)
            waitForTaskResult(connection, task).getOrThrow()
        }
    }

    suspend fun powerOff(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val task = connection.vimPort.powerOffVMTask(connection.getVmRefByUuid(uuid))
            waitForTaskResult(connection, task).getOrThrow()
        }
    }

    suspend fun getHosts(): List<Host> {
        return baseSyncTask { connection ->
            val datacenterRef = connection.getDatacenterRef()
            val getMoRef = connection.getMoRef()
            val hostList =
                getMoRef.inContainerByType(datacenterRef, "HostSystem", arrayOf("name", "summary", "datastore", "vm"), RetrieveOptions())
            hostList.map { (hostRef, hostProps) ->
                val hostSummary = hostProps["summary"]!! as HostListSummary
                val hostHardwareSummary = hostSummary.hardware
                var usedMem = 0.0
                var usedCPU = 0.0
                if (hostSummary.runtime.connectionState === HostSystemConnectionState.CONNECTED) {
                    usedMem = 1.0 * hostSummary.quickStats.overallMemoryUsage
                    usedCPU = 1.0 * hostSummary.quickStats.overallCpuUsage
                }
                val dataStores = (hostProps["datastore"] as ArrayOfManagedObjectReference?)!!.managedObjectReference
                val (usedStorage, totalStorage) = dataStores.map { getMoRef.entityProps(it, "summary")["summary"]!! as DatastoreSummary }
                    .filter { it.isAccessible }
                    .map { Pair(it.capacity - it.freeSpace, it.capacity) }
                    .reduceOrNull { (a, b), (a1, b1) -> Pair(a + a1, b + b1) } ?: Pair(0L, 0L)
                Host(
                    ip = hostProps["name"]!! as String,
                    status = hostSummary.runtime.connectionState.value(),
                    totalMem = 1.0 * hostHardwareSummary.memorySize,
                    usedMem = usedMem,
                    totalCPU = 1.0 * hostHardwareSummary.cpuMhz * hostHardwareSummary.numCpuCores,
                    usedCPU = usedCPU,
                    totalStorage = totalStorage,
                    usedStorage = usedStorage,
                    count = (hostProps["vm"]!! as ArrayOfManagedObjectReference).managedObjectReference.size,
                )
            }
        }.getOrThrow()
    }

    suspend fun configVM(
        uuid: String,
        opt: ConfigVmOptions,
    ): Result<VirtualMachine> {
        return baseSyncTask { connection ->
            configVM(
                connection,
                connection.getVmRefByUuid(uuid)!!,
                opt.vm,
                experimentId = opt.experimentId,
                adminId = opt.adminId,
                teacherId = opt.teacherId,
                studentId = opt.studentId,
            ).getOrThrow()
        }
    }

    private fun configVM(
        connection: Connection,
        vmRef: ManagedObjectReference,
        vm: VirtualMachine,
        experimentId: Int? = null,
        adminId: String? = null,
        teacherId: String? = null,
        studentId: String? = null,
    ): Result<VirtualMachine> {
        val vmConfigSpec = VirtualMachineConfigSpec()
        var vmExtraInfo = VirtualMachineExtraInfo.valueFromVirtualMachine(vm)
        experimentId?.let {
            vmExtraInfo = vmExtraInfo.copy(
                experimental = true,
                experimentId = it
            )
        }
        adminId?.let { vmExtraInfo = vmExtraInfo.copy(adminId = it) }
        teacherId?.let { vmExtraInfo = vmExtraInfo.copy(teacherId = it) }
        studentId?.let { vmExtraInfo = vmExtraInfo.copy(studentId = it) }
        vmConfigSpec.annotation = vmExtraInfo.toJson()
        val task = connection.vimPort.reconfigVMTask(vmRef, vmConfigSpec)
        try {
            waitForTaskResult(connection, task).getOrThrow()
        } catch (e: Throwable) {
            return Result.failure(e)
        }
        vm.applyExtraInfo(vmExtraInfo)
        return Result.success(vm)
    }

    private fun getAllVmsFromVCenter(connection: Connection): List<VirtualMachine> {
        val logger = logger("get-all-vms")()
        logger.info { "try to fetch virtual machine list from vcenter......" }
        val datacenterRef = connection.getDatacenterRef()
        val getMoRef = connection.getMoRef()
        val hostList =
            getMoRef.inContainerByType(datacenterRef, "HostSystem", arrayOf("name"), RetrieveOptions())
        val finalVirtualMachineList = mutableListOf<VirtualMachine?>()
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
                    try {
                        val vm = convertVMModel(hostName, vmProps)
                        finalVirtualMachineList.add(vm)
                    } catch (e: NullPointerException) {
                        logger.error { e }
                    }
                }
            } catch (e: Throwable) {
                logger.error { e.stackTraceToString() }
            }
        }
        logger.info { "done:) fetch virtual machine list from vcenter" }
        return finalVirtualMachineList.filterNotNull()
    }

    suspend fun create(options: CreateVmOptions): Result<VirtualMachine> {
        return baseSyncTask { connection ->
            val task = clone(
                connection,
                options.name,
                options.extraInfo.templateUuid,
                options.extraInfo,
                options.cpu,
                options.memory,
                options.diskSize,
                options.powerOn,
            )
            waitForTaskResult(connection, task).getOrThrow()
            getAllVmsFromVCenter(connection).find { vm ->
                vm.name == options.name && vm.applyId == options.extraInfo.applyId
            } ?: throw Exception("can not found vm ${options.name}")
        }
    }

    private fun clone(
        connection: Connection,
        name: String,
        templateUuid: String,
        vmExtraInfo: VirtualMachineExtraInfo,
        cpuNum: Int,
        memoryMb: Int,
        diskSizeBytes: Long,
        powerOn: Boolean,
    ): ManagedObjectReference {
        val datacenterRef = connection.getDatacenterRef()
        val getMoRef = connection.getMoRef()
        val vimPort = connection.vimPort
        /*
        * 配置虚拟机克隆的目标位置信息
        * 在主机列表中找到第一个磁盘空间充足的主机作为目标位置
        * */
        val relocateSpec = VirtualMachineRelocateSpec()
        val hostList =
            getMoRef.inContainerByType(
                datacenterRef,
                "HostSystem",
                arrayOf("datastore", "summary", "name"),
                RetrieveOptions()
            )
        var host: ManagedObjectReference? = null
        var datastore: ManagedObjectReference? = null
        val diskRequired = Long.MIN_VALUE
        hostList.forEach { (hostRef, hostProps) ->
            val hostSummary = hostProps["summary"]!! as HostListSummary
            if (hostSummary.runtime.connectionState === HostSystemConnectionState.CONNECTED) {
                val dataStores = (hostProps["datastore"] as ArrayOfManagedObjectReference?)!!.managedObjectReference
                for (ds in dataStores) {
                    val datastoreSummary = getMoRef.entityProps(ds, "summary")["summary"]!! as DatastoreSummary
                    println(datastoreSummary.name)
                    if (datastoreSummary.name == "Dell SCV101") {
                        continue
                    }
                    if (datastoreSummary.isAccessible && datastoreSummary.freeSpace / (1024 * 1024) > 1024 * 1024 && datastoreSummary.freeSpace > diskRequired) {
                        host = hostRef
                        datastore = datastoreSummary.datastore
                    }
                }
            }
        }
        if (host == null) {
            throw IllegalAccessError("error :无符合条件的物理主机，无法创建虚拟机。")
        }
        val crmor = getMoRef.entityProps(host, "parent")["parent"]!! as ManagedObjectReference
        relocateSpec.host = host
        relocateSpec.pool = getMoRef.entityProps(crmor, "resourcePool")["resourcePool"] as ManagedObjectReference
        relocateSpec.datastore = datastore
        // 找到待克隆的虚拟机模板
        val templateRef = connection.getVmRefByUuid(templateUuid)
        /*
        * 配置虚拟机的元信息
        * 元信息包括：CPU核心数、内存大小、磁盘大小、拥有者id、使用者id、是否为实验虚拟机等
        * */
        val configSpec = VirtualMachineConfigSpec()
        configSpec.numCoresPerSocket = cpuNum
        configSpec.numCPUs = cpuNum
        configSpec.memoryMB = memoryMb.toLong()
        var diskConfig: VirtualDiskConfigSpec
        val devices = (getMoRef.entityProps(templateRef, "config.hardware.device")["config.hardware.device"]!!
                as ArrayOfVirtualDevice).virtualDevice
        for (vd in devices) {
            if (vd is VirtualDisk) {
                if (vd.capacityInKB < diskSizeBytes / 1024) {
                    vd.capacityInKB = diskSizeBytes / 1024
                }
                diskConfig = VirtualDiskConfigSpec()
                diskConfig.device = vd
                diskConfig.operation = VirtualDeviceConfigSpecOperation.EDIT
                configSpec.deviceChange.add(diskConfig)
            }
        }
        configSpec.annotation = vmExtraInfo.toJson()
        val cloneSpec = VirtualMachineCloneSpec()
        cloneSpec.location = relocateSpec
        cloneSpec.isPowerOn = powerOn
        cloneSpec.isTemplate = false
        cloneSpec.config = configSpec
        return vimPort.cloneVMTask(templateRef, connection.getCreateVmSubFolder(), name, cloneSpec)
    }

    suspend fun delete(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val vmRef = connection.getVmRefByUuid(uuid)
            val task = connection.vimPort.destroyTask(vmRef)
            waitForTaskResult(connection, task).getOrThrow()
        }
    }

    suspend fun getVM(uuid: String): Result<VirtualMachine> = runCatching {
        baseSyncTask { connection ->
            val vmRef = connection.getVmRefByUuid(uuid)
            val vmProps = connection.getMoRef().entityProps(vmRef, "summary", "config.hardware.device", "guest.net")

            val vmSummary = vmProps["summary"]!! as VirtualMachineSummary
            val hostRef = vmSummary.runtime.host
            val hostProps = connection.getMoRef().entityProps(hostRef, "name")
            val hostName = hostProps["name"]!! as String
            convertVMModel(hostName, vmProps) ?: throw Exception("can not found vm $uuid")
        }.getOrThrow()
    }

    suspend fun getWebTicket(uuid: String): TicketResponse {
        return baseSyncTask { connection ->
            val vmRef = connection.getVmRefByUuid(uuid)
            val vmTicket = connection.vimPort.acquireTicket(vmRef, "webmks")
            TicketResponse(
                ticket = vmTicket.ticket,
                host = vmTicket.host
            )
        }.getOrThrow()
    }

    suspend fun convertVMToTemplate(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val vmRef = connection.getVmRefByUuid(uuid)
            connection.vimPort.markAsTemplate(vmRef)
        }
    }

    private suspend fun <T> baseSyncTask(action: suspend (Connection) -> T): Result<T> {
        val resultChannel = Channel<Result<T>>()
        taskChannel.send { connection ->
            try {
                resultChannel.send(Result.success(action(connection)))
            } catch (e: Throwable) {
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
}

internal fun convertVMModel(
    hostName: String,
    vmProps: Map<String, Any>,
): VirtualMachine? {
    val vmSummary = vmProps["summary"]!! as VirtualMachineSummary
    val guestNicInfoList = vmProps["guest.net"]!! as ArrayOfGuestNicInfo
    val devices = vmProps["config.hardware.device"] as ArrayOfVirtualDevice? ?: return null
    return convertVMModel(hostName, vmSummary, guestNicInfoList, devices)
}

internal fun convertVMModel(
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

    vm.powerState = VirtualMachine.PowerState.from(vmSummary.runtime.powerState.value())
    vm.overallStatus = VirtualMachine.OverallStatus.from(vmSummary.overallStatus.value())

    vm.netInfos = guestNicInfoList.guestNicInfo.map {
        VirtualMachine.NetInfo(it.macAddress, it.ipAddress)
    }

    val extraInfo: VirtualMachineExtraInfo = try {
        jsonMapper.readValue(vmConfig.annotation)
    } catch (e: Exception) {
        VirtualMachineExtraInfo()
    }
    vm.applyExtraInfo(extraInfo)

    return vm
}
