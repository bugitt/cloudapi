package cn.edu.buaa.scs.utils

import org.junit.jupiter.api.Test
import org.ktorm.entity.Entity


internal class JsonTest {

    private data class B(
        val value: Int,
        val name: String,
    )

    private interface A : Entity<A> {
        companion object : Entity.Factory<A>()

        var id: Int
        var name: String

        var b: B
    }

    private data class C(
        val a: Int,
    ) {
        @Suppress("unused")
        val b: Int
            get() = a + 1
    }

    @Test
    fun interfaceSerializer() {
        val a = A {
            id = 89
            name = "test"
            b = B(89, "tt")
        }
        val strValue = jsonMapper.writeValueAsString(a)
        val a2: A = jsonReadValue(strValue)
        assert(a.id == a2.id)
        assert(a.name == a2.name)
        assert(a.b.value == a2.b.value)
        assert(a.b.name == a2.b.name)
    }

    @Test
    fun dataClass() {
        val c1 = C(1)
        val strValue = jsonMapper.writeValueAsString(c1)
        assert(strValue.contains("""{"a":1,"b":2}"""))
    }
}