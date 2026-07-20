# Stack (Backend)

> Escopo: backend. Legenda: ✅ decidido · ⚠️ incerto, confirmar na implementação.

## Runtime
- **Java 25** (LTS) ✅
- **Spring Boot 4.1.x** ✅ (Jakarta EE 11, Jackson 3 — não usar padrões da linha 3.x, já EOL desde 30/06/2026)
- **Maven** ✅
- `springdoc-openapi-starter-webmvc-ui` 3.0.3 ✅ — OpenAPI gerado a partir dos controllers reais; UI em `/swagger-ui.html`, spec em `/v3/api-docs`. Local habilitado por padrão; produção desabilitada por padrão e só exposta se `SPRINGDOC_ENABLED=true`.

## Banco de dados
- `spring-boot-starter-data-jpa`, `postgresql` (driver)
- `flyway-core` ✅ — migração de schema
- `sessions.attributes` em JSONB ✅ — mapeamento atual usa Hibernate ORM nativo com `@JdbcTypeCode(SqlTypes.JSON)` sobre `Map<String, Object>` e coluna `jsonb`; não foi necessária lib auxiliar.

## Autenticação
- `spring-boot-starter-security`
- `firebase-admin` — valida ID token/JWT do Firebase Authentication no backend e também cobre envio via FCM. Backend nunca processa senha.
- Validação de token no backend via Firebase Admin SDK.
- Em `local`, existe fallback opcional para bearer token estático controlado por env (`LOCAL_AUTH_*`) só para destravar desenvolvimento e teste manual dos endpoints protegidos antes da configuração real do Firebase.

## Segurança / Rate limiting
- Implementação atual: filtro próprio em memória por usuário autenticado (fallback por IP), configurado por env (`RATE_LIMIT_*`) ✅
- Bucket4j fica opcional para evolução futura se o rate limiting precisar sair do nível básico atual.

## Feature flags
- Configuração própria via `@ConfigurationProperties`, sem serviço SaaS adicional no MVP ✅
- Flags atuais controlam somente integrações com dependência externa/rollout: upload R2, localização Google Places e futuro processamento de fotos.
- `GET /api/features` fornece ao client autenticado o estado não sensível; tentativa de usar feature desligada retorna `503`.

## Storage de fotos (Cloudflare R2)
- AWS SDK S3 (`software.amazon.awssdk:s3`) ✅ — R2 é S3-compatible, aponta pro endpoint do R2.
- `cwebp/libwebp` via binário do pacote Debian `webp` no container ✅ — worker agendado gera WebP em dois tamanhos sem copiar metadata, com até 3 tentativas e status persistido. A abordagem evita dependência JNI frágil e aceita JPEG, PNG e WebP no MVP; HEIC/HEIF não são aceitos.

## Push
- `firebase-admin` — reutilizado para FCM e validação de autenticação.

## Monitoramento
- `sentry-spring-boot-starter`
- `spring-boot-starter-actuator`

## E-mail
- Nenhuma dependência obrigatória no backend neste momento. Auth por e-mail/senha fica no Firebase Authentication; e-mails transacionais de produto, se entrarem depois, serão avaliados separadamente.

## Pagamento
- ⚠️ Provedor não decidido (Stripe/Asaas/Mercado Pago). Provavelmente sem SDK dedicado — `RestClient` nativo do Spring resolve chamada de API + validação de assinatura de webhook.

## Testes
- `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ)
- `spring-boot-webmvc-test` ✅ — testes de contrato/controller slice para travar payloads, status HTTP e shape das APIs principais sem depender do banco.
- `testcontainers-junit-jupiter` + `testcontainers-postgresql` ✅ — testes de integração com Postgres real via Docker/Testcontainers.
- Mock/stub de autenticação: simular claims/token validado do Firebase nos testes; não subir provedor real de auth em teste automatizado.
- Em `local`, auth controlada de desenvolvimento é aceitável temporariamente para avançar domínio/API antes da integração real com Firebase dev estar concluída; o projeto já expõe um bearer estático opcional via env para esse fluxo.

## Sugestões extras (não são decisão fechada, só recomendação padrão de mercado)
- `lombok`, `mapstruct`

## Notas para mobile (fora de escopo deste doc)
- Projeto Firebase deve ser **o mesmo** entre backend (Admin SDK) e mobile (SDK cliente).
- Login por e-mail/senha e provedores sociais fica centralizado no Firebase Authentication.
- Quando integração real de mobile/auth virar pré-requisito, revisar com o usuário o cadastro do projeto Firebase, apps Android/iOS, SHA fingerprints, secrets e variáveis por ambiente antes de considerar dev/prod fechados.
