# Variantes observadas dos cards da Uber

Esta especificacao foi derivada de dez capturas fornecidas para desenvolvimento. As imagens e os enderecos reais nao sao armazenados no repositorio. Os valores numericos foram preservados apenas para criar casos de teste reproduziveis.

## Estados identificados

| Estado | Sinal principal | Acao do leitor |
| --- | --- | --- |
| Radar | Botao `Selecionar` | Ler e avaliar como oferta |
| Exclusiva direta | Selo `Exclusivo` e botao `Aceitar` | Ler e avaliar como oferta |
| Viagem encontrada | Titulo de confirmacao e botao `Iniciar navegacao` | Nao reavaliar; encerrar a oferta pendente |

O texto do botao e o estado do card sao parte do contrato. Um card de viagem encontrada nao pode ser confundido com uma nova oferta.

## Campos obrigatorios para o MVP

- valor bruto;
- categoria;
- tempo e distancia de coleta;
- tempo e distancia da viagem;
- estado do card;
- confianca da leitura.

Campos opcionais observados:

- valor por quilometro estimado;
- nota e quantidade de avaliacoes;
- passageiro verificado;
- embarque prioritario e bonus;
- selo exclusivo;
- indicacao de direcao ao destino.

## Regras de normalizacao confirmadas

Quando o card mostra `R$/km est.`, o valor corresponde a:

```text
valor bruto / (distancia de coleta + distancia da viagem)
```

A igualdade, com arredondamento de duas casas, foi confirmada em todos os exemplos que exibiam esse campo. Nas ofertas exclusivas o campo pode desaparecer, portanto o leitor sempre deve calcula-lo usando as distancias.

O valor monetario usa virgula decimal, enquanto as distancias observadas usam ponto decimal. O parser nao pode aplicar uma unica regra de separador a todos os campos.

## Variacao de altura

O card cresce quando aparecem bonus, verificacao, indicacao de destino ou enderecos quebrados em varias linhas. A leitura nao deve depender de coordenadas fixas, altura fixa ou ordem absoluta de todos os nos. Deve usar rotulos, grupos semanticos e validacao cruzada dos numeros.

## Isolamento de janelas

Algumas capturas contêm icones ou overlays de outros aplicativos. O leitor deve aceitar dados somente da janela e do `packageName` da Uber Driver. Textos de janelas flutuantes externas nunca entram no calculo ou na telemetria.

## Privacidade

Os enderecos podem existir transitoriamente no aparelho para geocodificacao e regras locais, mas nao devem aparecer em fixtures, logs de desenvolvimento ou commits. A telemetria de leitura usa apenas tipos de campo, presenca, confianca, latencia e codigos de erro.
