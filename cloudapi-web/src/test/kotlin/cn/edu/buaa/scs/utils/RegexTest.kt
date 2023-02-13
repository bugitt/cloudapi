package cn.edu.buaa.scs.utils

import org.junit.jupiter.api.Test


internal class RegexTest {
    @Test
    fun fetchFromSSOValidateXML() {
        val regex = Regex("<cas:user>(.*)</cas:user>")
        val matchResult = regex.find(
            """
            <cas:serviceResponse xmlns:cas='http://www.yale.edu/tp/cas'>
    <cas:authenticationSuccess>
        <cas:user>SY2121108</cas:user>
        <cas:attributes>
            <cas:employeeNumber>SY2121108</cas:employeeNumber>
            </cas:attributes>
    </cas:authenticationSuccess>
</cas:serviceResponse>""".trimIndent()
        )
        println(matchResult?.groups?.get(1)?.value)
    }
}