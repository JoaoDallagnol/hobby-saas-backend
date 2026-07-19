# MVP Execution Checklist

> Quadro operacional do MVP (Fase 0). Este arquivo serve como backlog executável e checklist compartilhado.
>
> Como usar:
> - Marcar `- [x]` quando o item estiver realmente concluído.
> - Não marcar apenas porque existe schema/documentação; precisa existir implementação válida no escopo do item.
> - Se surgir trabalho novo, adicionar como subtarefa do bloco correto.
> - Itens de Fase 1+ ficam fora deste arquivo por padrão, mesmo que já existam no schema.
> - Nenhuma mudança de produto, roadmap, schema, contrato ou implementação pode terminar deixando `AGENTS.md` ou arquivos de `docs/` desatualizados entre si.
> - Se surgir inconsistência entre documentos, reportar antes de editar; depois da decisão, atualizar todos os arquivos impactados no mesmo trabalho.
> - Antes de fechar qualquer mudança, fazer análise de impacto: verificar dependências em funcionalidades relacionadas, banco, APIs, validações, roadmap, checklist e documentação.
> - Ordem de execução permitida: construir primeiro domínio, persistência, contratos e regras centrais da API; integrações externas reais podem ser fechadas depois, desde que o checklist técnico e a arquitetura final continuem respeitados.

## Definição de pronto do MVP

- [ ] Usuário consegue autenticar via Firebase Authentication real e ser provisionado no banco na primeira request autenticada.
- [ ] Usuário consegue manter perfil básico e hobbies praticados.
- [x] Usuário consegue criar, listar e consultar sessões com atributos fixos e dinâmicos por hobby.
- [x] Usuário consegue cadastrar e reutilizar equipamentos próprios nas sessões.
- [x] Usuário consegue manter backlog/Kanban por hobby e vincular sessão a item/projeto quando aplicável.
- [x] Sessão aceita fotos por fluxo de presigned URL direto para R2, sem binário via backend.
- [x] Sessão aceita `place_id`; backend resolve e persiste localização sem confiar em coordenadas do client.
- [x] Sistema calcula ou expõe streak de constância no escopo do MVP.
- [ ] Base mínima de segurança, backup, monitoramento e deploy local está operacional.
- [ ] Tudo essencial do MVP está coberto por testes suficientes para evoluir sem quebrar regra central do produto.

## 1. Base do projeto

- [x] Inicializar backend Spring Boot 4.1.x com Java 25 e Maven.
  - [x] Estruturar módulos/pacotes base do projeto.
  - [x] Configurar perfis de ambiente (`local`, `test`, `prod` ou equivalente).
  - [x] Configurar leitura de variáveis de ambiente para secrets e integrações.
  - [x] Adicionar dependências base: Web, Validation, Security, JPA, Flyway, Postgres, Actuator, testes.
- [ ] Definir contrato inicial da API.
  - [x] Escolher abordagem de OpenAPI/Swagger para documentação e navegação dos endpoints.
  - [x] Manter contrato alinhado com comportamento real da API conforme as features forem entrando.
  - [x] Não documentar endpoint inexistente nem deixar endpoint existente sem revisão do contrato quando houver mudança relevante.
- [x] Subir ambiente local via Docker Compose.
  - [x] Postgres disponível para a aplicação.
  - [x] App sobe apontando para os serviços locais.
  - [x] Estratégia de auth local definida sem depender de provedor self-hosted (ex: projeto Firebase de dev e/ou fluxo de token de teste controlado).
- [x] Configurar observabilidade mínima.
  - [x] Actuator habilitado com endpoints úteis.
  - [x] Integração com Sentry preparada ou documentada com placeholders de config.

## 2. Persistência e migrações

- [x] Criar baseline Flyway do schema MVP real.
  - [x] `users`
  - [x] `hobby_categories`
  - [x] `hobbies`
  - [x] `user_hobbies`
  - [x] `hobby_attribute_template`
  - [x] `sessions`
  - [x] `session_photos`
  - [x] `places`
  - [x] `equipment`
  - [x] `session_equipment`
  - [x] `backlog_items`
  - [x] `hobby_suggestions`
- [x] Garantir constraints e índices básicos.
  - [x] PKs e FKs alinhadas ao dicionário de dados.
  - [x] Unicidade/composição necessária em `user_hobbies` e tabelas de junção.
  - [x] Índices em chaves de busca frequente (`user_id`, `hobby_id`, `started_at`, `place_id`).
- [x] Fechar estratégia de mapeamento de `sessions.attributes` (JSONB).
  - [x] Validar se Hibernate resolve nativamente no stack atual.
  - [x] Introduzir lib auxiliar só se houver necessidade real.

## 3. Seed e catálogo base

- [x] Definir estratégia de seed inicial para hobbies e categorias.
  - [x] Importar apenas nomes aproveitáveis do seed externo citado na doc.
  - [x] Remover duplicatas antes de persistir.
  - [x] Não importar a categorização fraca original como verdade do domínio.
- [x] Popular `hobby_categories` com conjunto inicial coerente.
- [x] Popular `hobbies` com catálogo inicial suficiente para o MVP.
- [x] Popular `hobby_attribute_template` com exemplos reais para hobbies prioritários do MVP.
  - [x] Pelo menos um hobby de endurance/exercício.
  - [x] Pelo menos um hobby criativo/artístico.
  - [x] Pelo menos um hobby intelectual/estudo.

## 4. Autenticação e provisionamento de usuário

> Observação: este bloco não precisa bloquear a implementação inicial das regras de negócio do backend, desde que a integração real com Firebase seja concluída antes de considerar o MVP fechado end-to-end.

- [ ] Configurar autenticação backend com Firebase Authentication.
  - [ ] Validar ID token/JWT emitido pelo Firebase conforme ambiente real (`dev`/`prod`).
  - [x] Mapear identidade autenticada sem criar modelo próprio de senha.
- [ ] Implementar provisionamento just-in-time de `users`.
  - [x] Criar usuário na primeira request autenticada se não existir.
  - [x] Sincronizar `sub`, `email`, `name` e `email_verified` a partir do token.
  - [x] Nunca gerar `users.id` no banco; usar o `sub`/`uid` do token validado como string.
- [ ] Garantir autorização por recurso.
  - [x] Usuário só acessa/edita os próprios dados.
  - [x] Regras não dependem de payload do client para validar posse/permissão.

## 5. Perfil do hobbista

- [x] Implementar leitura e atualização do perfil base do usuário.
  - [x] `name` vindo do fluxo definido para o produto.
  - [x] `bio` persistida no banco do produto.
- [x] Implementar gestão de hobbies do usuário (`user_hobbies`).
  - [x] Adicionar hobby ao perfil.
  - [x] Remover hobby do perfil.
  - [x] Atualizar `experience_level`.
- [x] Definir contrato de API do perfil.
  - [x] Payloads de leitura.
  - [x] Payloads de escrita.
  - [x] Regras de validação.

## 6. Tracker de sessão

- [x] Implementar criação de sessão.
  - [x] Validar `title`, `startedAt`, `durationMinutes`, `notes`, `satisfaction`.
  - [x] Validar `satisfaction` no backend independentemente do front.
  - [x] Garantir que `hobbyId` exista e seja permitido para o usuário conforme regra definida.
  - [x] Aceitar `equipmentIds` válidos do usuário quando o fluxo estiver presente.
  - [x] Aceitar `projectId` válido do usuário quando o vínculo com backlog estiver presente.
- [x] Implementar listagem e detalhe de sessão.
  - [x] Ordenação por data.
  - [x] Filtros mínimos úteis para o MVP.
  - [x] Inclusão de atributos dinâmicos no retorno.
  - [x] Inclusão de equipamentos e vínculo com backlog, se aplicável no contrato.
- [x] Implementar atualização de sessão.
  - [x] Revalidar atributos fixos e dinâmicos.
  - [x] Reforçar posse do recurso.
- [x] Implementar exclusão de sessão.
  - [x] Definir comportamento para fotos e vínculos relacionados.
- [ ] Decidir se o MVP precisa de paginação desde o início.

## 6A. Biblioteca de equipamentos

- [x] Implementar CRUD de equipamentos do usuário.
  - [x] Criar equipamento com `category` e `name`.
  - [x] Listar equipamentos do usuário.
  - [x] Atualizar equipamento.
  - [x] Excluir equipamento com regra clara para vínculos históricos.
- [x] Garantir que `category` e `name` sejam tratados como colunas independentes.
- [x] Garantir que sessão nunca aceite equipamento como texto livre.
- [ ] Definir contrato de API para autocomplete/histórico, se entrar no MVP inicial.

## 6B. Backlog/Kanban por hobby

- [x] Implementar CRUD de `backlog_items`.
  - [x] Criar item de backlog.
  - [x] Listar itens por usuário e hobby.
  - [x] Atualizar título e status.
  - [x] Excluir item.
- [x] Definir estados válidos de `status` no MVP.
- [x] Permitir vínculo opcional de sessão com item/projeto do próprio usuário.
- [x] Garantir autorização por recurso também no backlog.

## 7. Atributos dinâmicos por hobby

- [x] Implementar leitura de templates por hobby.
- [x] Validar `sessions.attributes` contra `hobby_attribute_template`.
  - [x] Rejeitar chave inexistente para o hobby.
  - [x] Rejeitar tipo incompatível.
  - [ ] Respeitar campos obrigatórios se essa regra existir.
- [x] Garantir persistência em JSONB sem abrir exceção para coluna dedicada por hobby.
- [x] Cobrir com testes de validação positiva e negativa.

## 8. Fotos e storage

- [x] Implementar fluxo de presigned URL para upload direto no R2.
  - [x] Endpoint para solicitar upload.
  - [x] Geração de chave segura por usuário/sessão/escopo temporário.
  - [x] Restrições mínimas de content type e tamanho, se aplicável.
- [x] Persistir apenas storage keys/URLs necessárias no banco.
- [x] Modelar associação de fotos à sessão.
- [ ] Definir processamento assíncrono.
  - [ ] Thumbnail.
  - [ ] Compressão/WebP.
  - [ ] Remoção de EXIF.
- [ ] Fechar decisão técnica da lib/processo de imagem.

## 9. Localização

- [x] Implementar recebimento de `place_id` vindo do client.
- [x] Integrar com Google Place Details usando apenas Essentials FieldMask.
- [x] Implementar cache em `places`.
  - [x] Reutilizar lugar já resolvido.
  - [x] Persistir `place_id`, `name`, `lat`, `lng`.
- [x] Garantir que o backend ignore/rejeite coordenadas enviadas pelo client.
- [x] Definir representação de localização nas APIs de sessão.

## 10. Streak

- [x] Definir regra de negócio exata do streak do MVP.
  - [x] Diário corrido.
  - [ ] Por hobby.
  - [ ] Regra híbrida.
- [x] Implementar cálculo de streak.
  - [x] Regra para dias sem sessão.
  - [x] Regra para múltiplas sessões no mesmo dia.
  - [x] Timezone de referência.
- [x] Expor streak na API adequada.
- [x] Cobrir edge cases em testes.

## 11. Segurança e operação mínima

- [ ] Garantir que segredos fiquem fora do código.
- [ ] Definir estratégia de gestão de secrets por ambiente.
  - [x] `local`: usar `.env`, variáveis locais ou mecanismo equivalente fora do versionamento.
  - [x] `prod`: usar secrets/env vars do servidor sem valor sensível commitado.
  - [x] Manter exemplos seguros versionados sem credenciais reais.
  - [x] Separar credenciais/projetos Firebase de `dev` e `prod`.
- [ ] Garantir perfis/configuração por ambiente.
  - [x] `application.yaml` base sem secret real.
  - [x] `application-local.yaml` para desenvolvimento local.
  - [x] `application-prod.yaml` para comportamento de produção.
  - [x] Definir claramente quais variáveis são obrigatórias em cada ambiente.
- [ ] Preparar HTTPS/proxy e variáveis esperadas para produção.
- [x] Definir estratégia de rate limiting.
  - [x] Camada de edge/proxy.
  - [x] Camada de aplicação com limite básico em memória por usuário/IP.
- [ ] Planejar backup diário do Postgres para R2.
  - [x] Script/job documentado ou implementado.
  - [ ] Estratégia de retenção definida.
- [x] Documentar restauração mínima do backup.
- [ ] Mapear cadastros/configurações manuais necessários por plataforma externa.
  - [x] Firebase `dev` e `prod`.
  - [x] Hostinger/VPS.
  - [x] Cloudflare/R2.
  - [x] Google Places.
  - [ ] Stores mobile quando entrarem no escopo.
- [ ] Se e-mail transacional próprio entrar no MVP técnico, configurar Brevo com domínio e autenticação de envio.
- [ ] Criar baseline de revisão de segurança para mudanças novas.
  - [x] Verificar autenticação e autorização por recurso em cada endpoint novo.
  - [x] Verificar validação de input e tratamento seguro de erro.
  - [x] Verificar ausência de segredo hardcoded ou log sensível.
  - [x] Verificar exposição indevida de dados, IDOR/BOLA e confiança excessiva em payload do client.
  - [x] Verificar CORS, headers e comportamento de produção conforme necessidade real.

## 12. Testes

- [ ] Testes unitários para regras de domínio críticas.
- [x] Testes de integração com Postgres real via Testcontainers.
- [x] Testes de segurança/autorização.
  - [x] Usuário não acessa recurso de outro usuário.
  - [x] JWT inválido/ausente falha corretamente.
- [x] Testes de validação de atributos dinâmicos.
- [x] Testes do fluxo de provisionamento JIT.
- [x] Testes do fluxo de localização.
- [x] Testes do contrato das APIs principais.

## 13. Documentação viva

- [ ] Manter `AGENTS.md`, `docs/roadmap.md`, `docs/funcionalidades.md`, `docs/diretrizes-tecnicas.md`, `docs/modelagem-banco-dados.md`, `docs/infraestrutura-e-seguranca.md` e este checklist alinhados entre si.
- [ ] Nenhuma implementação, alinhamento de escopo ou mudança técnica relevante termina com documentação divergente.
- [ ] Ao mudar roadmap, schema, contrato, fluxo ou decisão técnica, atualizar os arquivos impactados na mesma entrega.
- [ ] Se houver inconsistência entre documentos, reportar antes de editar e consolidar a decisão antes da atualização.
- [ ] Antes de remover, promover, rebaixar ou alterar uma funcionalidade, revisar impactos em features dependentes, APIs, banco, validações e ordem de implementação.
- [ ] Atualizar `docs/modelagem-banco-dados.md` se a implementação exigir ajuste real de schema.
- [ ] Atualizar `docs/stack.md` quando uma dependência deixar de ser hipótese e virar decisão.
- [ ] Registrar decisões abertas que ainda não bloqueiam o MVP.
  - [ ] Nome do app/domínio.
  - [ ] Provedor de pagamento.
  - [ ] Enumeração de categorias de equipamento.
  - [ ] Estratégia final de processamento de imagem.
  - [ ] Estratégia final de mapeamento JSONB.

## Fora do escopo deste checklist

- [ ] Não puxar automaticamente Fase 1 para dentro do MVP.
- [ ] Não iniciar Fase 2 sem massa crítica real.
- [ ] Não adicionar pagamento antes da escolha do provedor.
- [ ] Não transformar decisões pendentes em regra fixa sem atualizar a documentação correspondente.
