package cn.edu.buaa.scs.kube.crd.v1alpha1

import com.fasterxml.jackson.annotation.*
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import io.fabric8.kubernetes.api.model.DefaultKubernetesResourceList
import io.fabric8.kubernetes.api.model.KubernetesResource
import io.fabric8.kubernetes.api.model.Namespaced
import io.fabric8.kubernetes.client.CustomResource
import io.fabric8.kubernetes.model.annotation.Group
import io.fabric8.kubernetes.model.annotation.Kind
import io.fabric8.kubernetes.model.annotation.Version
import javax.annotation.Generated

/**
 * Deployer is the Schema for the deployers API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Generated("jsonschema2pojo")
@Group(Constants.GROUP)
@Version(Constants.API_VERSION)
@Kind("Deployer")
class Deployer : CustomResource<DeployerSpec, DeployerStatus>(), Namespaced

class DeployerList : DefaultKubernetesResourceList<Deployer>()

/**
 * DeployerStatus defines the observed state of Deployer
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("base")
@Generated("jsonschema2pojo")
@JsonDeserialize(using = JsonDeserializer.None::class)
class DeployerStatus : KubernetesResource {
    @get:JsonProperty("base")
    @set:JsonProperty("base")
    @JsonProperty("base")
    var base: BaseStatus? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(DeployerStatus::class.java.name).append('@')
            .append(Integer.toHexString(System.identityHashCode(this))).append('[')
        sb.append("base")
        sb.append('=')
        sb.append(if (base == null) "<null>" else base)
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
        result = result * 31 + if (base == null) 0 else base.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is DeployerStatus == false) {
            return false
        }
        val rhs = other
        return base === rhs.base || base != null && base!!.equals(rhs.base)
    }
}


/**
 * DeployerSpec defines the desired state of Deployer
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("containers", "round", "type")
@Generated("jsonschema2pojo")
@JsonDeserialize(using = JsonDeserializer.None::class)
class DeployerSpec : KubernetesResource {
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("containers")
    @set:JsonProperty("containers")
    @JsonProperty("containers")
    var containers: List<ContainerSpec>? = null

    @get:JsonProperty("round")
    @set:JsonProperty("round")
    @JsonProperty("round")
    var round: Int? = -1
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("type")
    @set:JsonProperty("type")
    @JsonProperty("type")
    var type: Type? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(DeployerSpec::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("containers")
        sb.append('=')
        sb.append(if (containers == null) "<null>" else containers)
        sb.append(',')
        sb.append("round")
        sb.append('=')
        sb.append(if (round == null) "<null>" else round)
        sb.append(',')
        sb.append("type")
        sb.append('=')
        sb.append(if (type == null) "<null>" else type)
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
        result = result * 31 + if (containers == null) 0 else containers.hashCode()
        result = result * 31 + if (round == null) 0 else round.hashCode()
        result = result * 31 + if (type == null) 0 else type.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is DeployerSpec == false) {
            return false
        }
        val rhs = other
        return (containers === rhs.containers || containers != null && containers == rhs.containers) && (round === rhs.round || round != null && round == rhs.round) && (type == rhs.type || type != null && type == rhs.type)
    }

    @Generated("jsonschema2pojo")
    enum class Type(private val value: String) {
        JOB("job"), SERVICE("service");

        override fun toString(): String {
            return value
        }

        @JsonValue
        fun value(): String {
            return value
        }

        companion object {
            private val CONSTANTS: MutableMap<String, Type> = HashMap()

            init {
                for (c in values()) {
                    CONSTANTS[c.value] = c
                }
            }

            @JsonCreator
            fun fromValue(value: String): Type {
                val constant = CONSTANTS[value]
                return constant ?: throw IllegalArgumentException(value)
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("args", "command", "env", "image", "initial", "name", "ports", "resource")
@Generated("jsonschema2pojo")
class ContainerSpec {
    @get:JsonProperty("args")
    @set:JsonProperty("args")
    @JsonProperty("args")
    var args: List<String>? = null

    @get:JsonProperty("command")
    @set:JsonProperty("command")
    @JsonProperty("command")
    var command: List<String>? = null

    @get:JsonProperty("env")
    @set:JsonProperty("env")
    @JsonProperty("env")
    var env: Map<String, String>? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("image")
    @set:JsonProperty("image")
    @JsonProperty("image")
    var image: String? = null

    @get:JsonProperty("initial")
    @set:JsonProperty("initial")
    @JsonProperty("initial")
    var initial: Boolean? = false
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

    @get:JsonProperty("ports")
    @set:JsonProperty("ports")
    @JsonProperty("ports")
    var ports: List<Port>? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("resource")
    @set:JsonProperty("resource")
    @JsonProperty("resource")
    var resource: Resource? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(ContainerSpec::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("args")
        sb.append('=')
        sb.append(if (args == null) "<null>" else args)
        sb.append(',')
        sb.append("command")
        sb.append('=')
        sb.append(if (command == null) "<null>" else command)
        sb.append(',')
        sb.append("env")
        sb.append('=')
        sb.append(if (env == null) "<null>" else env)
        sb.append(',')
        sb.append("image")
        sb.append('=')
        sb.append(if (image == null) "<null>" else image)
        sb.append(',')
        sb.append("initial")
        sb.append('=')
        sb.append(if (initial == null) "<null>" else initial)
        sb.append(',')
        sb.append("name")
        sb.append('=')
        sb.append(if (name == null) "<null>" else name)
        sb.append(',')
        sb.append("ports")
        sb.append('=')
        sb.append(if (ports == null) "<null>" else ports)
        sb.append(',')
        sb.append("resource")
        sb.append('=')
        sb.append(if (resource == null) "<null>" else resource)
        sb.append(',')
        if (sb[sb.length - 1] == ',') {
            sb.setCharAt(sb.length - 1, ']')
        } else {
            sb.append(']')
        }
        return sb.toString()
    }
}


@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("cpu", "memory")
@Generated("jsonschema2pojo")
class Resource {
    /**
     *
     * (Required)
     *
     */
    /**
     *
     * (Required)
     *
     */
    /**
     *
     * (Required)
     *
     */
    @get:JsonProperty("cpu")
    @set:JsonProperty("cpu")
    @JsonProperty("cpu")
    var cpu: Int? = null
    /**
     *
     * (Required)
     *
     */
    /**
     *
     * (Required)
     *
     */
    /**
     *
     * (Required)
     *
     */
    @get:JsonProperty("memory")
    @set:JsonProperty("memory")
    @JsonProperty("memory")
    var memory: Int? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(Resource::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("cpu")
        sb.append('=')
        sb.append(if (cpu == null) "<null>" else cpu)
        sb.append(',')
        sb.append("memory")
        sb.append('=')
        sb.append(if (memory == null) "<null>" else memory)
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
        result = result * 31 + if (cpu == null) 0 else cpu.hashCode()
        result = result * 31 + if (memory == null) 0 else memory.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is Resource == false) {
            return false
        }
        val rhs = other
        return (cpu === rhs.cpu || cpu != null && cpu == rhs.cpu) && (memory === rhs.memory || memory != null && memory == rhs.memory)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder
@Generated("jsonschema2pojo")
class Env {
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(Env::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        if (sb[sb.length - 1] == ',') {
            sb.setCharAt(sb.length - 1, ']')
        } else {
            sb.append(']')
        }
        return sb.toString()
    }

    override fun hashCode(): Int {
        return 1
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is Env == false) {
            return false
        }
        val rhs = other
        return true
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("export", "port", "protocol")
@Generated("jsonschema2pojo")
class Port {
    @get:JsonProperty("export")
    @set:JsonProperty("export")
    @JsonProperty("export")
    var export: Boolean? = false
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("port")
    @set:JsonProperty("port")
    @JsonProperty("port")
    var port: Int? = null

    @get:JsonProperty("protocol")
    @set:JsonProperty("protocol")
    @JsonProperty("protocol")
    var protocol: Protocol? = Protocol.fromValue("tcp")
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(Port::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("export")
        sb.append('=')
        sb.append(if (export == null) "<null>" else export)
        sb.append(',')
        sb.append("port")
        sb.append('=')
        sb.append(if (port == null) "<null>" else port)
        sb.append(',')
        sb.append("protocol")
        sb.append('=')
        sb.append(if (protocol == null) "<null>" else protocol)
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
        result = result * 31 + if (protocol == null) 0 else protocol.hashCode()
        result = result * 31 + if (export == null) 0 else export.hashCode()
        result = result * 31 + if (port == null) 0 else port.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is Port == false) {
            return false
        }
        val rhs = other
        return (protocol == rhs.protocol || protocol != null && protocol == rhs.protocol) && (export === rhs.export || export != null && export == rhs.export) && (port === rhs.port || port != null && port == rhs.port)
    }

    @Generated("jsonschema2pojo")
    enum class Protocol(private val value: String) {
        TCP("tcp"), UDP("udp"), SCTP("sctp");

        override fun toString(): String {
            return value
        }

        @JsonValue
        fun value(): String {
            return value
        }

        companion object {
            private val CONSTANTS: MutableMap<String, Protocol> = HashMap()

            init {
                for (c in values()) {
                    CONSTANTS[c.value] = c
                }
            }

            @JsonCreator
            fun fromValue(value: String): Protocol {
                val constant = CONSTANTS[value]
                return constant ?: throw IllegalArgumentException(value)
            }
        }
    }
}
