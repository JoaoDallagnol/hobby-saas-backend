# Infraestrutura e Segurança

> 🟢 MVP (bloqueia lançamento) · 🔵 Fase seguinte (implementa quando a dor aparecer) · ⚠️ pendente de decisão
>
> Estado consolidado, secrets necessários e pendências operacionais: `docs/relatorio-status-mvp.md`.

## Autenticação 🟢

- **Firebase Authentication** (modo padrão, sem upgrade para Identity Platform neste momento).
- Firebase Authentication guarda a credencial e executa o fluxo de login; app **nunca** vê/armazena senha.
- `users.id` = `sub`/`uid` do token emitido pelo Firebase Authentication, armazenado como string (não gerar id próprio, não ter coluna de mapeamento separada).
- Provisionamento **just-in-time**: primeira request autenticada de um user novo cria a linha em `users` na hora, a partir do token validado.
- Separar projeto/credenciais de `dev` e `prod` no Firebase desde cedo. Não reutilizar configuração de produção em ambiente local.
- Quando a integração real virar pré-requisito, orientar o usuário sobre criação de projeto, apps, métodos de login, service account, restrições e secrets antes de fechar a entrega.
- Ordem pragmática permitida: backend pode evoluir primeiro em domínio, banco, contratos e regras de negócio com auth controlada de `local`/`test`; integração real com Firebase continua obrigatória antes de fechar MVP funcional end-to-end ou publicar clientes.
- Em `local`, existe suporte explícito a bearer token estático controlado por env (`LOCAL_AUTH_*`) para desenvolvimento do backend sem projeto Firebase pronto. Esse modo é aceito só no profile `local` e não substitui a validação real do Firebase em `dev`/`prod`.

## Hospedagem 🟢

- **VPS única** (Hostinger, linha KVM, datacenter São Paulo) rodando Spring Boot + Postgres via **Docker Compose**. Não usar PaaS por serviço (Railway/Render) neste desenho atual.
- Descartados: Oracle Cloud Free Tier (instabilidade/limite reduzido), Hetzner (EUR + datacenter EU).
- App deve ser **stateless** (sem estado em memória local, config via env var) — pré-requisito barato pra escalar depois sem reescrever.
- 🔵 Load balancer/API Gateway: não implementar agora. Evolução: Postgres gerenciado → Nginx como LB simples → API Gateway só se virar multi-serviço de verdade.

## Segurança 🟢

- DDoS: Cloudflare (free) como proxy DNS.
- Rate limit: Cloudflare (free) + Nginx (`limit_req`) + filtro em memória no backend por usuário autenticado (fallback por IP). Bucket4j fica como opção futura se o limite em aplicação precisar evoluir.
- CORS do backend deve ser explícito por ambiente via `CORS_ALLOWED_ORIGINS`; não deixar `*` em produção.
- Headers mínimos no backend/API: `X-Content-Type-Options`, `X-Frame-Options`, `Referrer-Policy`; permissões do browser restritas para recursos não usados pela API.
- **Nunca confiar em campo de plano/permissão vindo do cliente** (ex: `isPremium`) — checar sempre contra o banco.
- Pagamento: status de assinatura só muda via **webhook assinado** do provedor, nunca por chamada do client dizendo "paguei".
- Autorização por recurso em todo endpoint (`session.user_id == usuário autenticado`, não só "está logado").
- Tokens do Firebase devem ser validados no backend; nunca confiar em `uid`, `email`, plano ou papel enviados pelo client fora do token validado + banco.
- SQL Injection: mitigado por padrão via Spring Data JPA (queries parametrizadas); risco só em query nativa concatenada.
- HTTPS obrigatório (Let's Encrypt). Segredos só em env var, nunca no código.
- O repositório versiona o desenho mínimo esperado de produção em `docker-compose.prod.yml` + `deploy/nginx/default.conf.template`: Nginx termina TLS, redireciona 80→443, envia `X-Forwarded-*` corretos ao backend e aplica `limit_req` de borda.
- Mobile: token em Keychain/Keystore nativo; API key do Google Places restrita por package/bundle ID + assinatura; nenhum segredo real embutido no app.
- 🔵 Certificate pinning e detecção de root/jailbreak: não agora.

## Configuração por ambiente 🟢

- Perfis mínimos obrigatórios: `local` e `prod`.
- `application.yaml` fica como base neutra, sem segredo real.
- `application-local.yaml` suporta desenvolvimento local, com Adobe S3Mock persistindo mídia em volume/pasta Docker e demais integrações externas opcionais temporariamente vazias.
- `application-prod.yaml` assume variáveis obrigatórias para produção e não deve depender de fallback inseguro.
- OpenAPI/Swagger:
  - `local`: habilitado por padrão (`SPRINGDOC_ENABLED=true`).
  - `prod`: desabilitado por padrão (`SPRINGDOC_ENABLED=false`) e só exposto quando houver necessidade explícita.
- Nunca usar a mesma credencial/projeto Firebase, R2 ou Google Places entre `dev` e `prod`.
- Nenhum secret, JSON de service account, token, private key ou senha deve ser commitado em `.env`, compose, código, docs ou examples com valor real.
- Feature flags são env vars do servidor e não permissões do usuário. O endpoint autenticado `GET /api/features` expõe apenas o estado booleano necessário para o client adaptar a UI.
- Flags atuais:
  - `FEATURE_PHOTO_UPLOADS_ENABLED`: habilita presigned upload e associação de keys R2 do próprio usuário;
  - `FEATURE_SESSION_LOCATION_ENABLED`: habilita resolução de `place_id` em sessões;
  - `FEATURE_PHOTO_PROCESSING_ENABLED`: habilita o worker de thumbnail/WebP/remoção de EXIF.
  - `FEATURE_GAMIFICATION_ENABLED`: rollout de metas, XP, badges, recordes e desafio mensal;
  - `FEATURE_PLUS_ENABLED`: rollout das superfícies Plus; não concede entitlement.
- Em `local`, integrações externas ficam desligadas por padrão. Em `prod`, fotos, processamento e localização ficam ligados por padrão, mas podem ser desligados durante rollout/incidente; upload não deve ficar ativo em produção com processamento desligado.
- No profile `prod`, o health indicator marca a aplicação como `DOWN` quando Firebase ou uma integração habilitada não possui a configuração mínima. Os detalhes listam apenas nomes lógicos ausentes, nunca valores.
- O repositório protege contra vazamento acidental com:
  - `.gitignore` cobrindo `.env`, certificados/TLS e arquivos típicos de service account;
  - `scripts/check-no-secrets.sh` para varredura rápida de padrões óbvios antes de commit/review.

### Variáveis de ambiente por tipo

#### Base local

- `SPRING_PROFILES_ACTIVE=local`
- `SPRINGDOC_ENABLED=true`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `RATE_LIMIT_ENABLED`
- `RATE_LIMIT_CAPACITY`
- `RATE_LIMIT_REFILL_TOKENS`
- `RATE_LIMIT_REFILL_MINUTES`
- `FEATURE_PHOTO_UPLOADS_ENABLED`
- `FEATURE_SESSION_LOCATION_ENABLED`
- `FEATURE_PHOTO_PROCESSING_ENABLED`
- `FEATURE_GAMIFICATION_ENABLED`
- `FEATURE_PLUS_ENABLED`
- `CWEBP_BINARY`
- `PHOTO_PROCESSING_POLL_DELAY_MS`
- `PHOTO_DELETION_POLL_DELAY_MS`
- `PHOTO_VISIBILITY_POLL_DELAY_MS`

#### Integrações opcionais enquanto o backend central evolui

- `FIREBASE_PROJECT_ID`
- `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` ou `FIREBASE_SERVICE_ACCOUNT_PATH`
- `LOCAL_AUTH_ENABLED`
- `LOCAL_AUTH_TOKEN`
- `LOCAL_AUTH_USER_ID`
- `LOCAL_AUTH_EMAIL`
- `LOCAL_AUTH_NAME`
- `LOCAL_AUTH_EMAIL_VERIFIED`
- `R2_ENDPOINT`
- `R2_PRESIGN_ENDPOINT`
- `R2_PRIVATE_BUCKET`
- `R2_PUBLIC_BUCKET`
- `R2_PUBLIC_BASE_URL`
- `R2_ACCESS_KEY`
- `R2_SECRET_KEY`
- `CLOUDFLARE_ZONE_ID`
- `CLOUDFLARE_API_TOKEN`
- `GOOGLE_PLACES_API_KEY`
- `SENTRY_DSN`
- `BREVO_SMTP_USERNAME`
- `BREVO_SMTP_PASSWORD`

#### Produção mínima

- `SPRING_PROFILES_ACTIVE=prod`
- `SPRINGDOC_ENABLED=false`
- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `CORS_ALLOWED_ORIGINS`
- `RATE_LIMIT_ENABLED`
- `RATE_LIMIT_CAPACITY`
- `RATE_LIMIT_REFILL_TOKENS`
- `RATE_LIMIT_REFILL_MINUTES`
- `FEATURE_PHOTO_UPLOADS_ENABLED`
- `FEATURE_SESSION_LOCATION_ENABLED`
- `FEATURE_PHOTO_PROCESSING_ENABLED`
- `FEATURE_GAMIFICATION_ENABLED`
- `FEATURE_PLUS_ENABLED`
- `CWEBP_BINARY`
- `PHOTO_PROCESSING_POLL_DELAY_MS`
- `PHOTO_DELETION_POLL_DELAY_MS`
- `PHOTO_VISIBILITY_POLL_DELAY_MS`
- `FIREBASE_PROJECT_ID`
- `FIREBASE_SERVICE_ACCOUNT_JSON_BASE64` ou `FIREBASE_SERVICE_ACCOUNT_PATH`
- `APP_DOMAIN`
- `APP_UPSTREAM`
- `SSL_CERTIFICATE_PATH`
- `SSL_CERTIFICATE_KEY_PATH`
- `NGINX_CERTS_DIR`
- `NGINX_CLIENT_MAX_BODY_SIZE`
- `NGINX_RATE_LIMIT_RPS`
- `NGINX_RATE_LIMIT_BURST`

#### Produção quando a feature correspondente estiver ativa

- Fotos R2: `R2_ENDPOINT`, `R2_PRIVATE_BUCKET`, `R2_PUBLIC_BUCKET`, `R2_PUBLIC_BASE_URL`, `R2_ACCESS_KEY`, `R2_SECRET_KEY`, `CLOUDFLARE_ZONE_ID`, `CLOUDFLARE_API_TOKEN`. `R2_PRESIGN_ENDPOINT` só é necessário quando o endpoint visto pelo client difere do endpoint interno.
- Localização Google Places: `GOOGLE_PLACES_API_KEY`
- Erros em produção: `SENTRY_DSN`
- E-mail transacional próprio: `BREVO_SMTP_USERNAME`, `BREVO_SMTP_PASSWORD`

## Gestão de secrets 🟢

- `local`:
  - usar shell local, `.env` não versionado ou mecanismo equivalente fora do git;
  - manter `.env.example` só com placeholders;
  - se usar auth local temporária, ativar `LOCAL_AUTH_ENABLED=true`, definir `LOCAL_AUTH_TOKEN` forte e usar esse bearer só em ambiente de desenvolvimento;
  - o Compose local usa S3Mock com credenciais fictícias e dois buckets persistidos em volume; esses valores não são secrets nem devem ser copiados para produção;
  - se quiser validar throttling localmente, ativar `RATE_LIMIT_ENABLED=true` e ajustar `RATE_LIMIT_*` conforme o volume de teste;
  - se precisar service account do Firebase, preferir arquivo local fora do repositório ou variável base64 injetada só no ambiente do dev.
  - se usar certificado local de teste ou service account em arquivo, manter fora do git e fora de paths versionados não ignorados.
- `prod`:
  - usar env vars do servidor/compose final ou secret file fora do repositório;
  - restringir acesso aos secrets ao menor conjunto de usuários/processos possível;
  - separar credenciais por ambiente e por serviço;
  - preparar rotação sem alteração de código.
  - template operacional versionado: `deploy/production.env.example`; copiar para path fora do git (ex: `/opt/hobby-saas/prod.env`), aplicar permissão restrita (`chmod 600`) e subir o compose com `--env-file`.
- Logs e erros:
  - não logar bearer token, API key, DSN com credencial, senha SMTP, segredo R2 ou JSON completo de service account;
  - erros devem falhar de forma segura sem despejar config sensível na resposta HTTP.
  - bearer token validado não fica retido como credencial dentro do `SecurityContext` e mensagens internas do Firebase não são refletidas diretamente na resposta.

## E-mail transacional 🟢

- **Brevo**: 300 e-mails/dia, free permanente, sem cartão.
- Não é pré-requisito para o login básico no Firebase Authentication.
- Usar quando o produto realmente precisar enviar e-mail transacional próprio (ex: comunicação operacional, alertas, notificações fora do auth).
- Não montar SMTP próprio: problema não é código, é reputação de IP (spam por padrão).
- Depende de domínio próprio + registros SPF/DKIM/DMARC.

## Notificação push 🔵 (depende do mobile existir)

- **FCM** (Firebase Cloud Messaging) — grátis, ilimitado, unifica iOS/Android.
- Fluxo: app registra token de dispositivo → backend salva (`user_devices`) → backend chama API do FCM quando notificar.
- Uso: lembrete de abandono (premium), streak.

## Backup do banco 🟢 (bloqueia — risco real de perda de dado)

- `pg_dump` via cron diário → upload pro Cloudflare R2. Retenção rotativa (7 diários + 4 semanais).
- Backup semanal de VM da Hostinger **não substitui isso** (snapshot pode capturar estado inconsistente do Postgres).
- Testar restauração pelo menos 1x antes de precisar de verdade.
- Scripts base do repositório:
  - `scripts/backup-postgres-to-r2.sh`
  - `scripts/restore-postgres-backup.sh`
- Pré-requisitos operacionais do host/job:
  - `pg_dump`/`psql`
  - AWS CLI configurado para endpoint S3-compatible do R2
  - env vars de banco e R2 carregadas fora do git
- Estratégia mínima de execução:
  - cron diário no host/VPS
  - upload para prefixo versionado por data
  - retenção/limpeza automatizada no script de backup por env:
    - `BACKUP_DAILY_RETENTION` (default `7`)
    - `BACKUP_WEEKLY_RETENTION` (default `4`)
    - `BACKUP_WEEKLY_DAY_UTC` (default `7`, domingo UTC)

## Monitoramento 🟢 (esforço baixo, ativar desde já)

- **Sentry** plano Developer: grátis, 5k erros/mês, 1 usuário, inclui 1 uptime monitor.
- 🔵 Métricas de negócio/tráfego: Spring Actuator + Micrometer (nativo) → visualização via Prometheus + Grafana só quando sentir falta. Log estruturado via Grafana Loki, só se `docker compose logs` via SSH incomodar. Atenção: 6 containers na mesma VPS pode exigir upgrade de plano (KVM1 → KVM2).

## CI/CD 🔵 (não bloqueia 1º deploy manual, mas fácil/grátis, ativar cedo)

- **GitHub Actions**: grátis ilimitado em repo público; 2.000 min/mês grátis em privado.
- Pipeline simples: push → testes → SSH na VPS → `git pull && docker compose up -d --build`. Sem registry de imagem externo.
- Hostinger não substitui isso (deploy 1-clique da Hostinger é pra apps prontas de terceiro, não pra atualizar código próprio continuamente).

## Cadastros e contas operacionais ⚠️

- Quando qualquer integração sair do papel e virar dependência real de dev/prod, parar e orientar explicitamente o usuário sobre o cadastro/configuração necessária.
- Runbook operacional consolidado: `docs/cadastros-e-configuracoes-externas.md`.
- Cadastros que provavelmente serão necessários neste projeto:
  - Projeto Firebase `dev` e `prod`, com método de login habilitado e service account para backend.
  - Conta Hostinger/VPS para produção.
  - Conta Cloudflare + zona do domínio.
  - Bucket/credenciais do Cloudflare R2.
  - Projeto Google Places com billing habilitado e chaves restritas.
  - Contas de loja mobile (Google Play e Apple) quando publicação virar escopo.
- Não assumir que cadastro externo já existe só porque a integração está documentada.

### Checklist operacional de cadastro/configuração

#### Firebase Authentication

- criar projeto `dev` e projeto `prod`;
- habilitar método(s) de login necessários;
- gerar service account para o backend;
- registrar apps clientes quando mobile/web real entrarem no fluxo;
- guardar `project_id` e credencial fora do git.

#### Hostinger / VPS

- provisionar VPS final;
- configurar acesso SSH seguro;
- instalar Docker e Docker Compose plugin;
- instalar também utilitários operacionais do backup (`postgresql-client`, AWS CLI ou equivalente);
- configurar firewall e HTTPS reverso antes de expor produção publicamente;
- definir onde as env vars/secrets de produção ficarão armazenadas.
- aplicar `docker-compose.prod.yml` com `APP_DOMAIN` e paths de certificado válidos antes de abrir tráfego público;
- manter o app sem porta publicada diretamente na internet; entrada pública deve passar pelo Nginx/Cloudflare.

### Proxy/HTTPS esperado em produção

- Cloudflare na borda pública.
- Nginx na VPS como reverse proxy/TLS terminator.
- Spring Boot atrás do proxy, somente na rede interna do compose.
- `server.forward-headers-strategy=framework` já está ativo no profile `prod`; o proxy precisa enviar `Host`, `X-Forwarded-For`, `X-Forwarded-Host` e `X-Forwarded-Proto`.
- Certificado TLS não é commitado. O compose de produção espera arquivos montados em `NGINX_CERTS_DIR` e paths coerentes com `SSL_CERTIFICATE_PATH` / `SSL_CERTIFICATE_KEY_PATH`.

#### Cloudflare / R2

- criar conta e zona DNS do domínio quando o nome do app fechar;
- criar buckets de fotos privado/público por ambiente e um bucket privado separado para backups;
- gerar credenciais com menor privilégio possível;
- configurar domínio público/CDN somente no bucket público de variantes processadas;
- configurar token limitado a Cache Purge da zona para a transição `everyone` → `only_me`.

#### Google Places

- criar projeto com billing habilitado;
- ativar Places API necessária;
- restringir chave por uso e revisar escopo;
- manter chave separada por ambiente se necessário.

## Domínio/DNS ⚠️ pendente (nome do app não decidido)

Passo a passo: registrar domínio → apontar nameservers pro Cloudflare → criar registros (VPS, `api.dominio`, `app.dominio` se necessário, SPF/DKIM/DMARC do Brevo quando houver e-mail transacional próprio).

| Registrador | Preço/ano | WHOIS privado | |
|---|---|---|---|
| Registro.br (.com.br) | R$40–96 | Não | Exige CPF/CNPJ, mais confiança BR |
| Cloudflare Registrar (.com/.app) | ~US$9–10 (custo) | Sim, grátis | Mesmo painel do Cloudflare |
| Revendedoras BR | R$25–96 | Varia | Pix/boleto, suporte PT-BR |

## Analytics de produto 🔵 (não bloqueia MVP com poucos usuários conhecidos)

- **PostHog** — free tier generoso, self-hosted opcional (mesma VPS). Cobre funil/retenção/coorte — diferente de Sentry (erro) e Prometheus (infra).

## Pagamento ⚠️ pendente

A fundação de entitlement usa `FREE`/`PLUS` no banco, mas não existe checkout nem endpoint de auto-upgrade. Até o provedor ser escolhido, uma conta Plus só pode ser preparada por operação interna controlada. Na integração real, mudança de assinatura ocorrerá exclusivamente por webhook assinado, idempotente e auditável. Nenhum client envia plano confiável.

| Provedor | Mensalidade | Taxa recorrência | |
|---|---|---|---|
| Stripe | Não | — | Aceita BR oficialmente, elegibilidade de conta tem nuances |
| Asaas | Não | ~2,99% + R$0,49 | Favorito levantado, API simples |
| Mercado Pago | Não | ~3,19–4,99% | Maior confiança de marca no BR |

Descartados: Iugu (mensalidade fixa mesmo sem venda), Pagar.me (over-spec pro caso atual).

## Resumo — bloqueia MVP

🟢 Auth · Hospedagem · Segurança · Backup · Monitoramento · Domínio (pendente) · Pagamento (pendente)
🔵 E-mail transacional próprio · Push · Métricas/Prometheus/Grafana/Loki · CI/CD · Analytics de produto
