package com.webmania.sdk.kotlin

/** Typed Reseller operation facade. Blocking calls observe native thread interruption. */
class ResellerClient(credentials: ResellerCredentials, config: com.webmania.sdk.ClientConfig = clientConfig()): AutoCloseable {
    private val delegate = engine(config, credentials.raw)
    override fun close() = delegate.close()
    override fun toString() = "ResellerClient[protected]"
    fun b2bV1CreateCompanies(request: com.webmania.sdk.model.api1b2b.B2bV1CreateCompaniesRequest = com.webmania.sdk.model.api1b2b.B2bV1CreateCompaniesRequest(), configure: com.webmania.sdk.model.api1b2b.B2bV1CreateCompaniesRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1b2b.B2bV1CreateCompaniesResponse> =
        delegate.b2bV1CreateCompanies(request.apply(configure))
    fun b2bV1ListCompanies(request: com.webmania.sdk.model.api1b2b.B2bV1ListCompaniesRequest = com.webmania.sdk.model.api1b2b.B2bV1ListCompaniesRequest(), configure: com.webmania.sdk.model.api1b2b.B2bV1ListCompaniesRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1b2b.B2bV1ListCompaniesResponse> =
        delegate.b2bV1ListCompanies(request.apply(configure))
    fun b2bV1GetProcessedRequests(request: com.webmania.sdk.model.api1b2b.B2bV1GetProcessedRequestsRequest = com.webmania.sdk.model.api1b2b.B2bV1GetProcessedRequestsRequest(), configure: com.webmania.sdk.model.api1b2b.B2bV1GetProcessedRequestsRequest.() -> Unit = {}): com.webmania.sdk.ApiResult<com.webmania.sdk.model.api1b2b.B2bV1GetProcessedRequestsResponse> =
        delegate.b2bV1GetProcessedRequests(request.apply(configure))
}
