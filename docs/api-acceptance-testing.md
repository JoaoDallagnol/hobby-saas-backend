# Testes de Aceite da API com Postman

> Tutorial operacional da collection importável e da execução automatizada local. A suíte valida a API como um client real, mas não substitui JUnit/Testcontainers nem valida provedores externos reais.

## Objetivo

A suíte executa fluxos HTTP encadeados contra um ambiente descartável e verifica contrato, autenticação, autorização por recurso, regras Free/Plus, persistência e falhas seguras. Ela usa dois usuários locais controlados:

| Usuário | Bearer local | Entitlement |
|---|---|---|
| `acceptance-free-user` | fixture local da suíte | Free, por ausência de assinatura |
| `acceptance-plus-user` | fixture local da suíte | Plus, inserido diretamente no banco descartável |

Esses bearers são identificadores fictícios exclusivos do profile `local`, não credenciais reais. O backend continua sem endpoint de auto-upgrade.

## Arquivos

| Arquivo | Responsabilidade |
|---|---|
| `postman/HobbySaaS.postman_collection.json` | Collection v2.1 importável no Postman e executável pela CLI |
| `postman/local.postman_environment.json` | Environment importável, somente com valores locais fictícios |
| `postman/generate-collection.mjs` | Fonte versionada da collection; editar esta fonte e regenerar o JSON |
| `postman/docker-compose.acceptance.yml` | API, PostgreSQL e S3Mock isolados do Compose normal |
| `postman/package.json` + `package-lock.json` | Postman CLI fixada e reproduzível |
| `scripts/run-api-acceptance.sh` | Reset, startup, JIT, fixture Plus, execução, relatórios e inspeção de logs |
| `skills/run-api-acceptance/SKILL.md` | Procedimento mínimo obrigatório para agentes executarem/diagnosticarem a suíte |

Não mover os JSON para a raiz. A pasta `postman/` evita poluição, mantém collection, environment e infraestrutura juntos e continua compatível com a importação do Postman.

## Pré-requisitos

- Docker com `docker compose`;
- Node.js 16 ou superior e npm;
- `curl`;
- portas `18080` livres para a API de aceite;
- acesso ao npm somente na primeira instalação/alteração do lockfile.

O Postman Desktop é opcional para automação e necessário apenas para exploração manual pela interface. A execução local pela Postman CLI usa arquivo do repositório, não exige login/API key e não envia o resultado à nuvem. A dependência `postman-cli` é somente de desenvolvimento, não entra no JAR nem na imagem da aplicação.

## Execução automática recomendada

Na raiz do projeto:

```bash
./scripts/run-api-acceptance.sh
```

O script:

1. remove somente o projeto Compose `hobby-saas-acceptance` e seus volumes descartáveis;
2. constrói e sobe API, PostgreSQL 17 e S3Mock nas configurações de aceite;
3. espera o health responder `UP`;
4. provisiona os dois usuários via JIT usando tokens locais distintos;
5. insere o entitlement Plus diretamente no banco isolado;
6. instala a Postman CLI pelo lockfile, se necessário;
7. executa a collection na ordem;
8. gera relatórios JSON e JUnit em `target/postman/`;
9. falha se encontrar `ERROR`/`FATAL` inesperado nos logs;
10. remove containers e volumes de aceite após sucesso.

Se o daemon Docker local recusar o primeiro `stop` com `permission denied`, o runner encerra o PID 1 somente dos três containers do projeto `hobby-saas-acceptance` e repete o `down`. Esse fallback existe porque a falha já ocorreu neste ambiente; ele não alcança o Compose normal nem produção.

Opções:

```bash
./scripts/run-api-acceptance.sh --keep
./scripts/run-api-acceptance.sh --prepare-only
```

- `--keep`: executa tudo e mantém o ambiente em `http://127.0.0.1:18080` para investigação.
- `--prepare-only`: recria o ambiente, provisiona Free/Plus e para antes da CLI; usar antes do teste manual no Postman.
- em uma falha, o ambiente permanece disponível e o script imprime os logs recentes.

Para encerrar manualmente:

```bash
docker compose -f postman/docker-compose.acceptance.yml down -v --remove-orphans
```

Esse comando apaga apenas dados da suíte de aceite. Nunca substituir o arquivo Compose por `docker-compose.yml`, nem apontar fixtures/reset para banco local normal ou produção.

## Importar e executar no Postman

1. Execute `./scripts/run-api-acceptance.sh --prepare-only`.
2. No Postman, selecione **Import**.
3. Importe `postman/HobbySaaS.postman_collection.json`.
4. Importe `postman/local.postman_environment.json`.
5. Selecione o environment **Hobby SaaS Local Acceptance**.
6. Abra a collection **Hobby SaaS API Acceptance**.
7. Use **Run collection** e preserve a ordem definida.
8. Confira se todas as requests e assertions ficaram verdes.
9. Em falha, compare response/status com o log da aplicação antes de alterar qualquer regra.
10. Encerre o Compose de aceite com o comando da seção anterior.

A collection cria variáveis de execução (`hobbyId`, ids de equipamento/backlog/sessão/meta/badge/manutenção, usernames e datas) a partir das próprias respostas. Executar requests isoladas fora de ordem pode falhar por variável ausente; nesse caso execute primeiro a pasta de preparação correspondente ou a collection completa.

## Cobertura local atual

| Área | Cenários principais |
|---|---|
| Infra/contrato | health, OpenAPI e presença dos contratos centrais |
| Segurança | bearer ausente/inválido, identidade derivada do servidor e recursos de outro usuário retornando `404` |
| Usuários | JIT, perfil, username público, DTO público sem UID/e-mail |
| Hobbies | catálogo, vínculo, experiência e templates de atributos |
| Feature flags | estados esperados no ambiente de aceite |
| Equipamentos | create/list/update, filtro e ownership |
| Backlog | Free básico, bloqueio avançado, Plus avançado e ownership |
| Sessões | create/list/get/update, equipamento/projeto, JSONB e paginação |
| Visibilidade | `only_me`, `everyone`, perfil público, lista/detalhe público e rejeição de `followers` |
| Integrações desligadas | localização (`placeId` + `label`) e upload respondem `503` com falha segura |
| Gamificação | streak, meta Free, limite Free, meta Plus, XP, badge e recordes |
| Plano/analytics | entitlement vindo do banco, `403` Free, insights e Wrapped Plus |
| Plus | tema, badge conquistado em destaque, manutenção e campos avançados |
| Portabilidade | exportação JSON escopada e CSV não vazio |
| Cleanup lógico | arquivamento de metas e exclusão da regra de manutenção |

O ambiente é recriado a cada execução, portanto o conjunto permanece repetível e não depende do estado de uma execução anterior.

## O que a suíte local não comprova

- token, expiração, revogação e providers do Firebase real;
- upload/GET/purge/CDN e permissões reais do Cloudflare R2;
- Place Details, quota, billing e FieldMask do Google Places;
- TLS, DNS, Nginx, backup, restore, Sentry ou recursos da Hostinger;
- checkout, webhook e cobrança, ainda não implementados;
- carga, stress, pentest profissional ou comportamento do aplicativo mobile.

Esses cenários exigem credenciais e ambientes próprios. Nunca colocar valores reais na collection/environment versionados. Quando forem habilitados, criar execução separada por ambiente e manter a suíte local como gate rápido e sem custo.

## Diagnóstico de falhas

Ordem de análise:

1. identificar request e assertion no terminal ou `target/postman/postman-results.json`;
2. conferir status e corpo sem copiar bearer/headers sensíveis para relatório;
3. executar `docker compose -f postman/docker-compose.acceptance.yml ps`;
4. ler `docker compose -f postman/docker-compose.acceptance.yml logs --tail=200 app postgres s3mock`;
5. se necessário, consultar o PostgreSQL somente para confirmar fixture/ownership;
6. classificar como falha da aplicação, contrato, collection, fixture ou infraestrutura;
7. corrigir a camada correta e executar novamente a suíte completa.

Uma resposta diferente não significa automaticamente bug no backend: o contrato pode ter mudado legitimamente e a collection ter ficado desatualizada. Antes de consolidar qualquer alteração, aplicar a análise de impacto exigida pelo `AGENTS.md`.

## Manutenção após mudar a API

1. conferir o roadmap e a documentação funcional/técnica roteada pelo `AGENTS.md`;
2. atualizar controller/DTO, OpenAPI e testes Java;
3. alterar `postman/generate-collection.mjs`;
4. executar `npm --prefix postman run generate`;
5. validar JSON com `jq empty postman/*.json`;
6. executar `./scripts/run-api-acceptance.sh`;
7. executar `mvn test`, `mvn clean install`, `git diff --check` e `./scripts/check-no-secrets.sh`;
8. atualizar este documento, checklists e relatório se cobertura/status mudarem.

Não editar somente o JSON gerado: a próxima geração apagaria a mudança. O `package-lock.json` só deve mudar junto de uma atualização deliberada da Postman CLI, seguida por `npm audit` e nova execução completa.

## Uso futuro em CI

O mesmo script pode virar job do GitHub Actions porque já cria ambiente descartável e produz JUnit. Antes disso:

- confirmar disponibilidade de Docker no runner;
- manter cache de npm sem versionar `node_modules`;
- publicar `target/postman/postman-results.xml` como resultado de testes;
- publicar JSON/logs somente em falha e sem secrets;
- não executar integrações reais automaticamente até existirem secrets, quotas e ambiente dev próprios.
