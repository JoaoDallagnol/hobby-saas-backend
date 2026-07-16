# Infraestrutura e Segurança

> 🟢 MVP (bloqueia lançamento) · 🔵 Fase seguinte (implementa quando a dor aparecer) · ⚠️ pendente de decisão

## Autenticação 🟢

- **Keycloak self-hosted** (não Auth0/Cognito/Supabase Auth — Keycloak é Java-nativo, integra direto no Spring Security via OIDC).
- Keycloak guarda credencial (senha com hash). App **nunca** vê/armazena senha.
- `users.id` = `sub` do JWT do Keycloak (não gerar id próprio, não ter coluna de mapeamento separada).
- Provisionamento **just-in-time**: primeira request autenticada de um user novo cria a linha em `users` na hora, a partir do JWT.
- MFA obrigatório na conta admin do Keycloak.

## Hospedagem 🟢

- **VPS única** (Hostinger, linha KVM, datacenter São Paulo) rodando Spring Boot + Keycloak + Postgres via **Docker Compose**. Não usar PaaS por serviço (Railway/Render) — sai mais caro rodando múltiplos serviços separados.
- Descartados: Oracle Cloud Free Tier (instabilidade/limite reduzido), Hetzner (EUR + datacenter EU).
- App deve ser **stateless** (sem estado em memória local, config via env var) — pré-requisito barato pra escalar depois sem reescrever.
- 🔵 Load balancer/API Gateway: não implementar agora. Evolução: Postgres gerenciado → Nginx como LB simples → API Gateway só se virar multi-serviço de verdade.

## Segurança 🟢

- DDoS: Cloudflare (free) como proxy DNS.
- Rate limit: Cloudflare (free) + Nginx (`limit_req`) + Bucket4j (por usuário/endpoint no código).
- **Nunca confiar em campo de plano/permissão vindo do cliente** (ex: `isPremium`) — checar sempre contra o banco.
- Pagamento: status de assinatura só muda via **webhook assinado** do provedor, nunca por chamada do client dizendo "paguei".
- Autorização por recurso em todo endpoint (`session.user_id == usuário autenticado`, não só "está logado").
- SQL Injection: mitigado por padrão via Spring Data JPA (queries parametrizadas); risco só em query nativa concatenada.
- HTTPS obrigatório (Let's Encrypt). Segredos só em env var, nunca no código.
- Mobile: token em Keychain/Keystore nativo; API key do Google Places restrita por package/bundle ID + assinatura; nenhum segredo real embutido no app.
- 🔵 Certificate pinning e detecção de root/jailbreak: não agora.

## E-mail transacional 🟢 (bloqueia — Keycloak depende disso)

- **Brevo**: 300 e-mails/dia, free permanente, sem cartão.
- Configurado direto no realm do Keycloak (SMTP), sem código.
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

## Monitoramento 🟢 (esforço baixo, ativar desde já)

- **Sentry** plano Developer: grátis, 5k erros/mês, 1 usuário, inclui 1 uptime monitor.
- 🔵 Métricas de negócio/tráfego: Spring Actuator + Micrometer (nativo) → visualização via Prometheus + Grafana só quando sentir falta. Log estruturado via Grafana Loki, só se `docker compose logs` via SSH incomodar. Atenção: 6 containers na mesma VPS pode exigir upgrade de plano (KVM1 → KVM2).

## CI/CD 🔵 (não bloqueia 1º deploy manual, mas fácil/grátis, ativar cedo)

- **GitHub Actions**: grátis ilimitado em repo público; 2.000 min/mês grátis em privado.
- Pipeline simples: push → testes → SSH na VPS → `git pull && docker compose up -d --build`. Sem registry de imagem externo.
- Hostinger não substitui isso (deploy 1-clique da Hostinger é pra apps prontas de terceiro, não pra atualizar código próprio continuamente).

## Domínio/DNS ⚠️ pendente (nome do app não decidido)

Passo a passo: registrar domínio → apontar nameservers pro Cloudflare → criar registros (VPS, `api.dominio`, `auth.dominio`, SPF/DKIM/DMARC do Brevo).

| Registrador | Preço/ano | WHOIS privado | |
|---|---|---|---|
| Registro.br (.com.br) | R$40–96 | Não | Exige CPF/CNPJ, mais confiança BR |
| Cloudflare Registrar (.com/.app) | ~US$9–10 (custo) | Sim, grátis | Mesmo painel do Cloudflare |
| Revendedoras BR | R$25–96 | Varia | Pix/boleto, suporte PT-BR |

## Analytics de produto 🔵 (não bloqueia MVP com poucos usuários conhecidos)

- **PostHog** — free tier generoso, self-hosted opcional (mesma VPS). Cobre funil/retenção/coorte — diferente de Sentry (erro) e Prometheus (infra).

## Pagamento ⚠️ pendente

| Provedor | Mensalidade | Taxa recorrência | |
|---|---|---|---|
| Stripe | Não | — | Aceita BR oficialmente, elegibilidade de conta tem nuances |
| Asaas | Não | ~2,99% + R$0,49 | Favorito levantado, API simples |
| Mercado Pago | Não | ~3,19–4,99% | Maior confiança de marca no BR |

Descartados: Iugu (mensalidade fixa mesmo sem venda), Pagar.me (over-spec pro caso atual).

## Resumo — bloqueia MVP

🟢 Auth · Hospedagem · Segurança · E-mail · Backup · Monitoramento · Domínio (pendente) · Pagamento (pendente)
🔵 Push · Métricas/Prometheus/Grafana/Loki · CI/CD · Analytics de produto
