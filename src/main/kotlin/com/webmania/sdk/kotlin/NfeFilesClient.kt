package com.webmania.sdk.kotlin

/** Typed NfeFiles operation facade. Blocking calls observe native thread interruption. */
class NfeFilesClient(credentials: NfeFileAuth, config: com.webmania.sdk.ClientConfig = clientConfig()): AutoCloseable {
    private val delegate = engine(config, credentials.raw)
    override fun close() = delegate.close()
    override fun toString() = "NfeFilesClient[protected]"
    fun nfeDownloadDanfe(request: com.webmania.sdk.model.api1files.NfeDownloadDanfeRequest = com.webmania.sdk.model.api1files.NfeDownloadDanfeRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadDanfeRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadDanfe(request.apply(configure))
    fun nfeDownloadSimpleDanfe(request: com.webmania.sdk.model.api1files.NfeDownloadSimpleDanfeRequest = com.webmania.sdk.model.api1files.NfeDownloadSimpleDanfeRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadSimpleDanfeRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadSimpleDanfe(request.apply(configure))
    fun nfeDownloadLabelDanfe(request: com.webmania.sdk.model.api1files.NfeDownloadLabelDanfeRequest = com.webmania.sdk.model.api1files.NfeDownloadLabelDanfeRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadLabelDanfeRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadLabelDanfe(request.apply(configure))
    fun nfeDownloadCorrectionPdf(request: com.webmania.sdk.model.api1files.NfeDownloadCorrectionPdfRequest = com.webmania.sdk.model.api1files.NfeDownloadCorrectionPdfRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadCorrectionPdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadCorrectionPdf(request.apply(configure))
    fun nfeDownloadCorrectionPdfSequence(request: com.webmania.sdk.model.api1files.NfeDownloadCorrectionPdfSequenceRequest = com.webmania.sdk.model.api1files.NfeDownloadCorrectionPdfSequenceRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadCorrectionPdfSequenceRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadCorrectionPdfSequence(request.apply(configure))
    fun nfeDownloadXml(request: com.webmania.sdk.model.api1files.NfeDownloadXmlRequest = com.webmania.sdk.model.api1files.NfeDownloadXmlRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadXml(request.apply(configure))
    fun nfeDownloadCancellationXml(request: com.webmania.sdk.model.api1files.NfeDownloadCancellationXmlRequest = com.webmania.sdk.model.api1files.NfeDownloadCancellationXmlRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadCancellationXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadCancellationXml(request.apply(configure))
    fun nfeDownloadCorrectionXml(request: com.webmania.sdk.model.api1files.NfeDownloadCorrectionXmlRequest = com.webmania.sdk.model.api1files.NfeDownloadCorrectionXmlRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadCorrectionXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadCorrectionXml(request.apply(configure))
    fun nfeDownloadCorrectionXmlSequence(request: com.webmania.sdk.model.api1files.NfeDownloadCorrectionXmlSequenceRequest = com.webmania.sdk.model.api1files.NfeDownloadCorrectionXmlSequenceRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadCorrectionXmlSequenceRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadCorrectionXmlSequence(request.apply(configure))
    fun nfeDownloadManifestationXml(request: com.webmania.sdk.model.api1files.NfeDownloadManifestationXmlRequest = com.webmania.sdk.model.api1files.NfeDownloadManifestationXmlRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadManifestationXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadManifestationXml(request.apply(configure))
    fun nfeDownloadPaymentEventXml(request: com.webmania.sdk.model.api1files.NfeDownloadPaymentEventXmlRequest = com.webmania.sdk.model.api1files.NfeDownloadPaymentEventXmlRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadPaymentEventXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadPaymentEventXml(request.apply(configure))
    fun nfeDownloadPaymentEventXmlSequence(request: com.webmania.sdk.model.api1files.NfeDownloadPaymentEventXmlSequenceRequest = com.webmania.sdk.model.api1files.NfeDownloadPaymentEventXmlSequenceRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadPaymentEventXmlSequenceRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadPaymentEventXmlSequence(request.apply(configure))
    fun nfeDownloadIbsCbsEventXml(request: com.webmania.sdk.model.api1files.NfeDownloadIbsCbsEventXmlRequest = com.webmania.sdk.model.api1files.NfeDownloadIbsCbsEventXmlRequest(), configure: com.webmania.sdk.model.api1files.NfeDownloadIbsCbsEventXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.nfeDownloadIbsCbsEventXml(request.apply(configure))
    fun fiscalDownloadReport(request: com.webmania.sdk.model.api1files.FiscalDownloadReportRequest = com.webmania.sdk.model.api1files.FiscalDownloadReportRequest(), configure: com.webmania.sdk.model.api1files.FiscalDownloadReportRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fiscalDownloadReport(request.apply(configure))
    fun fiscalDownloadReportV2(request: com.webmania.sdk.model.api2reports.FiscalDownloadReportV2Request = com.webmania.sdk.model.api2reports.FiscalDownloadReportV2Request(), configure: com.webmania.sdk.model.api2reports.FiscalDownloadReportV2Request.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fiscalDownloadReportV2(request.apply(configure))
    /** Downloads the url of a completed report only from the configured report origin and /relatorios/ prefix. */
    fun downloadReport(url: String): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> = delegate.fiscalReportDownload(url)
}
