package cn.edu.buaa.scs.utils

import com.mongodb.reactivestreams.client.ClientSession
import org.litote.kmongo.coroutine.CoroutineClient

suspend fun <T> CoroutineClient.useTransaction(action: (ClientSession) -> T): T {
    return this.startSession().use { session ->
        session.startTransaction()
        val t = action(session)
        session.commitTransaction()
        t
    }
}