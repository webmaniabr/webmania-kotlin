package com.webmania.sdk.kotlin

/** Typed EmitterFiles operation facade. Blocking calls observe native thread interruption. */
class EmitterFilesClient(credentials: EmitterFileAuth, config: com.webmania.sdk.ClientConfig = clientConfig()): AutoCloseable {
    private val delegate = engine(config, credentials.raw)
    override fun close() = delegate.close()
    override fun toString() = "EmitterFilesClient[protected]"
    fun fileGetNfseXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetNfseXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetNfseXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetNfseXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetNfseXml(request.apply(configure))
    fun fileGetNfsePdf(request: com.webmania.sdk.model.api2emitterfiles.FileGetNfsePdfRequest = com.webmania.sdk.model.api2emitterfiles.FileGetNfsePdfRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetNfsePdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetNfsePdf(request.apply(configure))
    fun fileGetNfseRpsPdf(request: com.webmania.sdk.model.api2emitterfiles.FileGetNfseRpsPdfRequest = com.webmania.sdk.model.api2emitterfiles.FileGetNfseRpsPdfRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetNfseRpsPdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetNfseRpsPdf(request.apply(configure))
    fun fileGetCteXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetCteXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetCteXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetCteXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetCteXml(request.apply(configure))
    fun fileGetCteCancelXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetCteCancelXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetCteCancelXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetCteCancelXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetCteCancelXml(request.apply(configure))
    fun fileGetCtePdf(request: com.webmania.sdk.model.api2emitterfiles.FileGetCtePdfRequest = com.webmania.sdk.model.api2emitterfiles.FileGetCtePdfRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetCtePdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetCtePdf(request.apply(configure))
    fun fileGetCteCorrectionXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetCteCorrectionXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetCteCorrectionXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetCteCorrectionXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetCteCorrectionXml(request.apply(configure))
    fun fileGetCteCorrectionPdf(request: com.webmania.sdk.model.api2emitterfiles.FileGetCteCorrectionPdfRequest = com.webmania.sdk.model.api2emitterfiles.FileGetCteCorrectionPdfRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetCteCorrectionPdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetCteCorrectionPdf(request.apply(configure))
    fun fileGetMdfeXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetMdfeXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetMdfeXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetMdfeXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetMdfeXml(request.apply(configure))
    fun fileGetMdfePdf(request: com.webmania.sdk.model.api2emitterfiles.FileGetMdfePdfRequest = com.webmania.sdk.model.api2emitterfiles.FileGetMdfePdfRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetMdfePdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetMdfePdf(request.apply(configure))
    fun fileGetNfcomXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetNfcomXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetNfcomXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetNfcomXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetNfcomXml(request.apply(configure))
    fun fileGetNfcomCancelXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetNfcomCancelXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetNfcomCancelXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetNfcomCancelXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetNfcomCancelXml(request.apply(configure))
    fun fileGetNfcomPdf(request: com.webmania.sdk.model.api2emitterfiles.FileGetNfcomPdfRequest = com.webmania.sdk.model.api2emitterfiles.FileGetNfcomPdfRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetNfcomPdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetNfcomPdf(request.apply(configure))
    fun fileGetDceXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetDceXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetDceXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetDceXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetDceXml(request.apply(configure))
    fun fileGetDceCancelXml(request: com.webmania.sdk.model.api2emitterfiles.FileGetDceCancelXmlRequest = com.webmania.sdk.model.api2emitterfiles.FileGetDceCancelXmlRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetDceCancelXmlRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetDceCancelXml(request.apply(configure))
    fun fileGetDcePdf(request: com.webmania.sdk.model.api2emitterfiles.FileGetDcePdfRequest = com.webmania.sdk.model.api2emitterfiles.FileGetDcePdfRequest(), configure: com.webmania.sdk.model.api2emitterfiles.FileGetDcePdfRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.Download> =
        delegate.fileGetDcePdf(request.apply(configure))
}
