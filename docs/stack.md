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
- `spring-boot-starter-security`
- `firebase-admin` — valida ID token/JWT do Firebase Authentication no backend e também cobre envio via FCM. Backend nunca processa senha.
- ⚠️ Confirmar na implementação se a validação dos tokens ficará 100% via Firebase Admin SDK ou se parte do fluxo usará integração complementar com Spring Security Resource Server/JWK.

## Segurança / Rate limiting
- `bucket4j`

## Storage de fotos (Cloudflare R2)
- AWS SDK S3 (`software.amazon.awssdk:s3`) — R2 é S3-compatible, aponta pro endpoint do R2.
- ⚠️ Lib de processamento de imagem (resize + WebP + strip EXIF) não confirmada — Thumbnailator resolve resize; conversão WebP em Java incerta, pode acabar sendo binário externo (`cwebp`) via processo.

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
- `testcontainers` (+ Postgres) — testes de integração com banco real.
- Mock/stub de autenticação: simular claims/token validado do Firebase nos testes; não subir provedor real de auth em teste automatizado.

## Sugestões extras (não são decisão fechada, só recomendação padrão de mercado)
- `lombok`, `mapstruct`, `springdoc-openapi`

## Notas para mobile (fora de escopo deste doc)
- Projeto Firebase deve ser **o mesmo** entre backend (Admin SDK) e mobile (SDK cliente).
- Login por e-mail/senha e provedores sociais fica centralizado no Firebase Authentication.
- Quando integração real de mobile/auth virar pré-requisito, revisar com o usuário o cadastro do projeto Firebase, apps Android/iOS, SHA fingerprints, secrets e variáveis por ambiente antes de considerar dev/prod fechados.
