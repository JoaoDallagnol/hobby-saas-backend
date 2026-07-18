# Descrição das Funcionalidades — App de Gerenciamento de Hobbies

Um app para registrar, organizar e acompanhar a evolução de todos os seus hobbies em um só lugar — não nichado num hobby específico (como Strava é pra corrida ou Goodreads é pra leitura), mas generalista, cobrindo qualquer hobby que o usuário pratique.

---

## Fase 0 — MVP

O objetivo dessa fase é validar se as pessoas criam o hábito de registrar seus hobbies regularmente, antes de investir em qualquer funcionalidade que dependa de outros usuários.

O **Perfil do Hobbista** é o espaço onde o usuário lista os hobbies que pratica atualmente, define um nível de experiência em cada um e escreve uma breve biografia sobre si.

O **Registro de Sessões**, ou tracker, é a funcionalidade central do app: um botão simples para iniciar um cronômetro durante a prática de um hobby, ou registrar manualmente uma atividade que já aconteceu. Cada sessão tem título, data, tempo gasto, foto e um campo de notas em texto livre — esse campo de notas também absorve o que originalmente seria um "diário de reflexão" separado, permitindo que o usuário escreva o que aprendeu ou o que pretende tentar diferente da próxima vez, sem ter dois campos parecidos competindo pela atenção.

Os **atributos dinâmicos por hobby** são o que evita o app parecer raso demais comparado a um app especializado: em vez de todo hobby usar os mesmos campos genéricos, cada tipo de hobby pode ter campos extras próprios — corrida ganha um campo de distância, leitura ganha um campo de páginas lidas, marcenaria ganha um campo de material usado, e assim por diante. Isso é o que dá ao app profundidade equivalente à de um app nichado, sem abrir mão de cobrir qualquer hobby.

O **streak de constância** é um contador de dias seguidos em que o usuário registrou qualquer atividade, independente de qual hobby — é uma mecânica de hábito pura, que não depende de nenhum outro usuário do app, e serve como o principal gatilho de retorno diário. No MVP, o streak é **global** (não por hobby), conta **dias únicos** de atividade e usa **UTC como referência**, porque o produto ainda não tem timezone do usuário no modelo.

A **biblioteca de equipamentos** permite ao usuário cadastrar os itens que usa em cada hobby — uma câmera, um par de tênis de corrida, um kit de pincéis — e associar esses itens às sessões em que foram usados. Isso não depende de nenhum outro usuário do app e por isso entra já no MVP, junto com o restante.

O **backlog Kanban por hobby** é uma fila de projetos ou ideias futuras dentro de cada hobby — no hobby de leitura, são os livros já comprados mas ainda não lidos; na marcenaria, são os móveis planejados mas ainda não construídos. Uma sessão pode opcionalmente ser vinculada a um item desse backlog. Assim como a biblioteca de equipamentos, não depende de outros usuários e por isso também está no MVP.

---

## Fase 1 — Identidade do App

Nessa fase, o app começa a se diferenciar de um simples bloco de notas com foto, mas ainda sem depender de uma base grande de usuários.

O **painel de estatísticas de lazer** mostra, em gráficos, em quais hobbies o usuário investiu mais tempo na semana ou no mês, junto com o histórico de evolução ao longo do tempo.

As **sessões colaborativas ou em grupo** permitem marcar que uma sessão foi feita junto com outra pessoa do app — uma dupla de leitura, um grupo de corrida, uma partida de jogo de tabuleiro. Diferente de funcionalidades que dependem de descoberta por proximidade, aqui o próprio usuário convida quem já conhece, então funciona bem mesmo com poucos usuários na base.

O **teste de personalidade e descoberta** é um questionário interativo que, a partir do tempo livre, orçamento e traços de personalidade do usuário, sugere três novos hobbies que poderiam ser interessantes para ele — funciona bem como gancho de aquisição e engajamento no onboarding.

---

## Fase 2 — Efeito Rede

Essas funcionalidades compartilham uma restrição importante: todas dependem de uma massa crítica de usuários por hobby ou por cidade para funcionar de verdade. Lançar qualquer uma delas cedo demais faz o app parecer vazio ou abandonado, então a recomendação é considerar lançar restrito a uma cidade ou a um conjunto pequeno de hobbies antes de abrir tudo.

O **feed de exploração social** é um feed em ordem cronológica onde o usuário vê e curte fotos e registros de amigos ou de outras pessoas da mesma cidade.

A **busca por "hobby buddy" próximo** faz o matching de pessoas perto do usuário que praticam o mesmo hobby, para quem quer começar algo novo mas não tem com quem — compartilha exatamente a mesma restrição de densidade de usuários que o feed.

As **indicações e rankings locais** são uma seção do perfil onde o usuário lista onde pratica cada hobby e o que indica na cidade — por exemplo, a melhor cafeteria para ler ou a melhor praça para andar de skate.

O **mapa de calor da cidade**, ou heatmap, é um mapa interativo mostrando as zonas de maior atividade de cada tipo de hobby na cidade — parques com mais corredores, centros culturais com mais fotógrafos. Práticas feitas em casa nunca expõem o endereço exato, alimentando só a estatística geral do bairro de forma anônima. Essa funcionalidade em especial exige um volume real de sessões geolocalizadas antes de ser ativada, porque um heatmap com poucos dados fica enganoso e vazio, expondo a falta de tração do app em vez de ajudar.

---

## Fase 3 — Gamificação

A **árvore de ofício** transforma as horas registradas em experiência (XP), fazendo o usuário subir de nível dentro de cada hobby — por exemplo, de "Aprendiz de Jardinagem" a "Botânico Urbano". A curva de XP precisa variar por categoria de hobby, já que uma hora de leitura não representa o mesmo esforço percebido que uma hora de corrida; tratar todos os hobbies com a mesma fórmula minaria a credibilidade do sistema de gamificação.

---

## Fase 4 — Premium (Hobbyhood Pro)

O usuário comum segue usando o app de graça para registrar e ver a comunidade. Quem assina o plano pago paga pelo excesso de dados, pela customização e pelo orgulho de compartilhar.

Os **infográficos estéticos estilo "Wrapped"** geram automaticamente imagens e estatísticas visualmente bonitas do mês ou do ano do hobbista, prontas para compartilhar nos stories do Instagram.

A **comparação com o "eu do passado"** vai além do Wrapped simples, mostrando lado a lado fotos e progresso ao longo do tempo — como um timelapse de evolução em desenho, cerâmica ou transformação física.

O **multi-perfil de família ou casal** cobre hobbies compartilhados em casa, como um casal que joga board game junto ou um pai e filho que constroem modelismo juntos, com estatísticas combinadas entre os dois perfis.

As **metas avançadas e desafios com IA** permitem ao usuário pedir ajuda a uma IA para organizar rotinas de prática, como montar um cronograma para aprender anatomia praticando 15 minutos por dia.

O **inventário com alerta de manutenção ou validade** avisa o usuário quando é hora de trocar as cordas da guitarra, limpar a lente da câmera ou comprar mais insumos, com base no tempo de uso já registrado.

Os **lembretes inteligentes de abandono** usam IA para perceber quando o usuário parou de registrar um hobby que praticava com frequência, e mandam um aviso personalizado incentivando o retorno.

A **exportação de dados** permite ao usuário baixar seu histórico completo em CSV ou PDF, ou integrar com planilhas.

Os **templates de hobby customizados com acesso prioritário** permitem que a própria comunidade crie campos específicos para hobbies que ainda não têm um template pronto, com assinantes ganhando acesso antecipado aos templates mais votados antes de virarem públicos para todos.

A **customização de perfil** inclui medalhas exclusivas de conquistas, temas especiais para o app e destaque estético no feed.

Por fim, a **preferência na listagem para assinantes** deve ser tratada com cautela — a recomendação é implementar como um selo visível de assinante em vez de um ranking artificialmente inflado, para não corroer a confiança dos usuários no feed.
