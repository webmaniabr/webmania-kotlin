package com.webmania.sdk.kotlin.models.api2reports

// Fixed aliases preserve the exact qualified Java DTO runtime; no data or enums are erased.
typealias FiscalDownloadReportV2Request = com.webmania.sdk.model.api2reports.FiscalDownloadReportV2Request
fun fiscalDownloadReportV2Request(configure: FiscalDownloadReportV2Request.() -> Unit = {}): FiscalDownloadReportV2Request { com.webmania.sdk.kotlin.RuntimePin.check(); return com.webmania.sdk.model.api2reports.FiscalDownloadReportV2Request().apply(configure) }
typealias ReportError = com.webmania.sdk.model.api2reports.ReportError
fun reportError(configure: ReportError.() -> Unit = {}): ReportError { com.webmania.sdk.kotlin.RuntimePin.check(); return com.webmania.sdk.model.api2reports.ReportError().apply(configure) }
typealias ReportHttpError = com.webmania.sdk.model.api2reports.ReportHttpError
fun reportHttpError(configure: ReportHttpError.() -> Unit = {}): ReportHttpError { com.webmania.sdk.kotlin.RuntimePin.check(); return com.webmania.sdk.model.api2reports.ReportHttpError().apply(configure) }
typealias ReportUuid = com.webmania.sdk.model.api2reports.ReportUuid
