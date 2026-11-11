# Detalhes técnicos de integração

Este documento reúne os detalhes técnicos do SDK para Kotlin que complementam o [README](../README.md): autenticação, URL base, limites de transporte, arquivos, relatórios e o tratamento do resultado fiscal. O texto é o mesmo que estava no README até esta versão.

## Escopo e disponibilidade

Integração de **Emissor Fiscal e Monitor Fiscal** em aplicações de servidor. O Emissor abrange NF-e/NFC-e na API1 e NFS-e, CT-e/CT-e OS, MDF-e, NFCom e DC-e na API2, com arquivos, eventos e as três operações gerenciais B2B públicas da API1. O Monitor mantém apenas as famílias de documentos e capacidades do seu contrato documentado.

Disponível no repositório público [github.com/webmaniabr/webmania-kotlin](https://github.com/webmaniabr/webmania-kotlin), com instalação a partir do clone (veja a seção Instalação). Ainda não há publicação em registro de pacotes; os nomes e versões abaixo identificam os pacotes gerados a partir desta fonte. Regras do provedor, aceitação fiscal real e plataformas adicionais permanecem com limitações explícitas.

## Visão geral para desenvolvedores

| Item | Detalhe |
|---|---|
| Status | Disponível no GitHub: versão `0.1.0-alpha.10` (`com.webmania:webmania-sdk-kotlin`), instalada a partir do clone do repositório em um repositório Maven local; ainda sem publicação no Maven Central ou em outro registro de pacotes |
| Runtime | Kotlin/JVM 2.4.21, JDK 17+; chamadas bloqueantes (sem suspend/coroutine); depende exclusivamente de `com.webmania:webmania-sdk:0.1.0-preview.9` (SDK Java) |
| Clientes (`com.webmania.sdk.kotlin`) | `NfeClient` (NF-e/NFC-e API 1.0), `NfeFilesClient`, `ResellerClient` (gerencial B2B API 1.0), `EmitterClient` (emissor API 2.0), `EmitterFilesClient`, `MonitorClient` (Monitor Fiscal) |
| Cobertura | Mapa de operações em [operation-map.json](../operation-map.json); modelos por módulo em `com.webmania.sdk.kotlin.models.*` |
| Erros | `com.webmania.sdk.ApiException` do SDK Java, com `kind()`, `status()` e `outcomeUncertain()` |
| Contrato | OpenAPI fixado em [runtime-contract.lock.json](../runtime-contract.lock.json) por `contract_digest` (`371a499f74012bb6b41f23642869dee4954e8394dcba7642edc701aedb3dd59e`), o mesmo do SDK Java fixado |

### Autenticação

- **NF-e/NFC-e (API 1.0)** — `CompanyCredentials.create(key, secret, token, tokenSecret)` é enviada pelo SDK Java nos cabeçalhos `X-Consumer-Key`, `X-Consumer-Secret`, `X-Access-Token` e `X-Access-Token-Secret`. `ResellerClient` usa `ResellerCredentials`.
- **API 2.0** — `IssuerCredentials.create(token)` (emissor) e `MonitorCredentials.create(token)` (Monitor) são enviadas como `Authorization: Bearer <token>`; são credenciais distintas. Os clientes de arquivo exigem `NfeFileAuth` ou `EmitterFileAuth`.

O exemplo abaixo lê as credenciais com `System.getenv` (`WEBMANIA_CONSUMER_KEY`, `WEBMANIA_CONSUMER_SECRET`, `WEBMANIA_ACCESS_TOKEN`, `WEBMANIA_ACCESS_TOKEN_SECRET`).

## Requisitos

Kotlin/JVM 2.4.21, JDK 17+. Perfil controlado: Linux/JDK 17 e 21. Não há API suspend/coroutine; as chamadas são bloqueantes. Android, Kotlin/Native e Kotlin/JS não estão qualificados.

## Instalação

Clone os repositórios públicos do SDK Java e deste SDK e instale os dois, nessa ordem, no mesmo repositório Maven local:

```sh
git clone https://github.com/webmaniabr/webmania-java.git
git clone https://github.com/webmaniabr/webmania-kotlin.git
mvn -B -ntp -f webmania-java/pom.xml -Dmaven.repo.local=/caminho/feed-maven install
mvn -B -ntp -f webmania-kotlin/pom.xml -Dmaven.repo.local=/caminho/feed-maven install
```

Declare no consumidor:

```xml
<dependency>
  <groupId>com.webmania</groupId>
  <artifactId>webmania-sdk-kotlin</artifactId>
  <version>0.1.0-alpha.10</version>
</dependency>
```

Use mvn -Dmaven.repo.local=/caminho/feed-maven compile no consumidor; esse repositório local recebe os dois SDKs e suas dependências no `install`. As coordenadas ainda não estão publicadas no Maven Central. Não é necessário executar o gerador. A dependência Java é exclusivamente com.webmania:webmania-sdk:0.1.0-preview.9; mantenha o JAR exato indicado no lock. Classes avulsas ou outro JAR no classpath são recusados.

## Primeira consulta

Configure somente a credencial do serviço escolhido, por instância, a partir de ambiente privado ou cofre de segredos. O exemplo não emite nem cancela documentos. Execute consultas apenas com conta e ambiente autorizados; uma origem de fixture controlada deve ser configurada explicitamente pela API do cliente.

```kotlin
import com.webmania.sdk.kotlin.*
val credentials = CompanyCredentials.create(
    System.getenv("WEBMANIA_CONSUMER_KEY"), System.getenv("WEBMANIA_CONSUMER_SECRET"),
    System.getenv("WEBMANIA_ACCESS_TOKEN"), System.getenv("WEBMANIA_ACCESS_TOKEN_SECRET")
)
NfeClient(credentials).use { client ->
    val result = client.nfeGetSefazStatus()
    println(result.httpStatus)
}
```

A construção do cliente não comprova habilitação da conta. Respostas de sucesso e conversões explícitas para JSON podem conter dados fiscais, credenciais novas ou URLs privadas. Exponha apenas os campos necessários à aplicação.

## Integração e limites

Os clientes NfeClient, ResellerClient, EmitterClient e MonitorClient recebem credenciais nominais de seu escopo. Os clientes de arquivo exigem NfeFileAuth ou EmitterFileAuth. A dependência Java exata verifica schema antes de I/O; o módulo não implementa outro transporte.

Os aliases em models.<módulo> referenciam DTOs Java mutáveis, com nullability Java. Factories Kotlin configuram esses mesmos objetos; não produzem data classes imutáveis ou sealed unions Kotlin. Configure o DTO antes de compartilhá-lo entre threads. `ofVariantN`/`asVariantN` preservam unions.

`toJsonBytes()` e `toJson()` preservam BigDecimal/BigInteger, false, zero, null e extensões sensíveis à caixa. `explicitNull` difere de `omit`; extensions não substituem propriedades declaradas. A conversão JSON é deliberadamente sensível. Mapeamento bean Jackson arbitrário não é suportado: serialize a árvore explícita ou os bytes do modelo.

Operações bloqueiam a thread; não há suspend/coroutine. `Thread.interrupt()`, deadline e close cancelam chamadas e preservam incerteza quando houve possível envio. Feche o cliente com use. Em ApiException, teste outcomeUncertain e a presença de reference antes de revelar value para conciliação.

`collectCompanies`, `collectCompanyMunicipalities` e `collectReports` exigem PaginationOptions finitas e retornam continuação. `waitImport` e `NfeClient.waitReport` exigem PollingOptions finitas e intervalo mínimo de cinco segundos. Estado desconhecido não significa conclusão.

`Download.fileKind` distingue PDF, ENCRYPTED_PDF, XML, ZIP, ENCRYPTED_ZIP, XLSX, CSV e HTML. Pode ser null em JSON pendente ou redirect. `isEncrypted` é independente de fileProtection; bytes são cópias defensivas e não são decriptados. ZIP comum recebe inspeção estrutural/metadados, sem promessa de CRC/descompressão de todo payload. O hash/origem de cada classe e recurso Java do SDK é conferido; classes avulsas e JAR alterado são recusados. Essa verificação não é sandbox contra agentes JVM ou classloaders hostis.

## Credenciais e resultado fiscal

Credenciais da empresa API1, revenda API1, Bearer do emissor API2 e Bearer Monitor pertencem a escopos diferentes. Arquivos exigem a modalidade de acesso documentada. Uma falha não autoriza trocar automaticamente o modo ou a credencial. Mantenha segredos no servidor.

O catálogo contém 102 operações habilitadas. `nfeDownloadIbsCbsEventXml` não está disponível nesta versão do SDK: a chamada falha com `CAPABILITY_DISABLED` e nenhuma requisição é enviada. Condições fiscais, municipais, de cadastro, provedor e implantação ainda pendentes continuam sujeitas ao contrato e ao serviço. Validação local, cálculo ou prévia não comprovam autorização fiscal.

Não há retry automático de operação nem seguimento automático de redirects. Depois de possível envio de uma operação com efeito, uma falha pode indicar resultado desconhecido. Preserve o seletor original com segurança, consulte a tentativa anterior e decida explicitamente como prosseguir. Uma referência protegida exige acesso deliberado e pode estar ausente; não é uma chave de idempotência.

## Arquivos e processamento

Verifique tipo de resultado, status e representação antes de consumir bytes. JSON pendente, HTML e redirect não comprovam PDF pronto. Proteção de PDF/ZIP é distinta da autorização de download; não há descriptografia automática nem confirmação de autenticidade fiscal. O reconhecimento de formatos cobre somente o perfil documentado desta implementação. Uma Location revelada continua privada e exige decisão explícita de origem e autenticação para outra transferência.

Paginação e espera são optativas e limitadas quando disponíveis. Um estado futuro ou desconhecido não significa conclusão. Não repita emissão, cancelamento ou importação inicial como se fossem consultas de processamento.

Para migrar uma integração anterior, remapeie métodos, modelos e escopos de credencial pela referência desta versão. Não há promessa de substituição automática de outro SDK; compare também o changelog quando disponível.

## Ambiente e URL base

Cada módulo usa a origem pública do contrato (`https://webmania.com.br/api`, `https://nfe.webmania.com.br` e `https://api.webmania.com.br`) até que outra seja configurada. Para outro ambiente, informe a origem HTTPS de cada módulo, com prefixo de caminho quando necessário:

```kotlin
val config = clientConfig {
    origin("api1/fiscal.openapi.json", java.net.URI.create(System.getenv("WEBMANIA_API1_BASE_URL")))
    origin("api1/files.openapi.json", java.net.URI.create(System.getenv("WEBMANIA_FILES_BASE_URL")))
    origin("api2/nfse-issuance.openapi.json", java.net.URI.create(System.getenv("WEBMANIA_API2_BASE_URL")))
}
NfeClient(credentials, config).use { client -> println(client.nfeGetSefazStatus().httpStatus) }
```

A origem precisa ser HTTPS (HTTP só em loopback explícito), sem informação de usuário, query, fragmento ou segmentos de ponto; uma origem inválida falha antes de DNS ou socket.

## Documentação para assistentes e contratos

Índices em Markdown para assistentes: [documentação Webmania](https://webmania.com.br/docs/llms.txt) e [API 2.0](https://api.webmania.com.br/llms.txt). Contratos OpenAPI: [API 2.0](https://api.webmania.com.br/docs/openapi) e [NF-e](https://webmania.com.br/docs/rest-api-nfe/?format=openapi).

No catálogo de SDKs, `kotlin` aparece como disponível no repositório público, com instalação pelo clone; plataforma Kotlin 2.4 / JVM 17+. Estar listado nesses índices não amplia a cobertura documentada deste SDK.
