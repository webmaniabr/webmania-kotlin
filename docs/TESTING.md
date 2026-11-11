# Testes e desenvolvimento

Como compilar e testar o SDK para Kotlin a partir da fonte. O contrato fixado e a versão estão descritos em [INTEGRATION.md](INTEGRATION.md).

```sh
mvn -B -ntp -Dmaven.repo.local=.maven-local install
```

A suíte Surefire usa HTTP/TLS loopback com fixtures sintéticas e não faz chamadas fiscais reais. O repositório Maven local precisa conter o JAR Java exato indicado em [dependencies.lock.json](../dependencies.lock.json).
