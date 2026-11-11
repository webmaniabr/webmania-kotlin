# Changelog

## 0.1.0-alpha.10 (unreleased)

- README rewritten for integrators: requirements, installation, credentials from environment variables, homologation examples that issue, consult and cancel an NF-e and an NFS-e, and the method list per product. The technical details that were in the README moved unchanged to `docs/INTEGRATION.md`, `docs/TESTING.md`, `docs/OPERATIONS.md` and `examples/homologacao/README.md`. Documentation only.
- README: the SDK is presented as available from the public GitHub repository and installed from a clone (`git clone https://github.com/webmaniabr/webmania-kotlin.git`); it is still not published to a package registry. Documentation only: code and contract are unchanged.
- Pins the Java SDK whose in-process DNS fallback runs at most 16 lookups at once, so a hanging DNS cannot accumulate threads (new JAR, POM and sources SHA-256; the contract digest is unchanged).
- Homologation example: `WEBMANIA_LIVE_ALLOWED_HOSTS` is required (absent or empty, the example exits 77 before any request), and a payload is accepted only when its top-level `ambiente` is the JSON integer `2` (the text `"2"` is refused).
- Homologation example: after an NFS-e cancellation the example keeps consulting while the document is in `processando_cancelamento`; only `cancelado` counts as success. `HomologationExampleTest` compiles the example and checks offline, against a loopback TLS fixture, the host allow-list, the `ambiente` guard, the ledgering of an unknown cancellation, the post-cancellation consult (also with scope `nfse` alone), the cancellation of a document approved after the issue call and that a refused document is never cancelled; the packaged library is unchanged.
- Pins the Java SDK rebuilt from the current public contracts: tax-class list items (`nfeListTaxClasses`) are read with dedicated schemas that accept every value a saved class returns (ICMS `nao_contribuinte` as a boolean or `"0"`/`"1"`, a `null` rate when none is stored, one object instead of a list for NFCom and NFS-e groups, `type` always present); the tax-class save request (`nfeSaveTaxClasses`) follows the rules the save enforces (per-UF ICMS rates are `{estado, aliquota}` instead of `{estado, valor}`, `icms`, `ipi`, `pis` and `cofins` need at least one scenario, IBS/CBS `credito_presumido` is one object, `aliquota_carga_efetiva` is no longer published).
- Pins the Java SDK rebuilt from the current public contracts: tax-class list totals (`tributos_federais`, `tributos_estaduais`) accept an empty string for an unset value; tax-class `situacao_tributaria` has 2 digits for IPI, PIS and COFINS and 2 to 3 for ICMS; NFS-e `impostos.reducao` and `impostos.cp` accept text or a JSON number.
- Homologation example: the issued document is consulted until a terminal status and cancelled once a consult shows `aprovado`, whether the issue call approved it or it was approved later; a refused document gets no other mutation, and one without a terminal status within the bound is ledgered as `unknown` (never resent); the refused NF-e status is `reprovado`; decimal numbers of the payload files are read exactly (`BigDecimal`), as the SDK refuses binary floating point.
- Pins the Java SDK `com.webmania:webmania-sdk:0.1.0-preview.9` rebuilt from the current public contracts (14 modules, 103 operations, 102 enabled), by JAR, POM and sources SHA-256 in `dependencies.lock.json` and `runtime-contract.lock.json`; the contract is identified by `contract_digest` instead of a commit.
- Request-body aliases of the API 2.0 issuance operations follow the operation-derived Java names (`<OperationId>Payload`).
- `NfeFilesClient` gains `fiscalDownloadReportV2` and `downloadReport(url)` for current reports; legacy CSV/XLSX reports labeled `text/xml` are identified by their bytes.
- `nfeDownloadIbsCbsEventXml` is not available in this release.
- Runtime dependencies follow the Java SDK: Jackson `jackson-databind`/`jackson-core` 2.21.7 and `jackson-annotations` 2.21.
- The package version drops the local qualifier (`0.1.0-alpha.10`).
- New optional homologation example in `examples/homologacao`.

## Earlier previews

- 0.1.0-alpha.5: resolves every Java SDK class without running initializers and requires the pinned archive as their common origin; verifies contract resource origin and bytes before SDK use.
- 0.1.0-alpha.4: adds `fileKind`, `isEncrypted` and `fileProtection` accessors and complete file representation fixtures.
- 0.1.0-alpha.3: first scoped Kotlin/JVM facades over the Java SDK, typed model aliases and receiver-lambda factories.
