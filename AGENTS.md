# AGENTS.md

## Stack
Java 25, Spring Boot 4.1.x, Maven, Postgres, Flyway, Keycloak (auth), Cloudflare R2 (fotos), Docker Compose numa VPS única. Detalhe completo: `docs/stack.md`.

## Docs de referência (ler sob demanda, não de uma vez)
- `docs/diretrizes-tecnicas.md` — modelo de sessão, atributos dinâmicos, equipamento, localização, fotos, taxonomia de hobbies.
- `docs/modelagem-banco-dados.md` — schema completo (diagramas + dicionário de dados).
- `docs/infraestrutura-e-seguranca.md` — auth, hospedagem, segurança, serviços de suporte.
- `docs/stack.md` — dependências e libs.

## Regras não óbvias (ler sempre)
- `users.id` = `sub` do JWT do Keycloak. Nunca gerar id próprio, nunca criar coluna de senha em `users`.
- Atributo dinâmico por hobby sempre vai em `sessions.attributes` (JSONB), validado contra `hobby_attribute_template`. Nunca criar coluna nova pra atributo específico de hobby.
- `equipment.category` e `equipment.name` são colunas independentes do mesmo registro — não é chave/valor.
- Nunca confiar em campo de plano/permissão vindo do client (ex: `isPremium`) — checar sempre contra o banco.
- Status de pagamento só muda via webhook assinado do provedor, nunca por request do client.
- Localização: client manda só `place_id`; backend resolve lat/lng via Google Place Details (FieldMask Essentials) e persiste — nunca confiar em lat/lng vindo do client.
- Fotos: upload via presigned URL direto pro R2, nunca binário passando pelo backend.

## Comandos
- Build: `mvn clean install`
- Testes: `mvn test`
- Local: `docker compose up -d` (sobe Spring Boot + Keycloak + Postgres)

## Não fazer
- Não usar MongoDB ou qualquer banco além do Postgres — domínio é relacional, flexibilidade já é resolvida via JSONB.
- Não implementar load balancer/API Gateway — fora de escopo até o monolito numa VPS não bastar mais.
- Não adicionar SDK/dependência de pagamento sem confirmar o provedor primeiro (ainda não decidido: Stripe/Asaas/Mercado Pago).
- Não seguir padrões da linha Spring Boot 3.x (EOL) — baseline é Jakarta EE 11 + Jackson 3.

## Pendências (não travam trabalho, mas não estão fechadas)
- Nome do app / domínio.
- Provedor de pagamento.
- Enumeração de categorias de equipamento.
