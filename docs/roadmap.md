# Roadmap de Funcionalidades

> App multi-hobby de tracking (tipo Strava/Letterboxd, mas não nichado). Objetivo da Fase 0: validar se o usuário cria o hábito de registrar, antes de qualquer feature social.

## Fase 0 — MVP (sem dependência de outros usuários)
- Perfil do hobbista (hobbies, nível, bio)
- Tracker de sessão (título, data, tempo, foto, notas)
- Atributos dinâmicos por hobby (ver `diretrizes-tecnicas.md` — Alternativa C) — o que evita o app parecer raso vs. app nichado
- Diário de reflexão → unificado no campo `notes`
- Biblioteca de equipamentos
- Backlog Kanban por hobby (fila de projetos/ideias)
- Streak de constância

## Fase 1 — identidade do app (funciona com poucos usuários)
- Painel de estatísticas de tempo por hobby
- Sessões colaborativas/em grupo (não depende de descoberta, usuário convida quem já conhece)
- Teste de personalidade/descoberta de hobby (onboarding)

## Fase 2 — efeito rede ⚠️ não lançar sem massa crítica de usuários/cidade
- Feed social (cronológico, amigos/mesma cidade)
- Hobby Buddy (matching por proximidade — mesma restrição do feed)
- Indicações/rankings locais
- Heatmap de cidade — exige volume mínimo de sessões geolocalizadas por região, senão fica vazio/enganoso

## Fase 3 — gamificação
- XP por hobby (curva de XP **diferente por categoria** — 1h de leitura ≠ 1h de corrida em esforço percebido)

## Fase 4 — premium
- Wrapped estilo Strava (infográfico mensal/anual)
- Comparação "eu do passado" (timelapse de fotos/progresso)
- Multi-perfil família/casal
- Metas com IA
- Alerta de manutenção de equipamento
- Lembrete de abandono (IA detecta queda de frequência)
- Exportação de dados
- Templates de hobby da comunidade com acesso antecipado pra assinante
- Customização de perfil (badges, temas)
- Destaque de assinante na listagem (⚠️ como badge visível, nunca como ranking artificialmente inflado)

## Regra de priorização
Feed, Hobby Buddy e Heatmap compartilham a mesma trava: não implementar/expor sem massa crítica local.
