package cn.edu.buaa.scs.vm.vcenter

import cn.edu.buaa.scs.error.BadRequestException
import cn.edu.buaa.scs.error.NotFoundException
import cn.edu.buaa.scs.error.RemoteServiceException
import cn.edu.buaa.scs.model.VirtualMachine
import cn.edu.buaa.scs.utils.Constants
import cn.edu.buaa.scs.utils.HttpClientWrapper
import cn.edu.buaa.scs.vm.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import org.ktorm.dsl.and
import org.ktorm.dsl.eq
import org.ktorm.jackson.KtormModule

object VCenterClient : IVMClient {

    internal val client by lazy {
        HttpClientWrapper(HttpClient(CIO) {
            defaultRequest {
                url {
                    protocol = URLProtocol.HTTP
                    host = "127.0.0.1"
                    port = Constants.VCenter.port
                    path("/api/v2/vcenter/")
                }

            }
            install(ContentNegotiation) {
                jackson {
                    registerModule(KtormModule())
                }
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 100000L
            }
        })
    }

    private fun vmNotFound(uuid: String): NotFoundException = NotFoundException("virtualMachine($uuid) not found")

    override suspend fun getAllVMs(): Result<List<VirtualMachine>> = runCatching {
        client.get<List<VirtualMachine>>("vms").getOrThrow()
    }

    override suspend fun getVM(uuid: String): Result<VirtualMachine> {
        val vmResult = client.get<VirtualMachine>("vm/$uuid")
        return if (vmResult.isSuccess) {
            vmResult
        } else {
            vmResult.exceptionOrNull()?.let {
                if (it is RemoteServiceException && it.status == HttpStatusCode.NotFound.value) {
                    Result.failure(vmNotFound(uuid))
                } else {
                    Result.failure(it)
                }
            } ?: Result.failure(vmNotFound(uuid))
        }
    }

    override suspend fun powerOnSync(uuid: String): Result<Unit> = runCatching {
        client.post<String>("/vm/$uuid/powerOn")
    }

    override suspend fun powerOnAsync(uuid: String) {
        client.post<String>("/vm/$uuid/powerOn")
    }

    override suspend fun powerOffSync(uuid: String): Result<Unit> = runCatching {
        client.post<String>("/vm/$uuid/powerOff")
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
        // 首先检查是不是有同名vm
        if (options.existInDb()) {
            return Result.failure(BadRequestException("there is already a VirtualMachine with the same name"))
        }

        client.post<String>("vms", options).getOrThrow()

        // wait to find the vm in db
        waitForVMInDB(options.existPredicate()).getOrThrow()
    }

    override suspend fun deleteVM(uuid: String): Result<Unit> {
        // 先关机
        try {
            powerOffSync(uuid)
        } catch (_: Throwable) {
        }
        // 然后删除
        return client.delete("/vm/$uuid")
    }

    override suspend fun convertVMToTemplate(uuid: String): Result<VirtualMachine> = runCatching {
        client.post<String>("vm/$uuid/convertToTemplate").getOrThrow()
        waitForVMInDB {
            it.uuid eq uuid and it.isTemplate eq true
        }.getOrThrow()
    }

}
