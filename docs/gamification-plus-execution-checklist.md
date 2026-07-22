# Gamification and Plus Execution Checklist

> Quadro executável das funcionalidades de retenção e da fundação Plus. Não substitui `mvp-execution-checklist.md`: o MVP precisa ser validado end-to-end antes do rollout destas superfícies.

## Arquitetura e contrato

- [x] Definir separação Free/Plus sem pay-to-win.
- [x] Definir flags de disponibilidade separadas do entitlement comercial.
- [x] Definir metas, XP, níveis, badges, recordes e desafio mensal.
- [x] Definir estatísticas, retrospectiva, perfil Plus, backlog avançado e manutenção.
- [x] Manter cobrança, IA, mídia extra, família e social fora desta implementação.
- [x] Atualizar OpenAPI com todos os endpoints implementados.

## Persistência

- [x] Criar migration versionada para parâmetros de XP e tema de perfil.
- [x] Criar `hobby_xp` como projeção reconstruível.
- [x] Criar `goals` com métricas, cadências, período, estado e marcação avançada.
- [x] Criar `user_badges` e `user_featured_badges`.
- [x] Criar `subscriptions` genérica sem acoplar provedor ainda não decidido.
- [x] Adicionar campos Plus não destrutivos ao backlog.
- [x] Criar `equipment_maintenance_rules`.
- [x] Garantir constraints, índices e ownership necessários.

## Gamificação Free

- [x] CRUD de meta semanal, uma ativa por hobby e período corrente.
- [x] Progresso derivado das sessões após create/update/delete.
- [x] XP e nível por hobby com parâmetros da categoria.
- [x] Catálogo e concessão idempotente de badges oficiais.
- [x] Recordes pessoais básicos.
- [x] Desafio mensal oficial determinístico.
- [x] Melhor streak histórico sem alterar a regra do streak atual.
- [x] Exportação bruta segura dos próprios dados.
- [x] Cache limitado do dashboard por usuário, com fonte no Postgres e invalidação após commit de mutação de sessão.

## Fundação Plus

- [x] Resolver entitlement no banco; ausência equivale a Free.
- [x] Impedir auto-upgrade e ignorar qualquer plano enviado pelo client.
- [x] Múltiplas metas e cadências mensal/customizada.
- [x] Estatísticas avançadas, série diária e comparação entre períodos.
- [x] Retrospectiva mensal/anual em JSON estruturado.
- [x] Tema de perfil, até três badges em destaque e selo derivado.
- [x] Planejamento avançado do backlog sem apagar dados no downgrade.
- [x] Regras e alertas derivados de manutenção de equipamentos.
- [x] Desafio pessoal customizado reutilizando motor de metas.

## Segurança e rollout

- [x] `FEATURE_GAMIFICATION_ENABLED` com defaults e falha segura documentados.
- [x] `FEATURE_PLUS_ENABLED` com defaults e falha segura documentados.
- [x] Verificar IDOR/BOLA em meta, badge, equipamento, backlog e estatísticas.
- [x] Nunca expor provider id, storage keys, token, UID público ou dados de terceiros.
- [x] Rate limiting e validação de intervalo/paginação onde aplicável.
- [x] Cobrança futura somente por webhook assinado e idempotente.

## Testes e aceite

- [x] Testar regras Free/Plus e downgrade não destrutivo.
- [x] Testar recálculo derivado e histórico vazio.
- [x] Testar limites, períodos UTC e virada de semana/mês.
- [x] Testar ownership e respostas 401/403/404 seguras.
- [x] `mvn test`.
- [x] `mvn clean install`.
- [x] `docker compose up -d --build`.
- [x] Health `UP`.
- [x] `git diff --check` e `scripts/check-no-secrets.sh`.
- [x] Revisar alinhamento de todos os documentos afetados.

## Dependências posteriores — não marcar como implementação atual

- [ ] Escolher provedor de pagamento e desenhar checkout/webhook.
- [ ] Definir trial, preço, cancelamento, grace period e política de reembolso.
- [ ] Implementar IA somente após provedor, orçamento e política de privacidade.
- [ ] Implementar desafios sociais depois de seguidores/grupos e antifraude.
- [ ] Implementar mídia extra somente após rever contrato, storage e custo.
- [ ] Implementar PDF/PNG/timelapse de Wrapped no client ou pipeline próprio.
