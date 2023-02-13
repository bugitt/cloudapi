package cn.edu.buaa.scs.model

class IntOrString {
    private var intVal: Int = 0
    private var stringVal: String = ""

    constructor(value: Int) {
        this.intVal = value
    }

    constructor(value: String) {
        this.stringVal = value
    }

    fun getInt(): Int = intVal
    fun getString(): String = stringVal
}
