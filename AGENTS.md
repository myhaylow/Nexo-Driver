# Nexo Driver — instruções para agentes

## Objetivo atual

Construir e validar primeiro a leitura de ofertas da Uber Driver. A integração com a 99 só começa quando a leitura da Uber atingir a meta de confiança definida no plano do produto. Aceite e recusa automáticos permanecem desacoplados do aplicativo real nesta fase.

## Prioridades do produto

1. Confiança e latência da leitura.
2. Supermercados e bloqueios geográficos.
3. Valor por quilômetro.
4. Valor por hora.
5. Limites absolutos de coleta.
6. Direção desejada.
7. Fatores compensatórios.

Consulte `docs/product-plan.md` antes de alterar regras ou fluxos.

## Regras de engenharia

- Mantenha o motor de decisão puro, determinístico e independente do Android sempre que possível.
- Toda mudança de regra precisa de pelo menos um cenário em `fixtures/decision-scenarios.json` e um teste automatizado.
- Nunca grave capturas de tela, endereços reais, nomes, telefones ou identificadores de passageiro no repositório.
- Logs usados no desenvolvimento precisam ser anonimizados e conter somente os campos indispensáveis para reproduzir a leitura.
- Não introduza `MediaProjection` ou compartilhamento de tela no fluxo principal.
- Não acople gestos, aceite ou recusa ao pacote que interage com Uber/99 nesta fase. Automações só podem existir em simuladores ou testes explicitamente isolados.
- Não invente seletores ou formatos de card. Quando não houver fixture/evidência, registre a lacuna e peça um exemplo anonimizado.
- Interface em português do Brasil; código, nomes de tipos e commits podem permanecer em inglês.

## Comandos de validação

```bash
npm test
npm run validate:fixtures
npm run simulate
```

Para Android, depois de executar o setup do ambiente:

```bash
gradle -p android test assembleDebug --no-daemon
```

## Uso de agentes e subagentes

- O agente principal coordena escopo, integra mudanças e entrega a conclusão.
- Use `android-engineer` para uma implementação Android isolada.
- Use `decision-reviewer` para revisar fórmulas, regressões e cobertura; ele é somente leitura.
- Use `log-analyst` apenas com logs anonimizados; ele é somente leitura.
- Use o agente nativo `explorer` para localizar código e mapear dependências.
- Delegue somente tarefas independentes. Não permita que dois agentes editem o mesmo módulo ao mesmo tempo.
- Limite a delegação a uma camada; subagentes não devem criar outros subagentes.

## Definição de pronto

- Comportamento demonstrado por fixture ou teste.
- Testes Node aprovados e, quando houver alteração Android, build/test Android aprovado.
- Nenhum dado pessoal ou segredo adicionado.
- Documentação atualizada quando contrato, regra ou fluxo mudar.
- Resumo final informa o que mudou, como foi validado e o que ainda depende de aparelho físico.
