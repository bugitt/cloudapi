package cn.edu.buaa.scs.service

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import io.ktor.application.*

interface IService {
    abstract class Caller<T : IService> {
        private val svcMap: Cache<ApplicationCall, T> = CacheBuilder.newBuilder().weakKeys().build()
        fun getSvc(call: ApplicationCall, create: (ApplicationCall) -> T): T {
            return svcMap.get(call) { create(call) }
        }
    }
}