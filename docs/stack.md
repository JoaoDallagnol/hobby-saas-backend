# Stack (Backend)

> Escopo: backend. Legenda: ✅ decidido · ⚠️ incerto, confirmar na implementação.

## Runtime
- **Java 25** (LTS) ✅
- **Spring Boot 4.1.x** ✅ (Jakarta EE 11, Jackson 3 — não usar padrões da linha 3.x, já EOL desde 30/06/2026)
- **Maven** ✅

## Banco de dados
- `spring-boot-starter-data-jpa`, `postgresql` (driver)
- `flyway-core` ✅ — migração de schema
- ⚠️ Mapeamento de `sessions.attributes` (JSONB) — confirmar se precisa de lib extra (ex: Hypersistence Utils) ou se Hibernate 6 resolve nativo.

## Autenticação
- `spring-boot-starter-oauth2-resource-server` — valida JWT do Keycloak. Nunca processa senha.

## Segurança / Rate limiting
- `bucket4j`

## Storage de fotos (Cloudflare R2)
- AWS SDK S3 (`software.amazon.awssdk:s3`) — R2 é S3-compatible, aponta pro endpoint do R2.
- ⚠️ Lib de processamento de imagem (resize + WebP + strip EXIF) não confirmada — Thumbnailator resolve resize; conversão WebP em Java incerta, pode acabar sendo binário externo (`cwebp`) via processo.

## Push
- `firebase-admin` — só a peça de envio via FCM, não é adoção da plataforma Firebase inteira.

## Monitoramento
- `sentry-spring-boot-starter`
- `spring-boot-starter-actuator`

## E-mail
- Nenhuma dependência necessária — Brevo configurado direto no realm do Keycloak (SMTP).

## Pagamento
- ⚠️ Provedor não decidido (Stripe/Asaas/Mercado Pago). Provavelmente sem SDK dedicado — `RestClient` nativo do Spring resolve chamada de API + validação de assinatura de webhook.

## Testes
- `spring-boot-starter-test` (JUnit 5, Mockito, AssertJ)
- `testcontainers` (+ Postgres) — testes de integração com banco real. Mock de JWT via `spring-security-test`, não subir Keycloak real em teste.

## Sugestões extras (não são decisão fechada, só recomendação padrão de mercado)
- `lombok`, `mapstruct`, `springdoc-openapi`

## Notas para mobile (fora de escopo deste doc)
- Projeto Firebase deve ser **o mesmo** entre backend (Admin SDK) e mobile (SDK cliente).
- Login em produção: **Authorization Code + PKCE**. Direct Access Grant (usuário/senha direto) é só pra teste local via curl/Postman.
