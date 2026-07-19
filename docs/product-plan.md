# Plano vivo do Nexo Driver

## Objetivo

Analisar ofertas da Uber Driver com baixa latencia, calcular a viabilidade e mostrar um overlay compacto com ACEITAR, ANALISAR ou RECUSAR. A 99 entra apenas apos a validacao integral do leitor da Uber.

## Regras confirmadas

Prioridade do motor:

1. confianca da leitura;
2. supermercados e bloqueios geograficos;
3. valor por quilometro;
4. valor por hora;
5. limite de coleta;
6. direcao desejada;
7. fatores compensatorios.

O perfil inicial usa R$ 1,80/km, R$ 40/h, 3,5 km e 8 minutos para coleta. Perfis podem possuir agendas definidas pelo motorista.

## Overlay

- Borda forte verde, amarela ou vermelha.
- Modo Destino em azul ou roxo, preservando a cor da decisao no cabecalho.
- De duas a seis metricas configuraveis; quatro por padrao.
- Distancia total e tempo total sempre visiveis.
- Layout horizontal ou vertical.
- Posicao, escala, largura, altura e tamanho dos textos ajustaveis.
- Temas claro, escuro e Nexo.
- Icone flutuante abre corrida atual e historico rapido.

## Leitura

- Sem MediaProjection e sem compartilhamento continuo da tela.
- AccessibilityService orientado a eventos e limitado a Uber Driver.
- NotificationListenerService apenas como apoio.
- OCR fora do nucleo inicial.
- Leitura somente no MVP; executor permanece desacoplado.

## Geografia

- Municipios predefinidos por caixas de selecao.
- Areas personalizadas desenhadas no mapa.
- Supermercados e hipermercados em raio inicial de 180 metros.
- Modo Destino com raio inicial de 4 km e avaliacao da reducao de tempo restante.

## Desenvolvimento

- Nuvem: motor, simulador, fixtures, UI e testes deterministas.
- Aparelho: build Android, acessibilidade, overlay real, latencia e validacao de layouts.
- Gestos apenas no simulador durante a fase Lab.
- Uber real somente em modo sombra depois do gate de 95%.
