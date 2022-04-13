package cn.edu.buaa.scs.vm

import cn.edu.buaa.scs.utils.getConfigString
import io.ktor.application.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.content.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.net.URLDecoder
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

object OldVmClient : IVmClient {
    internal lateinit var svcPath: String

    private val client: HttpClient by lazy {
        HttpClient(CIO) {
            engine {
                maxConnectionsCount = 1000
                endpoint {
                    maxConnectionsPerRoute = 100
                    pipelineMaxSize = 20
                    keepAliveTime = 5000
                    connectTimeout = 5000
                    connectAttempts = 5
                }

            }
            defaultRequest {
                header("SOAPAction", "\"\"")
            }
        }
    }

    private suspend fun <T> baseAction(req: String, respTag: String, convert: (String) -> T): T {
        val resp = client.post<String>(svcPath) {
            body = TextContent(req, contentType = ContentType.Text.Xml)
        }
        val builder: DocumentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc = withContext(Dispatchers.IO) {
            ByteArrayInputStream(resp.toByteArray(Charsets.UTF_8)).use {
                builder.parse(it)
            }
        }
        val node = doc.getElementsByTagName(respTag).item(0)
        return convert(URLDecoder.decode(node.textContent, Charsets.UTF_8))
    }

    suspend fun getVmInfo(name: String): String {
        return baseAction(
            """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:vcm="http://vm.manager.buaa.edu.cn/VCManager">
                   <soapenv:Header/>
                   <soapenv:Body>
                      <vcm:GetVMInfo>
                         <name>$name</name>
                      </vcm:GetVMInfo>
                   </soapenv:Body>
                </soapenv:Envelope>
            """.trimIndent(),
            "GetVMInfoReturn"
        ) {
            it
        }
    }
}

@Suppress("unused")
fun Application.oldVmClientModule() {
    val endpoint = getConfigString("oldVmManager.endpoint")
    OldVmClient.svcPath = endpoint
}