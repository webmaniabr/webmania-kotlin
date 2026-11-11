// Optional homologation walk-through (ambiente 2): reads, then the NF-e and NFS-e lifecycles (issue, bounded
// consult, files, cancel, final consult). Configuration comes only from the WEBMANIA_* environment. Output is one
// JSON line per step with statuses, sizes and digests; identifiers, payload values and credentials are never
// printed. A mutation is never resent.
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.webmania.sdk.ApiException
import com.webmania.sdk.ApiResult
import com.webmania.sdk.Download
import com.webmania.sdk.JsonValue
import com.webmania.sdk.kotlin.*
import com.webmania.sdk.model.api1files.*
import com.webmania.sdk.model.api1fiscal.*
import com.webmania.sdk.model.api2emitterfiles.*
import com.webmania.sdk.model.api2emitterreads.*
import com.webmania.sdk.model.api2nfseissuance.*
import java.net.URI
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.time.Duration
import kotlin.system.exitProcess

// Decimal numbers are read as BigDecimal: the SDK refuses binary floating point in request bodies.
private val json = ObjectMapper().enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
private val processing = setOf("processando", "processamento", "contingencia", "pendente")
private var step = 0
private var failed = false
private var ledger: Path? = null
private class Unknown : RuntimeException(null, null, false, false)

private fun blocked(reason: String?): Nothing {
    val line = json.createObjectNode().put("result", "blocked"); if (reason != null) line.put("reason", reason)
    println(line.toString()); exitProcess(77)
}
private fun env(name: String, fallback: String? = null): String? = System.getenv(name)?.takeIf { it.isNotBlank() } ?: fallback
private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }
private fun line(operation: String, ok: Boolean, status: Int, outcome: String, kind: String?, identifier: String?): ObjectNode =
    json.createObjectNode().put("step", ++step).put("operation", operation).put("ok", ok).put("http_status", status).put("outcome", outcome)
        .put("kind", kind).put("ref", identifier?.let { sha256(it.toByteArray(StandardCharsets.UTF_8)).substring(0, 12) })
private fun emit(node: ObjectNode) { println(node.toString()); System.out.flush() }
private fun record(model: String, identifier: String?, status: String?, operation: String) {
    val path = ledger ?: return
    val entry = json.createObjectNode().put("model", model).put("identifier", identifier).put("status", status).put("operation", operation)
    Files.writeString(path, entry.toString() + "\n", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND)
}
private fun text(node: JsonNode?, name: String): String? = node?.path(name)?.takeIf { it.isTextual }?.textValue()
private fun kindOf(e: ApiException) = e.kind().name.lowercase()
private fun dataOf(result: ApiResult<*>): JsonNode? = (result.data() as? JsonValue)?.toJson()

private fun read(operation: String, identifier: String?, call: () -> ApiResult<*>): JsonNode? = try {
    val r = call(); emit(line(operation, true, r.status(), "success", null, identifier)); dataOf(r)
} catch (e: ApiException) {
    failed = true; emit(line(operation, false, e.status(), if (e.outcomeUncertain()) "unknown" else "failure", kindOf(e), identifier)); null
}
private fun mutation(model: String, operation: String, identifier: String?, call: () -> ApiResult<*>): JsonNode? = try {
    val r = call(); val data = dataOf(r) ?: json.createObjectNode()
    emit(line(operation, true, r.status(), "success", null, identifier ?: text(data, "uuid"))); data
} catch (e: ApiException) {
    val id = identifier ?: e.reference()?.value()
    if (e.outcomeUncertain()) { emit(line(operation, false, e.status(), "unknown", kindOf(e), id)); record(model, id, "unknown", operation); throw Unknown() }
    failed = true; emit(line(operation, false, e.status(), "failure", kindOf(e), id)); null
}
private fun file(operation: String, identifier: String, call: () -> ApiResult<Download>) {
    try {
        val r = call(); val d = r.data(); val pending = d.pending() || d.isRedirect
        val node = line(operation, !pending, r.status(), if (pending) "pending" else "success", null, identifier)
        val bytes = d.bytes(); node.put("size", bytes.size).put("sha256", sha256(bytes)).put("format", d.fileKind?.name?.lowercase())
        if (pending) failed = true
        emit(node)
    } catch (e: ApiException) {
        failed = true; emit(line(operation, false, e.status(), if (e.outcomeUncertain()) "unknown" else "failure", kindOf(e), identifier))
    }
}
private fun poll(model: String, uuid: String, operation: String, initial: String?, call: () -> ApiResult<*>, pending: Set<String> = processing): String? {
    var status = initial; var attempt = 0
    // At least one consult; then poll only while the document is in a pending status.
    while (attempt < 30 && (attempt == 0 || status == null || status in pending)) {
        if (attempt > 0) Thread.sleep(10_000)
        attempt++
        val data = read(operation, uuid, call)
        val observed = text(if (data != null && data.isArray && data.size() > 0) data.get(0) else data, "status")
        if (observed != null && observed != status) record(model, uuid, observed, operation)
        if (observed != null) status = observed
    }
    return status
}
// Whether the consults after the issue ended on aprovado: only then is the document cancelled, whether the issue call
// approved it or it was approved later. A refused document gets no other mutation; one without a terminal status
// within the bound is ledgered as unknown for reconciliation, and the issue is never resent.
private fun approved(model: String, uuid: String, operation: String, status: String?): Boolean {
    if (status == "aprovado") return true
    if (status == null || status in processing) record(model, uuid, "unknown", operation)
    failed = true; return false
}
private fun origin(name: String, fallback: String): URI {
    val uri = try { URI.create(env(name, fallback)!!) } catch (e: IllegalArgumentException) { blocked("origin") }
    if (uri.scheme != "https" || uri.host == null || uri.userInfo != null || uri.query != null || uri.fragment != null) blocked("origin")
    return uri
}
private fun payload(name: String): JsonNode {
    val path = env(name) ?: blocked("payload")
    val raw = try { Files.readAllBytes(Path.of(path)) } catch (e: java.io.IOException) { blocked("payload") }
    if (raw.size > 8 * 1024 * 1024) blocked("payload")
    val node = try { json.readTree(raw) } catch (e: java.io.IOException) { blocked("payload") }
    // Only the decoded top-level JSON integer 2 is homologation: the text "2", 2.0 or a nested value is refused.
    val ambiente = node?.get("ambiente")
    if (node == null || !node.isObject || ambiente == null || !(ambiente.isInt && ambiente.intValue() == 2)) blocked("ambiente")
    return node
}
private fun field(name: String, value: String): ObjectNode = json.createObjectNode().put(name, value)
private fun wrap(name: String, value: JsonNode): ObjectNode = json.createObjectNode().set(name, value)

private fun nfeCycle(c: NfeClient, files: NfeFilesClient, payload: JsonNode) {
    val issued = mutation("nfe", "nfeIssueDocument", null) { c.nfeIssueDocument(NfeIssueDocumentRequest(wrap("body", payload))) }
    val uuid = text(issued, "uuid") ?: run { failed = true; return }
    var status = text(issued, "status"); record("nfe", uuid, status, "nfeIssueDocument")
    val consult = { c.nfeGetDocument(NfeGetDocumentRequest(field("uuid", uuid))) }
    status = poll("nfe", uuid, "nfeGetDocument", status, consult)
    if (!approved("nfe", uuid, "nfeGetDocument", status)) return
    run {
        read("nfeQueryDocument", uuid) { c.nfeQueryDocument(NfeQueryDocumentRequest(wrap("body", field("uuid", uuid)))) }
        file("nfeDownloadDanfe", uuid) { files.nfeDownloadDanfe(NfeDownloadDanfeRequest(field("identifier", uuid))) }
        file("nfeDownloadSimpleDanfe", uuid) { files.nfeDownloadSimpleDanfe(NfeDownloadSimpleDanfeRequest(field("identifier", uuid))) }
        file("nfeDownloadXml", uuid) { files.nfeDownloadXml(NfeDownloadXmlRequest(field("identifier", uuid))) }
    }
    val body = field("uuid", uuid).put("motivo", "Cancelamento de teste em homologacao")
    mutation("nfe", "nfeCancelDocument", uuid) { c.nfeCancelDocument(NfeCancelDocumentRequest(wrap("body", body))) } ?: return
    record("nfe", uuid, "cancelamento_solicitado", "nfeCancelDocument")
    if (poll("nfe", uuid, "nfeGetDocument", "processando", consult) != "cancelado") { failed = true; return }
    file("nfeDownloadCancellationXml", uuid) { files.nfeDownloadCancellationXml(NfeDownloadCancellationXmlRequest(field("identifier", uuid))) }
}
private fun nfseCycle(c: EmitterClient, files: EmitterFilesClient, payload: JsonNode) {
    val issued = mutation("nfse", "emitterIssueNfse", null) { c.emitterIssueNfse(EmitterIssueNfseRequest(wrap("body", payload))) }
    val uuid = text(issued, "uuid") ?: run { failed = true; return }
    var status = text(issued, "status"); record("nfse", uuid, status, "emitterIssueNfse")
    val consult = { c.emitterGetNfse(EmitterGetNfseRequest(field("identifier", uuid))) }
    status = poll("nfse", uuid, "emitterGetNfse", status, consult)
    if (!approved("nfse", uuid, "emitterGetNfse", status)) return
    run {
        file("fileGetNfseXml", uuid) { files.fileGetNfseXml(FileGetNfseXmlRequest(field("identifier", uuid))) }
        file("fileGetNfsePdf", uuid) { files.fileGetNfsePdf(FileGetNfsePdfRequest(field("identifier", uuid))) }
        file("fileGetNfseRpsPdf", uuid) { files.fileGetNfseRpsPdf(FileGetNfseRpsPdfRequest(field("identifier", uuid))) }
    }
    val body = field("uuid", uuid).put("motivo", "1").put("justificativa", "Cancelamento de teste em homologacao")
    val cancelled = mutation("nfse", "emitterCancelNfse", uuid) { c.emitterCancelNfse(EmitterCancelNfseRequest(wrap("body", body))) } ?: return
    record("nfse", uuid, text(cancelled, "status"), "emitterCancelNfse")
    // An NFS-e cancellation can stay in processando_cancelamento until a later consult completes it.
    if (poll("nfse", uuid, "emitterGetNfse", "processando", consult, processing + "processando_cancelamento") != "cancelado") failed = true
}

fun main() {
    if (System.getenv("WEBMANIA_LIVE") != "1") blocked(null)
    val scope = env("WEBMANIA_LIVE_SCOPE", "readonly")!!
    if (scope !in setOf("readonly", "nfe", "nfse", "all")) blocked("scope")
    val api1 = origin("WEBMANIA_API1_BASE_URL", "https://webmania.com.br/api")
    val filesOrigin = origin("WEBMANIA_FILES_BASE_URL", "https://nfe.webmania.com.br")
    val api2 = origin("WEBMANIA_API2_BASE_URL", "https://api.webmania.com.br")
    // The host allow-list is mandatory: absent or empty, no origin is trusted and nothing is sent.
    val hosts = (env("WEBMANIA_LIVE_ALLOWED_HOSTS") ?: "").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()
    if (hosts.isEmpty()) blocked("allowed_hosts")
    if (listOf(api1, filesOrigin, api2).any { it.host.lowercase() !in hosts }) blocked("host_not_allowed")
    val nfe = scope == "nfe" || scope == "all"; val nfse = scope == "nfse" || scope == "all"
    val nfePayload = if (nfe) payload("WEBMANIA_LIVE_NFE_PAYLOAD") else null
    val nfsePayload = if (nfse) payload("WEBMANIA_LIVE_NFSE_PAYLOAD") else null
    if (nfe || nfse) {
        // The ledger must be writable before any mutation; existing content is preserved.
        val path = Path.of(env("WEBMANIA_LIVE_LEDGER") ?: blocked("ledger"))
        try { Files.writeString(path, "", StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND) } catch (e: java.io.IOException) { blocked("ledger") }
        ledger = path
    }
    val names = listOf("WEBMANIA_CONSUMER_KEY", "WEBMANIA_CONSUMER_SECRET", "WEBMANIA_ACCESS_TOKEN", "WEBMANIA_ACCESS_TOKEN_SECRET", "WEBMANIA_ISSUER_TOKEN")
    val values = names.map { env(it) ?: blocked("credentials") }
    val config = clientConfig {
        deadline(Duration.ofSeconds(60))
        origin("api1/fiscal.openapi.json", api1); origin("api1/files.openapi.json", filesOrigin)
        origin("api2/emitter-reads.openapi.json", api2); origin("api2/nfse-issuance.openapi.json", api2); origin("api2/emitter-files.openapi.json", api2)
    }
    val company = CompanyCredentials.create(values[0], values[1], values[2], values[3])
    try {
        NfeClient(company, config).use { nfeClient ->
        NfeFilesClient(NfeFileAuth.company(company), config).use { nfeFiles ->
        EmitterClient(IssuerCredentials.create(values[4]), config).use { emitter ->
        EmitterFilesClient(EmitterFileAuth.companyToken(values[4]), config).use { emitterFiles ->
            read("nfeGetSefazStatus", null) { nfeClient.nfeGetSefazStatus() }
            read("nfeGetCertificateValidity", null) { nfeClient.nfeGetCertificateValidity() }
            read("nfeListTaxClasses", null) { nfeClient.nfeListTaxClasses() }
            read("emitterGetNfseProviderStatus", null) { emitter.emitterGetNfseProviderStatus() }
            if (nfePayload != null) nfeCycle(nfeClient, nfeFiles, nfePayload)
            if (nfsePayload != null) nfseCycle(emitter, emitterFiles, nfsePayload)
        } } } }
    } catch (stop: Unknown) { exitProcess(1) }
    exitProcess(if (failed) 1 else 0)
}
