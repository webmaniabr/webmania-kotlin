package com.webmania.sdk.kotlin

import com.webmania.sdk.Credentials as JavaCredentials
import com.webmania.sdk.WebmaniaClient as JavaClient
import com.webmania.sdk.ClientConfig
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.jar.JarFile
import java.net.JarURLConnection

/** Fixed Java archive and ordinary classpath identity. */
internal object RuntimePin {
    private const val SHA = "29e262427ca8ec15918c2072020c674c64f88be9427efa949b92ddd802f494e3"
    private val valid: Boolean by lazy {
        val location = JavaClient::class.java.protectionDomain?.codeSource?.location
            ?: error("The pinned Java SDK archive is required")
        require(location.protocol == "file") { "The pinned Java SDK archive is required" }
        val file = Path.of(location.toURI())
        require(Files.isRegularFile(file)) { "The pinned Java SDK archive is required" }
        val digest = MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))
        val actual = digest.joinToString("") { "%02x".format(it.toInt() and 255) }
        require(actual == SHA) { "Java SDK dependency identity mismatch" }
        val loader = JavaClient::class.java.classLoader
        JarFile(file.toFile()).use { archive ->
            val entries = archive.entries()
            var classes = 0
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                if (entry.name.startsWith("com/webmania/sdk/") && entry.name.endsWith(".class")) {
                    val name = entry.name.removeSuffix(".class").replace('/', '.')
                    // Resolve every SDK class, including models and nested classes, without running initializers.
                    val type = Class.forName(name, false, loader)
                    val origin = type.protectionDomain?.codeSource?.location
                    val sameArchive = origin != null && origin.protocol == "file" && runCatching {
                        val source = Path.of(origin.toURI())
                        Files.isRegularFile(source) && Files.isSameFile(file, source)
                    }.getOrDefault(false)
                    require(sameArchive) { "Java SDK class origin mismatch" }
                    classes++
                } else if (entry.name.startsWith("contracts/") || entry.name.endsWith(".json")) {
                    val resource = loader.getResource(entry.name)
                    require(resource != null && resource.protocol == "jar") { "Java SDK resource origin mismatch" }
                    val connection = resource.openConnection()
                    val sameArchive = connection is JarURLConnection && connection.jarFileURL.protocol == "file" &&
                        connection.entryName == entry.name && runCatching {
                            val source = Path.of(connection.jarFileURL.toURI())
                            Files.isRegularFile(source) && Files.isSameFile(file, source)
                        }.getOrDefault(false)
                    require(sameArchive) { "Java SDK resource origin mismatch" }
                    connection.useCaches = false
                    val actualBytes = connection.getInputStream().use { it.readBytes() }
                    val expectedBytes = archive.getInputStream(entry).use { it.readBytes() }
                    require(actualBytes.contentEquals(expectedBytes)) { "Java SDK resource identity mismatch" }
                }
            }
            check(classes > 0) { "The pinned Java SDK archive is required" }
        }
        true
    }
    fun check() { check(valid) }
}

class CompanyCredentials private constructor(internal val raw: JavaCredentials) {
    companion object { fun create(key: String, secret: String, token: String, tokenSecret: String): CompanyCredentials {
        RuntimePin.check(); return CompanyCredentials(JavaCredentials.company(key, secret, token, tokenSecret))
    } }
    override fun toString() = "CompanyCredentials[protected]"
}
class ResellerCredentials private constructor(internal val raw: JavaCredentials) {
    companion object { fun create(key: String, secret: String, token: String, tokenSecret: String): ResellerCredentials {
        RuntimePin.check(); return ResellerCredentials(JavaCredentials.reseller(key, secret, token, tokenSecret))
    } }
    override fun toString() = "ResellerCredentials[protected]"
}
class IssuerCredentials private constructor(internal val raw: JavaCredentials) {
    companion object { fun create(token: String): IssuerCredentials { RuntimePin.check(); return IssuerCredentials(JavaCredentials.issuer(token)) } }
    override fun toString() = "IssuerCredentials[protected]"
}
class MonitorCredentials private constructor(internal val raw: JavaCredentials) {
    companion object { fun create(token: String): MonitorCredentials { RuntimePin.check(); return MonitorCredentials(JavaCredentials.monitor(token)) } }
    override fun toString() = "MonitorCredentials[protected]"
}
sealed class NfeFileAuth protected constructor(internal val raw: JavaCredentials) {
    private class Mode(raw: JavaCredentials): NfeFileAuth(raw)
    companion object {
        fun company(credentials: CompanyCredentials): NfeFileAuth { RuntimePin.check(); return Mode(credentials.raw) }
        fun documentToken(token: String): NfeFileAuth { RuntimePin.check(); return Mode(JavaCredentials.documentToken(token)) }
        fun publicReport(): NfeFileAuth { RuntimePin.check(); return Mode(JavaCredentials.anonymousFile()) }
    }
    override fun toString() = "NfeFileAuth[protected]"
}
sealed class EmitterFileAuth protected constructor(internal val raw: JavaCredentials) {
    private class Mode(raw: JavaCredentials): EmitterFileAuth(raw)
    companion object {
        fun companyToken(token: String): EmitterFileAuth { RuntimePin.check(); return Mode(JavaCredentials.fileCompany(token)) }
        fun documentToken(token: String): EmitterFileAuth { RuntimePin.check(); return Mode(JavaCredentials.documentToken(token)) }
        fun anonymous(): EmitterFileAuth { RuntimePin.check(); return Mode(JavaCredentials.anonymousFile()) }
    }
    override fun toString() = "EmitterFileAuth[protected]"
}
internal fun engine(config: ClientConfig, credentials: JavaCredentials): JavaClient {
    RuntimePin.check(); return JavaClient(config, credentials)
}
fun clientConfig(block: ClientConfig.Builder.() -> Unit = {}): ClientConfig {
    RuntimePin.check(); return ClientConfig.builder().apply(block).build()
}
fun jsonValue(raw: String): com.webmania.sdk.JsonValue {
    RuntimePin.check(); return com.webmania.sdk.JsonValue(com.webmania.sdk.Json.parse(raw))
}
val <T> com.webmania.sdk.ApiResult<T>.value: T get() { RuntimePin.check(); return data() }
val <T> com.webmania.sdk.ApiResult<T>.httpStatus: Int get() { RuntimePin.check(); return status() }
val <T> com.webmania.sdk.PageCollection<T>.values: List<T> get() { RuntimePin.check(); return items() }

/** The inspected representation; null for pending JSON and redirect results. */
typealias FileKind = com.webmania.sdk.Download.Kind
typealias FileProtection = com.webmania.sdk.Download.Protection
val com.webmania.sdk.Download.fileKind: FileKind? get() { RuntimePin.check(); return kind() }
/** Representation encryption is reported separately from authorization metadata. */
val com.webmania.sdk.Download.isEncrypted: Boolean get() { RuntimePin.check(); return encrypted() }
val com.webmania.sdk.Download.fileProtection: FileProtection get() { RuntimePin.check(); return protection() }
