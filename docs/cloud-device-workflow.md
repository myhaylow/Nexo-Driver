# Fluxo entre nuvem e aparelho fisico

## Durante o dia: nuvem

O aparelho nao e uma dependencia para o desenvolvimento diario. O Codex pode trabalhar em:

1. motor de decisao;
2. perfis e agendas;
3. casos de teste e simulacao;
4. normalizacao da leitura;
5. modelos de telemetria;
6. interface e pre-visualizacao do overlay;
7. regressao de bugs ja observados.

Cada comportamento relevante deve virar um cenario JSON reproduzivel em `fixtures/`.

## A noite: aparelho conectado

Uma sessao fisica deve ser curta e objetiva:

1. atualizar o repositorio no Codex Desktop;
2. compilar e instalar a variante pessoal ou Lab;
3. executar a checklist indicada para aquela versao;
4. exportar somente os logs tecnicos selecionados;
5. remover ou anonimizar enderecos antes de compartilhar;
6. registrar o resultado da sessao.

## Estagios de validacao

### Lab

- Automacoes liberadas apenas no aplicativo simulador.
- Uber e 99 fora da lista de pacotes permitidos para gestos.
- Casos deterministas e repetiveis.

### Pessoal em modo sombra

- Leitura e overlay reais.
- Sem gestos na Uber.
- Registro da recomendacao e da decisao manual observada.
- Liberado apenas depois do gate de 95% em laboratorio.

### Automacao real

- Fora do escopo inicial.
- Exige decisao posterior, validacao superior e revisao de riscos.

## Gate para iniciar a Uber real em modo sombra

- Pelo menos 95% dos cards de teste reconhecidos integralmente.
- Pelo menos 95% dos overlays posicionados sem cobrir controles essenciais.
- Campos criticos validados individualmente.
- Leitura incompleta nunca produz ACEITAR ou RECUSAR.
- Latencia observada normalmente inferior a um segundo.
- Zero gestos enviados ao pacote real da Uber.

## Privacidade dos logs

Logs compartilhados com a nuvem devem preferir identificadores de cenario e valores normalizados. Enderecos completos, nomes, telefones, placas, fotos e outras informacoes pessoais nao devem ser incluidos sem necessidade.
