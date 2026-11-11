package com.webmania.sdk.kotlin

/** Typed Nfe operation facade. Blocking calls observe native thread interruption. */
class NfeClient(credentials: CompanyCredentials, config: com.webmania.sdk.ClientConfig = clientConfig()): AutoCloseable {
    private val delegate = engine(config, credentials.raw)
    override fun close() = delegate.close()
    override fun toString() = "NfeClient[protected]"
    fun nfeGetCertificateValidity(request: com.webmania.sdk.model.api1fiscal.NfeGetCertificateValidityRequest = com.webmania.sdk.model.api1fiscal.NfeGetCertificateValidityRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeGetCertificateValidityRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeGetCertificateValidityResponse> =
        delegate.nfeGetCertificateValidity(request.apply(configure))
    fun nfeGetSefazStatus(request: com.webmania.sdk.model.api1fiscal.NfeGetSefazStatusRequest = com.webmania.sdk.model.api1fiscal.NfeGetSefazStatusRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeGetSefazStatusRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeGetSefazStatusResponse> =
        delegate.nfeGetSefazStatus(request.apply(configure))
    fun nfeGetDocument(request: com.webmania.sdk.model.api1fiscal.NfeGetDocumentRequest = com.webmania.sdk.model.api1fiscal.NfeGetDocumentRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeGetDocumentRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeGetDocumentResponse> =
        delegate.nfeGetDocument(request.apply(configure))
    fun nfeQueryDocument(request: com.webmania.sdk.model.api1fiscal.NfeQueryDocumentRequest = com.webmania.sdk.model.api1fiscal.NfeQueryDocumentRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeQueryDocumentRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeQueryDocumentResponse> =
        delegate.nfeQueryDocument(request.apply(configure))
    fun nfeCreateCorrection(request: com.webmania.sdk.model.api1fiscal.NfeCreateCorrectionRequest = com.webmania.sdk.model.api1fiscal.NfeCreateCorrectionRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeCreateCorrectionRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeCreateCorrectionResponse> =
        delegate.nfeCreateCorrection(request.apply(configure))
    fun nfeCreateManifestation(request: com.webmania.sdk.model.api1fiscal.NfeCreateManifestationRequest = com.webmania.sdk.model.api1fiscal.NfeCreateManifestationRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeCreateManifestationRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeCreateManifestationResponse> =
        delegate.nfeCreateManifestation(request.apply(configure))
    fun nfeCancelDocument(request: com.webmania.sdk.model.api1fiscal.NfeCancelDocumentRequest = com.webmania.sdk.model.api1fiscal.NfeCancelDocumentRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeCancelDocumentRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeCancelDocumentResponse> =
        delegate.nfeCancelDocument(request.apply(configure))
    fun nfeVoidNumbering(request: com.webmania.sdk.model.api1fiscal.NfeVoidNumberingRequest = com.webmania.sdk.model.api1fiscal.NfeVoidNumberingRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeVoidNumberingRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeVoidNumberingResponse> =
        delegate.nfeVoidNumbering(request.apply(configure))
    fun nfeCreatePaymentEvent(request: com.webmania.sdk.model.api1fiscal.NfeCreatePaymentEventRequest = com.webmania.sdk.model.api1fiscal.NfeCreatePaymentEventRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeCreatePaymentEventRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeCreatePaymentEventResponse> =
        delegate.nfeCreatePaymentEvent(request.apply(configure))
    fun nfeCancelPaymentEvent(request: com.webmania.sdk.model.api1fiscal.NfeCancelPaymentEventRequest = com.webmania.sdk.model.api1fiscal.NfeCancelPaymentEventRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeCancelPaymentEventRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeCancelPaymentEventResponse> =
        delegate.nfeCancelPaymentEvent(request.apply(configure))
    fun nfeCancelIbsCbsEvent(request: com.webmania.sdk.model.api1fiscal.NfeCancelIbsCbsEventRequest = com.webmania.sdk.model.api1fiscal.NfeCancelIbsCbsEventRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeCancelIbsCbsEventRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeCancelIbsCbsEventResponse> =
        delegate.nfeCancelIbsCbsEvent(request.apply(configure))
    fun nfeListTaxClasses(request: com.webmania.sdk.model.api1fiscal.NfeListTaxClassesRequest = com.webmania.sdk.model.api1fiscal.NfeListTaxClassesRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeListTaxClassesRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeListTaxClassesResponse> =
        delegate.nfeListTaxClasses(request.apply(configure))
    fun nfeDeleteTaxClasses(request: com.webmania.sdk.model.api1fiscal.NfeDeleteTaxClassesRequest = com.webmania.sdk.model.api1fiscal.NfeDeleteTaxClassesRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeDeleteTaxClassesRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeDeleteTaxClassesResponse> =
        delegate.nfeDeleteTaxClasses(request.apply(configure))
    fun nfeSaveTaxClasses(request: com.webmania.sdk.model.api1fiscal.NfeSaveTaxClassesRequest = com.webmania.sdk.model.api1fiscal.NfeSaveTaxClassesRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeSaveTaxClassesRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeSaveTaxClassesResponse> =
        delegate.nfeSaveTaxClasses(request.apply(configure))
    fun nfeIssueDocument(request: com.webmania.sdk.model.api1fiscal.NfeIssueDocumentRequest = com.webmania.sdk.model.api1fiscal.NfeIssueDocumentRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeIssueDocumentRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeIssueDocumentResponse> =
        delegate.nfeIssueDocument(request.apply(configure))
    fun nfeIssueReturn(request: com.webmania.sdk.model.api1fiscal.NfeIssueReturnRequest = com.webmania.sdk.model.api1fiscal.NfeIssueReturnRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeIssueReturnRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeIssueReturnResponse> =
        delegate.nfeIssueReturn(request.apply(configure))
    fun nfeIssueAdjustment(request: com.webmania.sdk.model.api1fiscal.NfeIssueAdjustmentRequest = com.webmania.sdk.model.api1fiscal.NfeIssueAdjustmentRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeIssueAdjustmentRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeIssueAdjustmentResponse> =
        delegate.nfeIssueAdjustment(request.apply(configure))
    fun nfeIssueComplement(request: com.webmania.sdk.model.api1fiscal.NfeIssueComplementRequest = com.webmania.sdk.model.api1fiscal.NfeIssueComplementRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeIssueComplementRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeIssueComplementResponse> =
        delegate.nfeIssueComplement(request.apply(configure))
    fun fiscalUpdateCompany(request: com.webmania.sdk.model.api1fiscal.FiscalUpdateCompanyRequest = com.webmania.sdk.model.api1fiscal.FiscalUpdateCompanyRequest(), configure: com.webmania.sdk.model.api1fiscal.FiscalUpdateCompanyRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.FiscalUpdateCompanyResponse> =
        delegate.fiscalUpdateCompany(request.apply(configure))
    fun fiscalCreateOrPollReport(request: com.webmania.sdk.model.api1fiscal.FiscalCreateOrPollReportRequest = com.webmania.sdk.model.api1fiscal.FiscalCreateOrPollReportRequest(), configure: com.webmania.sdk.model.api1fiscal.FiscalCreateOrPollReportRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.FiscalCreateOrPollReportResponse> =
        delegate.fiscalCreateOrPollReport(request.apply(configure))
    fun nfeCreateIbsCbsEvent(request: com.webmania.sdk.model.api1fiscal.NfeCreateIbsCbsEventRequest = com.webmania.sdk.model.api1fiscal.NfeCreateIbsCbsEventRequest(), configure: com.webmania.sdk.model.api1fiscal.NfeCreateIbsCbsEventRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1fiscal.NfeCreateIbsCbsEventResponse> =
        delegate.nfeCreateIbsCbsEvent(request.apply(configure))
    fun waitReport(request: com.webmania.sdk.model.api1fiscal.FiscalCreateOrPollReportRequest, options: com.webmania.sdk.PollingOptions) = delegate.processing().fiscalReport(request, options)
}
