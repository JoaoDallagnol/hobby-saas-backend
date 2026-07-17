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

## Definição de pronto do MVP

- [ ] Usuário consegue autenticar via Firebase Authentication e ser provisionado no banco na primeira request autenticada.
- [ ] Usuário consegue manter perfil básico e hobbies praticados.
- [ ] Usuário consegue criar, listar e consultar sessões com atributos fixos e dinâmicos por hobby.
- [ ] Usuário consegue cadastrar e reutilizar equipamentos próprios nas sessões.
- [ ] Usuário consegue manter backlog/Kanban por hobby e vincular sessão a item/projeto quando aplicável.
- [ ] Sessão aceita fotos por fluxo de presigned URL direto para R2, sem binário via backend.
- [ ] Sessão aceita `place_id`; backend resolve e persiste localização sem confiar em coordenadas do client.
- [ ] Sistema calcula ou expõe streak de constância no escopo do MVP.
- [ ] Base mínima de segurança, backup, monitoramento e deploy local está operacional.
- [ ] Tudo essencial do MVP está coberto por testes suficientes para evoluir sem quebrar regra central do produto.

## 1. Base do projeto

- [ ] Inicializar backend Spring Boot 4.1.x com Java 25 e Maven.
  - [ ] Estruturar módulos/pacotes base do projeto.
  - [ ] Configurar perfis de ambiente (`local`, `test`, `prod` ou equivalente).
  - [ ] Configurar leitura de variáveis de ambiente para secrets e integrações.
  - [ ] Adicionar dependências base: Web, Validation, Security Resource Server, JPA, Flyway, Postgres, Actuator, testes.
- [ ] Definir contrato inicial da API.
  - [ ] Escolher abordagem de OpenAPI/Swagger para documentação e navegação dos endpoints.
  - [ ] Manter contrato alinhado com comportamento real da API conforme as features forem entrando.
  - [ ] Não documentar endpoint inexistente nem deixar endpoint existente sem revisão do contrato quando houver mudança relevante.
- [ ] Subir ambiente local via Docker Compose.
  - [ ] Postgres disponível para a aplicação.
  - [ ] App sobe apontando para os serviços locais.
  - [ ] Estratégia de auth local definida sem depender de Keycloak (ex: projeto Firebase de dev e/ou fluxo de token de teste controlado).
- [ ] Configurar observabilidade mínima.
  - [ ] Actuator habilitado com endpoints úteis.
  - [ ] Integração com Sentry preparada ou documentada com placeholders de config.

## 2. Persistência e migrações

- [ ] Criar baseline Flyway do schema MVP real.
  - [ ] `users`
  - [ ] `hobby_categories`
  - [ ] `hobbies`
  - [ ] `user_hobbies`
  - [ ] `hobby_attribute_template`
  - [ ] `sessions`
  - [ ] `session_photos`
  - [ ] `places`
  - [ ] `equipment`
  - [ ] `session_equipment`
  - [ ] `backlog_items`
  - [ ] `hobby_suggestions`
- [ ] Garantir constraints e índices básicos.
  - [ ] PKs e FKs alinhadas ao dicionário de dados.
  - [ ] Unicidade/composição necessária em `user_hobbies` e tabelas de junção.
  - [ ] Índices em chaves de busca frequente (`user_id`, `hobby_id`, `started_at`, `place_id`).
- [ ] Fechar estratégia de mapeamento de `sessions.attributes` (JSONB).
  - [ ] Validar se Hibernate resolve nativamente no stack atual.
  - [ ] Introduzir lib auxiliar só se houver necessidade real.

## 3. Seed e catálogo base

- [ ] Definir estratégia de seed inicial para hobbies e categorias.
  - [ ] Importar apenas nomes aproveitáveis do seed externo citado na doc.
  - [ ] Remover duplicatas antes de persistir.
  - [ ] Não importar a categorização fraca original como verdade do domínio.
- [ ] Popular `hobby_categories` com conjunto inicial coerente.
- [ ] Popular `hobbies` com catálogo inicial suficiente para o MVP.
- [ ] Popular `hobby_attribute_template` com exemplos reais para hobbies prioritários do MVP.
  - [ ] Pelo menos um hobby de endurance/exercício.
  - [ ] Pelo menos um hobby criativo/artístico.
  - [ ] Pelo menos um hobby intelectual/estudo.

## 4. Autenticação e provisionamento de usuário

- [ ] Configurar autenticação backend com Firebase Authentication.
  - [ ] Validar ID token/JWT emitido pelo Firebase conforme ambiente.
  - [ ] Mapear identidade autenticada sem criar modelo próprio de senha.
- [ ] Implementar provisionamento just-in-time de `users`.
  - [ ] Criar usuário na primeira request autenticada se não existir.
  - [ ] Sincronizar `sub`, `email`, `name` e `email_verified` a partir do token.
  - [ ] Nunca gerar `users.id` no banco; usar o `sub`/`uid` do token validado.
- [ ] Garantir autorização por recurso.
  - [ ] Usuário só acessa/edita os próprios dados.
  - [ ] Regras não dependem de payload do client para validar posse/permissão.

## 5. Perfil do hobbista

- [ ] Implementar leitura e atualização do perfil base do usuário.
  - [ ] `name` vindo do fluxo definido para o produto.
  - [ ] `bio` persistida no banco do produto.
- [ ] Implementar gestão de hobbies do usuário (`user_hobbies`).
  - [ ] Adicionar hobby ao perfil.
  - [ ] Remover hobby do perfil.
  - [ ] Atualizar `experience_level`.
- [ ] Definir contrato de API do perfil.
  - [ ] Payloads de leitura.
  - [ ] Payloads de escrita.
  - [ ] Regras de validação.

## 6. Tracker de sessão

- [ ] Implementar criação de sessão.
  - [ ] Validar `title`, `startedAt`, `durationMinutes`, `notes`, `satisfaction`.
  - [ ] Validar `satisfaction` no backend independentemente do front.
  - [ ] Garantir que `hobbyId` exista e seja permitido para o usuário conforme regra definida.
  - [ ] Aceitar `equipmentIds` válidos do usuário quando o fluxo estiver presente.
  - [ ] Aceitar `projectId` válido do usuário quando o vínculo com backlog estiver presente.
- [ ] Implementar listagem e detalhe de sessão.
  - [ ] Ordenação por data.
  - [ ] Filtros mínimos úteis para o MVP.
  - [ ] Inclusão de atributos dinâmicos no retorno.
  - [ ] Inclusão de equipamentos e vínculo com backlog, se aplicável no contrato.
- [ ] Implementar atualização de sessão.
  - [ ] Revalidar atributos fixos e dinâmicos.
  - [ ] Reforçar posse do recurso.
- [ ] Implementar exclusão de sessão.
  - [ ] Definir comportamento para fotos e vínculos relacionados.
- [ ] Decidir se o MVP precisa de paginação desde o início.

## 6A. Biblioteca de equipamentos

- [ ] Implementar CRUD de equipamentos do usuário.
  - [ ] Criar equipamento com `category` e `name`.
  - [ ] Listar equipamentos do usuário.
  - [ ] Atualizar equipamento.
  - [ ] Excluir equipamento com regra clara para vínculos históricos.
- [ ] Garantir que `category` e `name` sejam tratados como colunas independentes.
- [ ] Garantir que sessão nunca aceite equipamento como texto livre.
- [ ] Definir contrato de API para autocomplete/histórico, se entrar no MVP inicial.

## 6B. Backlog/Kanban por hobby

- [ ] Implementar CRUD de `backlog_items`.
  - [ ] Criar item de backlog.
  - [ ] Listar itens por usuário e hobby.
  - [ ] Atualizar título e status.
  - [ ] Excluir item.
- [ ] Definir estados válidos de `status` no MVP.
- [ ] Permitir vínculo opcional de sessão com item/projeto do próprio usuário.
- [ ] Garantir autorização por recurso também no backlog.

## 7. Atributos dinâmicos por hobby

- [ ] Implementar leitura de templates por hobby.
- [ ] Validar `sessions.attributes` contra `hobby_attribute_template`.
  - [ ] Rejeitar chave inexistente para o hobby.
  - [ ] Rejeitar tipo incompatível.
  - [ ] Respeitar campos obrigatórios se essa regra existir.
- [ ] Garantir persistência em JSONB sem abrir exceção para coluna dedicada por hobby.
- [ ] Cobrir com testes de validação positiva e negativa.

## 8. Fotos e storage

- [ ] Implementar fluxo de presigned URL para upload direto no R2.
  - [ ] Endpoint para solicitar upload.
  - [ ] Geração de chave segura por usuário/sessão/escopo temporário.
  - [ ] Restrições mínimas de content type e tamanho, se aplicável.
- [ ] Persistir apenas storage keys/URLs necessárias no banco.
- [ ] Modelar associação de fotos à sessão.
- [ ] Definir processamento assíncrono.
  - [ ] Thumbnail.
  - [ ] Compressão/WebP.
  - [ ] Remoção de EXIF.
- [ ] Fechar decisão técnica da lib/processo de imagem.

## 9. Localização

- [ ] Implementar recebimento de `place_id` vindo do client.
- [ ] Integrar com Google Place Details usando apenas Essentials FieldMask.
- [ ] Implementar cache em `places`.
  - [ ] Reutilizar lugar já resolvido.
  - [ ] Persistir `place_id`, `name`, `lat`, `lng`.
- [ ] Garantir que o backend ignore/rejeite coordenadas enviadas pelo client.
- [ ] Definir representação de localização nas APIs de sessão.

## 10. Streak

- [ ] Definir regra de negócio exata do streak do MVP.
  - [ ] Diário corrido.
  - [ ] Por hobby.
  - [ ] Regra híbrida.
- [ ] Implementar cálculo de streak.
  - [ ] Regra para dias sem sessão.
  - [ ] Regra para múltiplas sessões no mesmo dia.
  - [ ] Timezone de referência.
- [ ] Expor streak na API adequada.
- [ ] Cobrir edge cases em testes.

## 11. Segurança e operação mínima

- [ ] Garantir que segredos fiquem fora do código.
- [ ] Definir estratégia de gestão de secrets por ambiente.
  - [ ] `local`: usar `.env`, variáveis locais ou mecanismo equivalente fora do versionamento.
  - [ ] `prod`: usar secrets/env vars do servidor sem valor sensível commitado.
  - [ ] Manter exemplos seguros versionados sem credenciais reais.
  - [ ] Separar credenciais/projetos Firebase de `dev` e `prod`.
- [ ] Garantir perfis/configuração por ambiente.
  - [ ] `application.yaml` base sem secret real.
  - [ ] `application-local.yaml` para desenvolvimento local.
  - [ ] `application-prod.yaml` para comportamento de produção.
  - [ ] Definir claramente quais variáveis são obrigatórias em cada ambiente.
- [ ] Preparar HTTPS/proxy e variáveis esperadas para produção.
- [ ] Definir estratégia de rate limiting.
  - [ ] Camada de edge/proxy.
  - [ ] Camada de aplicação com Bucket4j, se entrar já no MVP técnico.
- [ ] Planejar backup diário do Postgres para R2.
  - [ ] Script/job documentado ou implementado.
  - [ ] Estratégia de retenção definida.
- [ ] Documentar restauração mínima do backup.
- [ ] Mapear cadastros/configurações manuais necessários por plataforma externa.
  - [ ] Firebase `dev` e `prod`.
  - [ ] Hostinger/VPS.
  - [ ] Cloudflare/R2.
  - [ ] Google Places.
  - [ ] Stores mobile quando entrarem no escopo.
- [ ] Se e-mail transacional próprio entrar no MVP técnico, configurar Brevo com domínio e autenticação de envio.
- [ ] Criar baseline de revisão de segurança para mudanças novas.
  - [ ] Verificar autenticação e autorização por recurso em cada endpoint novo.
  - [ ] Verificar validação de input e tratamento seguro de erro.
  - [ ] Verificar ausência de segredo hardcoded ou log sensível.
  - [ ] Verificar exposição indevida de dados, IDOR/BOLA e confiança excessiva em payload do client.
  - [ ] Verificar CORS, headers e comportamento de produção conforme necessidade real.

## 12. Testes

- [ ] Testes unitários para regras de domínio críticas.
- [ ] Testes de integração com Postgres real via Testcontainers.
- [ ] Testes de segurança/autorização.
  - [ ] Usuário não acessa recurso de outro usuário.
  - [ ] JWT inválido/ausente falha corretamente.
- [ ] Testes de validação de atributos dinâmicos.
- [ ] Testes do fluxo de provisionamento JIT.
- [ ] Testes do fluxo de localização.
- [ ] Testes do contrato das APIs principais.

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
