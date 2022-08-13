package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.model.VirtualMachine
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
import net.schmizz.sshj.SSHClient
import net.schmizz.sshj.connection.channel.Channel
import net.schmizz.sshj.connection.channel.direct.Session
import net.schmizz.sshj.transport.verification.PromiscuousVerifier
import java.io.Closeable
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.math.min

lateinit var sshConfig: SSHConfig

data class SSHConfig(
    val privateKeyLocation: String,
    val username: String,
)

class SSH(
    private val sshClient: SSHClient,
    private val session: Session,
    private val channel: Channel
) : Closeable {

    companion object {
        fun initSSHConfig(privateKey: String, username: String): SSHConfig {
            val file = File("/tmp/privateKey")
            file.writeText(privateKey)
            return SSHConfig(privateKeyLocation = file.absolutePath, username = username)
        }

        private fun connect(ip: String, port: Int = 22, initCommand: String? = null): Result<SSH> =
            try {
                val sshClient = SSHClient()
                sshClient.addHostKeyVerifier(PromiscuousVerifier())
                sshClient.connect(ip, port)
                sshClient.authPublickey(sshConfig.username, sshConfig.privateKeyLocation)
                val session = sshClient.startSession()
                session.allocateDefaultPTY()
                val channel = initCommand?.let { session.exec(it) } ?: session.startShell()
                Result.success(SSH(sshClient, session, channel))
            } catch (e: Throwable) {
                Result.failure(e)
            }

        fun vmGetSSH(vm: VirtualMachine): SSH? {
            val ipList = vm.netInfos.map { it.ipList }.flatten()
            for (ip in ipList) {
                val ssh = connect(ip)
                if (ssh.isSuccess) return ssh.getOrNull()
            }
            return null
        }
    }

    override fun close() {
        session.close()
        sshClient.disconnect()
    }

    suspend fun readCommand(incoming: ReceiveChannel<Frame>) {
        incoming.consumeEach { frame ->
            if (frame is Frame.Text) {
                val text = frame.readText()
                text.forEach { char -> channel.outputStream.write(char.code) }
                channel.outputStream.flush()
            }
        }
    }

    suspend fun processOutput(outgoing: SendChannel<Frame>) {
        val data = ByteArray(1024)
        val bytesRead = readInputStreamWithTimeout(channel.inputStream, data)
        if (bytesRead > 0) {
            val text = String(data, 0, bytesRead)
            outgoing.send(Frame.Text(text))
        }
    }

    fun isActive(): Boolean = channel.isOpen

    @Throws(IOException::class)
    private fun readInputStreamWithTimeout(
        inputStream: InputStream,
        buffer: ByteArray,
        timeoutMillis: Int = 2000
    ): Int {
        var offset = 0
        val maxTimeMillis = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < maxTimeMillis && offset < buffer.size) {
            val readLength = min(inputStream.available(), buffer.size - offset)

            if (readLength == 0) {
                break
            }

            // can alternatively use bufferedReader, guarded by isReady():
            val bytesRead = inputStream.read(buffer, offset, readLength)
            if (bytesRead == -1) {
                break
            }
            offset += bytesRead
        }
        return offset
    }
}
