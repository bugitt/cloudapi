package cn.edu.buaa.scs.vm.vcenter

import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.model.VirtualMachineExtraInfo
import cn.edu.buaa.scs.model.virtualMachines
import cn.edu.buaa.scs.storage.mysql
import cn.edu.buaa.scs.utils.getConfigString
import cn.edu.buaa.scs.utils.logger
import cn.edu.buaa.scs.vm.CreateVmOptions
import cn.edu.buaa.scs.vm.IVMClient
import cn.edu.buaa.scs.vm.applyExtraInfo
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.BasicConnection
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.Connection
import com.vmware.photon.controller.model.adapters.vsphere.util.connection.WaitForValues
import com.vmware.vim25.*
import io.ktor.application.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.ktorm.dsl.eq
import org.ktorm.entity.find
import java.net.URI
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


typealias TaskFunc = suspend (Connection) -> Unit

object VCenterClient : IVMClient {

    private lateinit var vcenterConnect: () -> Connection

    private const val detention = 500L

    internal fun vmNotFound(uuid: String): NotFoundException = NotFoundException("virtualMachine($uuid) not found")

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

    private val taskChannel = Channel<TaskFunc>(1000)

    private fun start() {
        Thread {
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
                                } catch (e: Throwable) {
                                    logger("vm-worker-$it")().error { e.stackTraceToString() }
                                } finally {
                                    connection?.close()
                                }
                            }
                        }
                    }
                }
            }
        }.start()
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
        return if (vm == null) Result.failure(vmNotFound(uuid))
        else Result.success(vm)
    }

    override suspend fun powerOnSync(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val task = connection.vimPort.powerOnVMTask(connection.getVmRefByUuid(uuid), null)
            waitForTaskResult(connection, task).getOrThrow()
        }
    }

    override suspend fun powerOnAsync(uuid: String) {
        taskChannel.send { connection ->
            connection.vimPort.powerOnVMTask(connection.getVmRefByUuid(uuid), null)
        }
    }

    override suspend fun powerOffSync(uuid: String): Result<Unit> {
        return baseSyncTask { connection ->
            val task = connection.vimPort.powerOffVMTask(connection.getVmRefByUuid(uuid))
            waitForTaskResult(connection, task).getOrThrow()
        }
    }

    override suspend fun powerOffAsync(uuid: String) {
        taskChannel.send { connection ->
            connection.vimPort.powerOffVMTask(connection.getVmRefByUuid(uuid))
        }
    }

    override suspend fun configVM(uuid: String, experimentId: Int?): Result<Unit> {
        return baseSyncTask { connection ->
            val vmRef = connection.getVmRefByUuid(uuid)
            val vm = getVM(uuid).getOrThrow()
            val vmConfigSpec = VirtualMachineConfigSpec()
            val vmExtraInfo = VirtualMachineExtraInfo.valueFromVirtualMachine(vm)
            experimentId?.let { vmExtraInfo.experimentId = it; vmExtraInfo.isExperimental = true }
            vmConfigSpec.annotation = vmExtraInfo.toJson()
            val task = connection.vimPort.reconfigVMTask(vmRef, vmConfigSpec)
            waitForTaskResult(connection, task).getOrThrow()
        }
    }

    override suspend fun createVM(options: CreateVmOptions): Result<VirtualMachine> {
        // 首先检查是不是有同名vm
        if (options.existInDb()) {
            return Result.failure(BadRequestException("there is already a VirtualMachine with the same name"))
        }
        return baseSyncTask { connection ->
            val task = clone(
                connection,
                options.name,
                options.templateUuid,
                VirtualMachineExtraInfo(
                    options.adminId,
                    options.studentId,
                    options.teacherId,
                    options.isExperimental,
                    options.experimentId,
                    options.applyId,
                ),
                options.cpu,
                options.memory,
                options.diskSize,
                options.powerOn,
            )
            waitForTaskResult(connection, task).getOrThrow()
            // wait to find the vm in db
            withTimeout(500000L) {
                while (!options.existInDb()) {
                    delay(10L)
                }
                mysql.virtualMachines.find(options.existPredicate())!!
            }
        }
    }

    override suspend fun deleteVM(uuid: String): Result<Unit> {
        // 先关机
        try {
            powerOffSync(uuid)
        } catch (_: Throwable) {
        }
        // 然后删除
        return baseSyncTask { connection ->
            val vmRef = connection.getVmRefByUuid(uuid)
            val task = connection.vimPort.destroyTask(vmRef)
            waitForTaskResult(connection, task).getOrThrow()
        }
    }

    private fun getAllVmsFromVCenter(connection: Connection): List<VirtualMachine> {
        val logger = logger("get-all-vms")()
        logger.info { "try to fetch virtual machine list from vcenter......" }
        val datacenterRef = connection.getDatacenterRef()
        val getMoRef = connection.getMoRef()
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
            } catch (e: Throwable) {
                logger.error { e.stackTraceToString() }
            }
        }
        logger.info { "done:) fetch virtual machine list from vcenter" }
        return finalVirtualMachineList
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
        /*
        * 配置虚拟机的自定义策略信息
        * 根据Windows和Linux分类讨论
        * */
        var customSpec: CustomizationSpec? = null
        val templateSummary = getMoRef.entityProps(templateRef, "summary")["summary"]!! as VirtualMachineSummary
        if (templateSummary.config.guestId.startsWith("centos") || templateSummary.config.guestId.startsWith("fedora") ||
            templateSummary.config.guestId.startsWith("freebsd") || templateSummary.config.guestId.startsWith("ubuntu")
        ) {
            customSpec = vimPort.getCustomizationSpec(connection.serviceContent.customizationSpecManager, "open").spec
        } else if (templateSummary.config.guestId.startsWith("win")) {
            customSpec = vimPort.getCustomizationSpec(connection.serviceContent.customizationSpecManager, "group").spec
        }
        val cloneSpec = VirtualMachineCloneSpec()
        cloneSpec.location = relocateSpec
        cloneSpec.isPowerOn = powerOn
        cloneSpec.isTemplate = false
        cloneSpec.config = configSpec
        cloneSpec.customization = customSpec
        return vimPort.cloneVMTask(templateRef, connection.getCreateVmSubFolder(), name, cloneSpec)
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

    vm.powerState = vmSummary.runtime.powerState
    vm.overallStatus = vmSummary.overallStatus

    vm.netInfos = guestNicInfoList.guestNicInfo.map {
        VirtualMachine.NetInfo(it.macAddress, it.ipAddress)
    }

    vm.applyExtraInfo(VirtualMachineExtraInfo.valueFromJson(vmConfig.annotation))

    return vm
}