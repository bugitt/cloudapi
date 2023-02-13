package cn.edu.buaa.scs.kube.crd.v1alpha1

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version

/**
 * Workflow is the Schema for the workflows API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(Constants.GROUP)
@Version(Constants.API_VERSION)
@Kind("Workflow")
class Workflow : CustomResource<WorkflowSpec, WorkflowStatus>(), Namespaced

class WorkflowList : DefaultKubernetesResourceList<Workflow>()

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("builderList", "deployerList", "round")
@JsonDeserialize(using = JsonDeserializer.None::class)
class WorkflowSpec : KubernetesResource {
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("builderList")
    @set:JsonProperty("builderList")
    @JsonProperty("builderList")
    var builderList: List<NamespacedName>? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("deployerList")
    @set:JsonProperty("deployerList")
    @JsonProperty("deployerList")
    var deployerList: List<NamespacedName>? = null

    @get:JsonProperty("round")
    @set:JsonProperty("round")
    @JsonProperty("round")
    var round: Int? = -1
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(WorkflowSpec::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("builderList")
        sb.append('=')
        sb.append(if (builderList == null) "<null>" else builderList)
        sb.append(',')
        sb.append("deployerList")
        sb.append('=')
        sb.append(if (deployerList == null) "<null>" else deployerList)
        sb.append(',')
        sb.append("round")
        sb.append('=')
        sb.append(if (round == null) "<null>" else round)
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
        result = result * 31 + if (builderList == null) 0 else builderList.hashCode()
        result = result * 31 + if (round == null) 0 else round.hashCode()
        result = result * 31 + if (deployerList == null) 0 else deployerList.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is WorkflowSpec == false) {
            return false
        }
        val rhs = other
        return (builderList === rhs.builderList || builderList != null && builderList == rhs.builderList) && (round === rhs.round || round != null && round == rhs.round) && (deployerList === rhs.deployerList || deployerList != null && deployerList == rhs.deployerList)
    }
}

/**
 * WorkflowStatus defines the observed state of Workflow
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("base", "stage")
@JsonDeserialize(using = JsonDeserializer.None::class)
class WorkflowStatus : KubernetesResource {
    @get:JsonProperty("base")
    @set:JsonProperty("base")
    @JsonProperty("base")
    var base: BaseStatus? = null

    @get:JsonProperty("stage")
    @set:JsonProperty("stage")
    @JsonProperty("stage")
    var stage: String? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(WorkflowStatus::class.java.name).append('@')
            .append(Integer.toHexString(System.identityHashCode(this))).append('[')
        sb.append("base")
        sb.append('=')
        sb.append(if (base == null) "<null>" else base)
        sb.append(',')
        sb.append("stage")
        sb.append('=')
        sb.append(if (stage == null) "<null>" else stage)
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
        result = result * 31 + if (stage == null) 0 else stage.hashCode()
        result = result * 31 + if (base == null) 0 else base.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is WorkflowStatus == false) {
            return false
        }
        val rhs = other
        return (stage === rhs.stage || stage != null && stage == rhs.stage) && (base === rhs.base || base != null && base!!.equals(
            rhs.base
        ))
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("name", "namespace")
class NamespacedName {
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("name")
    @set:JsonProperty("name")
    @JsonProperty("name")
    var name: String? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("namespace")
    @set:JsonProperty("namespace")
    @JsonProperty("namespace")
    var namespace: String? = null
}
