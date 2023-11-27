package cn.edu.buaa.scs.vm.vcenter

import cn.edu.buaa.scs.config.globalConfig
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.model.Host
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.utils.HttpClientWrapper
import cn.edu.buaa.scs.utils.schedule.waitForDone
import cn.edu.buaa.scs.vm.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.ktorm.jackson.KtormModule

object VCenterClient : IVMClient {

    internal val client by lazy {
        HttpClientWrapper(
            HttpClient(CIO) {
                defaultRequest {
                    header(HttpHeaders.Authorization, "Bearer ${globalConfig.vcenter.serviceToken}")
                }
                install(ContentNegotiation) {
                    jackson {
                        registerModule(KtormModule())
                    }
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 10000L
                }
            },
            basePath = globalConfig.vcenter.serviceUrl
        )
    }

    private fun vmNotFound(uuid: String): NotFoundException = NotFoundException("virtualMachine($uuid) not found")

    override suspend fun getHosts(): Result<List<Host>> {
        TODO("Not yet implemented")
    }

    override suspend fun getAllVMs(): Result<List<VirtualMachine>> = runCatching {
        client.get<List<VirtualMachine>>("/vms").getOrThrow()
    }

    override suspend fun getVM(uuid: String): Result<VirtualMachine> = runCatching {
        client.get<VirtualMachine>("/vm/$uuid").getOrThrow()
    }

    override suspend fun getVMByName(name: String, applyId: String): Result<VirtualMachine> = runCatching {
        getAllVMs().getOrElse { listOf() }.find { vm ->
            vm.name == name && vm.applyId == applyId
        } ?: throw vmNotFound(name)
    }

    override suspend fun powerOnSync(uuid: String): Result<Unit> = runCatching {
        client.post<String>("/vm/$uuid/powerOn")
        waitForDone(10000L) {
            getVM(uuid).getOrNull()?.powerState == VirtualMachine.PowerState.PoweredOn
        }
    }

    override suspend fun powerOnAsync(uuid: String) {
        client.post<String>("/vm/$uuid/powerOn")
    }

    override suspend fun powerOffSync(uuid: String): Result<Unit> = runCatching {
        client.post<String>("/vm/$uuid/powerOff")
        waitForDone(10000L) {
            getVM(uuid).getOrNull()?.powerState == VirtualMachine.PowerState.PoweredOff
        }
    }

    override suspend fun powerOffAsync(uuid: String) {
        client.post<String>("/vm/$uuid/powerOff")
    }

    override suspend fun configVM(
        uuid: String,
        experimentId: Int?,
        adminId: String?,
        teacherId: String?,
        studentId: String?,
    ): Result<VirtualMachine> = runCatching {
        val vm = getVM(uuid)
        val opt = ConfigVmOptions(
            vm.getOrThrow(),
            experimentId,
            adminId,
            teacherId,
            studentId,
        )
        client.post<VirtualMachine>("/vm/$uuid/config", opt).getOrThrow()
    }


    override suspend fun createVM(options: CreateVmOptions): Result<VirtualMachine> = runCatching {
        client.post<VirtualMachine>("/vms", options).getOrThrow()
    }

    override suspend fun deleteVM(uuid: String): Result<Unit> = runCatching {
        // 先关机
        try {
            powerOffSync(uuid)
        } catch (_: Throwable) {
        }
        // 然后删除
        client.delete<String>("/vm/$uuid")
    }

    override suspend fun convertVMToTemplate(uuid: String): Result<VirtualMachine> = runCatching {
        client.post<String>("/vm/$uuid/convertToTemplate").getOrThrow()
        getVM(uuid).getOrThrow()
    }

}
