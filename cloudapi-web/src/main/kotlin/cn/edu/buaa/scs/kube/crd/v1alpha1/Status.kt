package cn.edu.buaa.scs.kube.crd.v1alpha1

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyDescription
import com.fasterxml.jackson.annotation.JsonPropertyOrder

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("currentRound", "historyList", "message", "status")
class BaseStatus {
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("currentRound")
    @set:JsonProperty("currentRound")
    @JsonProperty("currentRound")
    var currentRound: Int? = null
    /**
     * HistoryList is used to store the history of the CRD.
     */
    /**
     * HistoryList is used to store the history of the CRD.
     */
    /**
     * HistoryList is used to store the history of the CRD.
     */
    @get:JsonProperty("historyList")
    @set:JsonProperty("historyList")
    @JsonProperty("historyList")
    @JsonPropertyDescription("HistoryList is used to store the history of the CRD.")
    var historyList: List<String>? = null
    /**
     * Message is mainly used to store the error message when the CRD is failed.
     */
    /**
     * Message is mainly used to store the error message when the CRD is failed.
     */
    /**
     * Message is mainly used to store the error message when the CRD is failed.
     */
    @get:JsonProperty("message")
    @set:JsonProperty("message")
    @JsonProperty("message")
    @JsonPropertyDescription("Message is mainly used to store the error message when the CRD is failed.")
    var message: String? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("status")
    @set:JsonProperty("status")
    @JsonProperty("status")
    var status: String? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(BaseStatus::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("currentRound")
        sb.append('=')
        sb.append(if (currentRound == null) "<null>" else currentRound)
        sb.append(',')
        sb.append("historyList")
        sb.append('=')
        sb.append(if (historyList == null) "<null>" else historyList)
        sb.append(',')
        sb.append("message")
        sb.append('=')
        sb.append(if (message == null) "<null>" else message)
        sb.append(',')
        sb.append("status")
        sb.append('=')
        sb.append(if (status == null) "<null>" else status)
        sb.append(',')
        if (sb[sb.length - 1] == ',') {
            sb.setCharAt(sb.length - 1, ']')
        } else {
            sb.append(']')
        }
        return sb.toString()
    }

    override fun hashCode(): Int {
        var result = 1
        result = result * 31 + if (currentRound == null) 0 else currentRound.hashCode()
        result = result * 31 + if (message == null) 0 else message.hashCode()
        result = result * 31 + if (historyList == null) 0 else historyList.hashCode()
        result = result * 31 + if (status == null) 0 else status.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is BaseStatus == false) {
            return false
        }
        val rhs = other
        return (currentRound === rhs.currentRound || currentRound != null && currentRound == rhs.currentRound) && (message === rhs.message || message != null && message == rhs.message) && (historyList === rhs.historyList || historyList != null && historyList == rhs.historyList) && (status === rhs.status || status != null && status == rhs.status)
    }
}
