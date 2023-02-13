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

/**
 * Builder is the Schema for the builders API
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Group(Constants.GROUP)
@Version(Constants.API_VERSION)
@Kind("Builder")
class Builder : CustomResource<BuilderSpec, BuilderStatus>(), Namespaced

class BuilderList : DefaultKubernetesResourceList<Builder>()

/**
 * BuilderStatus defines the observed state of Builder
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("base")
@JsonDeserialize(using = JsonDeserializer.None::class)
class BuilderStatus : KubernetesResource {
    @get:JsonProperty("base")
    @set:JsonProperty("base")
    @JsonProperty("base")
    var base: BaseStatus? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(BuilderStatus::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
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
        if (other is BuilderStatus == false) {
            return false
        }
        val rhs = other
        return base === rhs.base || base != null && base == rhs.base
    }
}

/**
 * BuilderSpec defines the desired state of Builder
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("context", "destination", "dockerfilePath", "pushSecretName", "round")
@JsonDeserialize(using = JsonDeserializer.None::class)
class BuilderSpec : KubernetesResource {
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("context")
    @set:JsonProperty("context")
    @JsonProperty("context")
    var context: BuilderContext? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("destination")
    @set:JsonProperty("destination")
    @JsonProperty("destination")
    var destination: String? = null

    @get:JsonProperty("dockerfilePath")
    @set:JsonProperty("dockerfilePath")
    @JsonProperty("dockerfilePath")
    var dockerfilePath: String? = "Dockerfile"

    @get:JsonProperty("pushSecretName")
    @set:JsonProperty("pushSecretName")
    @JsonProperty("pushSecretName")
    var pushSecretName: String? = "push-secret"

    @get:JsonProperty("round")
    @set:JsonProperty("round")
    @JsonProperty("round")
    var round: Int? = -1
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(BuilderSpec::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("context")
        sb.append('=')
        sb.append(if (context == null) "<null>" else context)
        sb.append(',')
        sb.append("destination")
        sb.append('=')
        sb.append(if (destination == null) "<null>" else destination)
        sb.append(',')
        sb.append("dockerfilePath")
        sb.append('=')
        sb.append(if (dockerfilePath == null) "<null>" else dockerfilePath)
        sb.append(',')
        sb.append("pushSecretName")
        sb.append('=')
        sb.append(if (pushSecretName == null) "<null>" else pushSecretName)
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
        result = result * 31 + if (context == null) 0 else context.hashCode()
        result = result * 31 + if (destination == null) 0 else destination.hashCode()
        result = result * 31 + if (dockerfilePath == null) 0 else dockerfilePath.hashCode()
        result = result * 31 + if (round == null) 0 else round.hashCode()
        result = result * 31 + if (pushSecretName == null) 0 else pushSecretName.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is BuilderSpec == false) {
            return false
        }
        val rhs = other
        return (context === rhs.context || context != null && context == rhs.context) && (destination === rhs.destination || destination != null && destination == rhs.destination) && (dockerfilePath === rhs.dockerfilePath || dockerfilePath != null && dockerfilePath == rhs.dockerfilePath) && (round === rhs.round || round != null && round == rhs.round) && (pushSecretName === rhs.pushSecretName || pushSecretName != null && pushSecretName == rhs.pushSecretName)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("git", "raw", "s3")
class BuilderContext {
    @get:JsonProperty("git")
    @set:JsonProperty("git")
    @JsonProperty("git")
    var git: GitContext? = null

    @get:JsonProperty("raw")
    @set:JsonProperty("raw")
    @JsonProperty("raw")
    var raw: String? = null

    @get:JsonProperty("s3")
    @set:JsonProperty("s3")
    @JsonProperty("s3")
    var s3: S3Context? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(BuilderContext::class.java.name).append('@')
            .append(Integer.toHexString(System.identityHashCode(this))).append('[')
        sb.append("git")
        sb.append('=')
        sb.append(if (git == null) "<null>" else git)
        sb.append(',')
        sb.append("raw")
        sb.append('=')
        sb.append(if (raw == null) "<null>" else raw)
        sb.append(',')
        sb.append("s3")
        sb.append('=')
        sb.append(if (s3 == null) "<null>" else s3)
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
        result = result * 31 + if (s3 == null) 0 else s3.hashCode()
        result = result * 31 + if (raw == null) 0 else raw.hashCode()
        result = result * 31 + if (git == null) 0 else git.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is BuilderContext == false) {
            return false
        }
        val rhs = other
        return (s3 === rhs.s3 || s3 != null && s3 == rhs.s3) && (raw === rhs.raw || raw != null && raw == rhs.raw) && (git === rhs.git || git != null && git == rhs.git)
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("endpoint", "ref", "scheme", "userPassword", "username")
class GitContext {
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("endpoint")
    @set:JsonProperty("endpoint")
    @JsonProperty("endpoint")
    var endpoint: String? = null

    @get:JsonProperty("ref")
    @set:JsonProperty("ref")
    @JsonProperty("ref")
    var ref: String? = null

    @get:JsonProperty("scheme")
    @set:JsonProperty("scheme")
    @JsonProperty("scheme")
    var scheme: Scheme? = Scheme.fromValue("https")

    @get:JsonProperty("userPassword")
    @set:JsonProperty("userPassword")
    @JsonProperty("userPassword")
    var userPassword: String? = null

    @get:JsonProperty("username")
    @set:JsonProperty("username")
    @JsonProperty("username")
    var username: String? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(GitContext::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("endpoint")
        sb.append('=')
        sb.append(if (endpoint == null) "<null>" else endpoint)
        sb.append(',')
        sb.append("ref")
        sb.append('=')
        sb.append(if (ref == null) "<null>" else ref)
        sb.append(',')
        sb.append("scheme")
        sb.append('=')
        sb.append(if (scheme == null) "<null>" else scheme)
        sb.append(',')
        sb.append("userPassword")
        sb.append('=')
        sb.append(if (userPassword == null) "<null>" else userPassword)
        sb.append(',')
        sb.append("username")
        sb.append('=')
        sb.append(if (username == null) "<null>" else username)
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
        result = result * 31 + if (endpoint == null) 0 else endpoint.hashCode()
        result = result * 31 + if (ref == null) 0 else ref.hashCode()
        result = result * 31 + if (userPassword == null) 0 else userPassword.hashCode()
        result = result * 31 + if (scheme == null) 0 else scheme.hashCode()
        result = result * 31 + if (username == null) 0 else username.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is GitContext == false) {
            return false
        }
        val rhs = other
        return (endpoint === rhs.endpoint || endpoint != null && endpoint == rhs.endpoint) && (ref === rhs.ref || ref != null && ref == rhs.ref) && (userPassword === rhs.userPassword || userPassword != null && userPassword == rhs.userPassword) && (scheme == rhs.scheme || scheme != null && scheme == rhs.scheme) && (username === rhs.username || username != null && username == rhs.username)
    }

    enum class Scheme(private val value: String) {
        HTTP("http"), HTTPS("https");

        override fun toString(): String {
            return value
        }

        @JsonValue
        fun value(): String {
            return value
        }

        companion object {
            private val CONSTANTS: MutableMap<String, Scheme> = HashMap()

            init {
                for (c in values()) {
                    CONSTANTS[c.value] = c
                }
            }

            @JsonCreator
            fun fromValue(value: String): Scheme {
                val constant = CONSTANTS[value]
                return constant ?: throw IllegalArgumentException(value)
            }
        }
    }
}

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder("accessKeyID", "accessSecretKey", "bucket", "endpoint", "fileType", "objectKey", "region", "scheme")
class S3Context {
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("accessKeyID")
    @set:JsonProperty("accessKeyID")
    @JsonProperty("accessKeyID")
    var accessKeyID: String? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("accessSecretKey")
    @set:JsonProperty("accessSecretKey")
    @JsonProperty("accessSecretKey")
    var accessSecretKey: String? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("bucket")
    @set:JsonProperty("bucket")
    @JsonProperty("bucket")
    var bucket: String? = null

    @get:JsonProperty("endpoint")
    @set:JsonProperty("endpoint")
    @JsonProperty("endpoint")
    var endpoint: String? = "s3.amazonaws.com"

    @get:JsonProperty("fileType")
    @set:JsonProperty("fileType")
    @JsonProperty("fileType")
    var fileType: FileType? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("objectKey")
    @set:JsonProperty("objectKey")
    @JsonProperty("objectKey")
    var objectKey: String? = null
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    /**
     * (Required)
     */
    @get:JsonProperty("region")
    @set:JsonProperty("region")
    @JsonProperty("region")
    var region: String? = null

    @get:JsonProperty("scheme")
    @set:JsonProperty("scheme")
    @JsonProperty("scheme")
    var scheme: Scheme? = null
    override fun toString(): String {
        val sb = StringBuilder()
        sb.append(S3Context::class.java.name).append('@').append(Integer.toHexString(System.identityHashCode(this)))
            .append('[')
        sb.append("accessKeyID")
        sb.append('=')
        sb.append(if (accessKeyID == null) "<null>" else accessKeyID)
        sb.append(',')
        sb.append("accessSecretKey")
        sb.append('=')
        sb.append(if (accessSecretKey == null) "<null>" else accessSecretKey)
        sb.append(',')
        sb.append("bucket")
        sb.append('=')
        sb.append(if (bucket == null) "<null>" else bucket)
        sb.append(',')
        sb.append("endpoint")
        sb.append('=')
        sb.append(if (endpoint == null) "<null>" else endpoint)
        sb.append(',')
        sb.append("fileType")
        sb.append('=')
        sb.append(if (fileType == null) "<null>" else fileType)
        sb.append(',')
        sb.append("objectKey")
        sb.append('=')
        sb.append(if (objectKey == null) "<null>" else objectKey)
        sb.append(',')
        sb.append("region")
        sb.append('=')
        sb.append(if (region == null) "<null>" else region)
        sb.append(',')
        sb.append("scheme")
        sb.append('=')
        sb.append(if (scheme == null) "<null>" else scheme)
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
        result = result * 31 + if (accessKeyID == null) 0 else accessKeyID.hashCode()
        result = result * 31 + if (bucket == null) 0 else bucket.hashCode()
        result = result * 31 + if (endpoint == null) 0 else endpoint.hashCode()
        result = result * 31 + if (scheme == null) 0 else scheme.hashCode()
        result = result * 31 + if (objectKey == null) 0 else objectKey.hashCode()
        result = result * 31 + if (accessSecretKey == null) 0 else accessSecretKey.hashCode()
        result = result * 31 + if (region == null) 0 else region.hashCode()
        result = result * 31 + if (fileType == null) 0 else fileType.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        if (other is S3Context == false) {
            return false
        }
        val rhs = other
        return (accessKeyID === rhs.accessKeyID || accessKeyID != null && accessKeyID == rhs.accessKeyID) && (bucket === rhs.bucket || bucket != null && bucket == rhs.bucket) && (endpoint === rhs.endpoint || endpoint != null && endpoint == rhs.endpoint) && (scheme == rhs.scheme || scheme != null && scheme == rhs.scheme) && (objectKey === rhs.objectKey || objectKey != null && objectKey == rhs.objectKey) && (accessSecretKey === rhs.accessSecretKey || accessSecretKey != null && accessSecretKey == rhs.accessSecretKey) && (region === rhs.region || region != null && region == rhs.region) && (fileType == rhs.fileType || fileType != null && fileType == rhs.fileType)
    }

    enum class FileType(private val value: String) {
        TAR("tar"), TAR_GZ("tar.gz"), ZIP("zip"), RAR("rar"), DIR("dir");

        override fun toString(): String {
            return value
        }

        @JsonValue
        fun value(): String {
            return value
        }

        companion object {
            private val CONSTANTS: MutableMap<String, FileType> = HashMap()

            init {
                for (c in values()) {
                    CONSTANTS[c.value] = c
                }
            }

            @JsonCreator
            fun fromValue(value: String): FileType {
                val constant = CONSTANTS[value]
                return constant ?: throw IllegalArgumentException(value)
            }
        }
    }

    enum class Scheme(private val value: String) {
        HTTP("http"), HTTPS("https");

        override fun toString(): String {
            return value
        }

        @JsonValue
        fun value(): String {
            return value
        }

        companion object {
            private val CONSTANTS: MutableMap<String, Scheme> = HashMap()

            init {
                for (c in values()) {
                    CONSTANTS[c.value] = c
                }
            }

            @JsonCreator
            fun fromValue(value: String): Scheme {
                val constant = CONSTANTS[value]
                return constant ?: throw IllegalArgumentException(value)
            }
        }
    }
}
