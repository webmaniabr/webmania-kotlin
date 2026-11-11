package com.webmania.sdk.kotlin

/** Typed Emitter operation facade. Blocking calls observe native thread interruption. */
class EmitterClient(credentials: IssuerCredentials, config: com.webmania.sdk.ClientConfig = clientConfig()): AutoCloseable {
    private val delegate = engine(config, credentials.raw)
    override fun close() = delegate.close()
    override fun toString() = "EmitterClient[protected]"
    fun emitterIssueCte(request: com.webmania.sdk.model.api2cteissuance.EmitterIssueCteRequest = com.webmania.sdk.model.api2cteissuance.EmitterIssueCteRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterIssueCteRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.CteFiscalResponse> =
        delegate.emitterIssueCte(request.apply(configure))
    fun emitterIssueSimplifiedCte(request: com.webmania.sdk.model.api2cteissuance.EmitterIssueSimplifiedCteRequest = com.webmania.sdk.model.api2cteissuance.EmitterIssueSimplifiedCteRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterIssueSimplifiedCteRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.CteFiscalResponse> =
        delegate.emitterIssueSimplifiedCte(request.apply(configure))
    fun emitterCancelCte(request: com.webmania.sdk.model.api2cteissuance.EmitterCancelCteRequest = com.webmania.sdk.model.api2cteissuance.EmitterCancelCteRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterCancelCteRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.FiscalEventResponse> =
        delegate.emitterCancelCte(request.apply(configure))
    fun emitterCorrectCte(request: com.webmania.sdk.model.api2cteissuance.EmitterCorrectCteRequest = com.webmania.sdk.model.api2cteissuance.EmitterCorrectCteRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterCorrectCteRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.FiscalEventResponse> =
        delegate.emitterCorrectCte(request.apply(configure))
    fun emitterConfirmCteDelivery(request: com.webmania.sdk.model.api2cteissuance.EmitterConfirmCteDeliveryRequest = com.webmania.sdk.model.api2cteissuance.EmitterConfirmCteDeliveryRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterConfirmCteDeliveryRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.FiscalEventResponse> =
        delegate.emitterConfirmCteDelivery(request.apply(configure))
    fun emitterCancelCteDelivery(request: com.webmania.sdk.model.api2cteissuance.EmitterCancelCteDeliveryRequest = com.webmania.sdk.model.api2cteissuance.EmitterCancelCteDeliveryRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterCancelCteDeliveryRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.FiscalEventResponse> =
        delegate.emitterCancelCteDelivery(request.apply(configure))
    fun emitterLinkCtePayment(request: com.webmania.sdk.model.api2cteissuance.EmitterLinkCtePaymentRequest = com.webmania.sdk.model.api2cteissuance.EmitterLinkCtePaymentRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterLinkCtePaymentRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.FiscalEventResponse> =
        delegate.emitterLinkCtePayment(request.apply(configure))
    fun emitterCancelCtePaymentLink(request: com.webmania.sdk.model.api2cteissuance.EmitterCancelCtePaymentLinkRequest = com.webmania.sdk.model.api2cteissuance.EmitterCancelCtePaymentLinkRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterCancelCtePaymentLinkRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.FiscalEventResponse> =
        delegate.emitterCancelCtePaymentLink(request.apply(configure))
    fun emitterGetCte(request: com.webmania.sdk.model.api2cteissuance.EmitterGetCteRequest = com.webmania.sdk.model.api2cteissuance.EmitterGetCteRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterGetCteRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.CteFiscalResponse> =
        delegate.emitterGetCte(request.apply(configure))
    fun emitterGetCtePaymentEvents(request: com.webmania.sdk.model.api2cteissuance.EmitterGetCtePaymentEventsRequest = com.webmania.sdk.model.api2cteissuance.EmitterGetCtePaymentEventsRequest(), configure: com.webmania.sdk.model.api2cteissuance.EmitterGetCtePaymentEventsRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2cteissuance.CtePaymentEvents> =
        delegate.emitterGetCtePaymentEvents(request.apply(configure))
    fun emitterIssueDce(request: com.webmania.sdk.model.api2dceissuance.EmitterIssueDceRequest = com.webmania.sdk.model.api2dceissuance.EmitterIssueDceRequest(), configure: com.webmania.sdk.model.api2dceissuance.EmitterIssueDceRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2dceissuance.DceFiscalResponse> =
        delegate.emitterIssueDce(request.apply(configure))
    fun emitterCancelDce(request: com.webmania.sdk.model.api2dceissuance.EmitterCancelDceRequest = com.webmania.sdk.model.api2dceissuance.EmitterCancelDceRequest(), configure: com.webmania.sdk.model.api2dceissuance.EmitterCancelDceRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2dceissuance.FiscalEventResponse> =
        delegate.emitterCancelDce(request.apply(configure))
    fun emitterGetDce(request: com.webmania.sdk.model.api2dceissuance.EmitterGetDceRequest = com.webmania.sdk.model.api2dceissuance.EmitterGetDceRequest(), configure: com.webmania.sdk.model.api2dceissuance.EmitterGetDceRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2dceissuance.DceFiscalResponse> =
        delegate.emitterGetDce(request.apply(configure))
    fun emitterGetNfseProviderStatus(request: com.webmania.sdk.model.api2emitterreads.EmitterGetNfseProviderStatusRequest = com.webmania.sdk.model.api2emitterreads.EmitterGetNfseProviderStatusRequest(), configure: com.webmania.sdk.model.api2emitterreads.EmitterGetNfseProviderStatusRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2emitterreads.EmitterGetNfseProviderStatusResponse> =
        delegate.emitterGetNfseProviderStatus(request.apply(configure))
    fun emitterGetNfcomServiceStatus(request: com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomServiceStatusRequest = com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomServiceStatusRequest(), configure: com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomServiceStatusRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomServiceStatusResponse> =
        delegate.emitterGetNfcomServiceStatus(request.apply(configure))
    fun emitterGetNfse(request: com.webmania.sdk.model.api2emitterreads.EmitterGetNfseRequest = com.webmania.sdk.model.api2emitterreads.EmitterGetNfseRequest(), configure: com.webmania.sdk.model.api2emitterreads.EmitterGetNfseRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2emitterreads.EmitterGetNfseResponse> =
        delegate.emitterGetNfse(request.apply(configure))
    fun emitterGetNfcom(request: com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomRequest = com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomRequest(), configure: com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomResponse> =
        delegate.emitterGetNfcom(request.apply(configure))
    fun emitterGetNfcomPaymentEvents(request: com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomPaymentEventsRequest = com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomPaymentEventsRequest(), configure: com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomPaymentEventsRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2emitterreads.EmitterGetNfcomPaymentEventsResponse> =
        delegate.emitterGetNfcomPaymentEvents(request.apply(configure))
    fun emitterManifestNfse(request: com.webmania.sdk.model.api2emitterreads.EmitterManifestNfseRequest = com.webmania.sdk.model.api2emitterreads.EmitterManifestNfseRequest(), configure: com.webmania.sdk.model.api2emitterreads.EmitterManifestNfseRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2emitterreads.EmitterManifestNfseResponse> =
        delegate.emitterManifestNfse(request.apply(configure))
    fun emitterIssueMdfe(request: com.webmania.sdk.model.api2mdfeissuance.EmitterIssueMdfeRequest = com.webmania.sdk.model.api2mdfeissuance.EmitterIssueMdfeRequest(), configure: com.webmania.sdk.model.api2mdfeissuance.EmitterIssueMdfeRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2mdfeissuance.MdfeFiscalResponse> =
        delegate.emitterIssueMdfe(request.apply(configure))
    fun emitterCloseMdfe(request: com.webmania.sdk.model.api2mdfeissuance.EmitterCloseMdfeRequest = com.webmania.sdk.model.api2mdfeissuance.EmitterCloseMdfeRequest(), configure: com.webmania.sdk.model.api2mdfeissuance.EmitterCloseMdfeRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2mdfeissuance.FiscalEventResponse> =
        delegate.emitterCloseMdfe(request.apply(configure))
    fun emitterCancelMdfe(request: com.webmania.sdk.model.api2mdfeissuance.EmitterCancelMdfeRequest = com.webmania.sdk.model.api2mdfeissuance.EmitterCancelMdfeRequest(), configure: com.webmania.sdk.model.api2mdfeissuance.EmitterCancelMdfeRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2mdfeissuance.FiscalEventResponse> =
        delegate.emitterCancelMdfe(request.apply(configure))
    fun emitterAddMdfeConductor(request: com.webmania.sdk.model.api2mdfeissuance.EmitterAddMdfeConductorRequest = com.webmania.sdk.model.api2mdfeissuance.EmitterAddMdfeConductorRequest(), configure: com.webmania.sdk.model.api2mdfeissuance.EmitterAddMdfeConductorRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2mdfeissuance.FiscalEventResponse> =
        delegate.emitterAddMdfeConductor(request.apply(configure))
    fun emitterGetMdfe(request: com.webmania.sdk.model.api2mdfeissuance.EmitterGetMdfeRequest = com.webmania.sdk.model.api2mdfeissuance.EmitterGetMdfeRequest(), configure: com.webmania.sdk.model.api2mdfeissuance.EmitterGetMdfeRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2mdfeissuance.MdfeFiscalResponse> =
        delegate.emitterGetMdfe(request.apply(configure))
    fun emitterIssueNfcom(request: com.webmania.sdk.model.api2nfcomissuance.EmitterIssueNfcomRequest = com.webmania.sdk.model.api2nfcomissuance.EmitterIssueNfcomRequest(), configure: com.webmania.sdk.model.api2nfcomissuance.EmitterIssueNfcomRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2nfcomissuance.NfcomFiscalResponse> =
        delegate.emitterIssueNfcom(request.apply(configure))
    fun emitterCancelNfcom(request: com.webmania.sdk.model.api2nfcomissuance.EmitterCancelNfcomRequest = com.webmania.sdk.model.api2nfcomissuance.EmitterCancelNfcomRequest(), configure: com.webmania.sdk.model.api2nfcomissuance.EmitterCancelNfcomRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2nfcomissuance.FiscalEventResponse> =
        delegate.emitterCancelNfcom(request.apply(configure))
    fun emitterIssueNfse(request: com.webmania.sdk.model.api2nfseissuance.EmitterIssueNfseRequest = com.webmania.sdk.model.api2nfseissuance.EmitterIssueNfseRequest(), configure: com.webmania.sdk.model.api2nfseissuance.EmitterIssueNfseRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2nfseissuance.NfseFiscalResponse> =
        delegate.emitterIssueNfse(request.apply(configure))
    fun emitterCancelNfse(request: com.webmania.sdk.model.api2nfseissuance.EmitterCancelNfseRequest = com.webmania.sdk.model.api2nfseissuance.EmitterCancelNfseRequest(), configure: com.webmania.sdk.model.api2nfseissuance.EmitterCancelNfseRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2nfseissuance.NfseFiscalResponse> =
        delegate.emitterCancelNfse(request.apply(configure))
    fun emitterReplaceNfse(request: com.webmania.sdk.model.api2nfseissuance.EmitterReplaceNfseRequest = com.webmania.sdk.model.api2nfseissuance.EmitterReplaceNfseRequest(), configure: com.webmania.sdk.model.api2nfseissuance.EmitterReplaceNfseRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api2nfseissuance.NfseFiscalResponse> =
        delegate.emitterReplaceNfse(request.apply(configure))
}
