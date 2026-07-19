# Aplicativo Android

Esqueleto pessoal do Nexo Driver, configurado para Android 16 (`compileSdk` e `targetSdk` 36), AGP 9.2.1 e JDK 17.

O repositorio ainda nao inclui os binarios do Gradle Wrapper. Na maquina com Android Studio/Gradle 9.4.1, gere o wrapper antes do primeiro build:

```bash
cd android
gradle wrapper --gradle-version 9.4.1
./gradlew assembleDebug
```

## Leitor de ofertas Uber

O aplicativo declara um `AccessibilityService` restrito ao pacote
`com.ubercab.driver`. O leitor percorre texto e descricoes sem usar geometria,
reconhece radar, exclusiva direta e resultado de pareamento, e somente encaminha
os dois primeiros estados para a futura ponte do motor de decisao. Nao ha OCR,
captura de tela, gestos nem acoes de aceitar ou recusar.

Os testes JVM constroem arvores de acessibilidade anonimizadas para os dez cards
versionados. A ativacao do servico, a estrutura produzida por versoes reais da
Uber Driver, a latencia e mudancas de layout ainda precisam ser verificadas em
aparelho fisico antes do modo sombra.

## Motor e Modo Lab

O motor Kotlin replica as regras deterministicas do simulador Node e permanece
sem dependencias Android. O bridge avalia apenas ofertas; uma viagem encontrada
limpa o estado atual. O Modo Lab mantém no máximo 20 diagnósticos normalizados
somente em memória, sem endereços, nomes, telefones, coordenadas ou IDs.

O preview usa uma oferta explicitamente fictícia. A sobreposição exige autorização
manual em Configurações e usa `TYPE_APPLICATION_OVERLAY` apenas para informar:
ela é não focável e não recebe toques.

## Perfis, agenda e financeiro

Perfis e custos ficam somente no aparelho, em preferências privadas do
aplicativo. O perfil padrão usa R$ 1,80/km, R$ 40/h, coleta máxima de 3,5 km e
8 minutos; o Modo Destino mantém seu perfil próprio. O motorista pode criar,
editar, duplicar, ativar e excluir perfis personalizados.

A agenda é opcional e usa intervalos com início inclusivo e fim exclusivo.
Faixas cujo fim é anterior ao início atravessam a meia-noite. Em sobreposições,
vence a maior prioridade e, em empate, o menor identificador da faixa; quando
nenhuma faixa está ativa, permanece o perfil selecionado manualmente. O perfil
de Destino tem precedência quando a oferta contém o indicador correspondente.

O custo por quilômetro soma combustível, manutenção, pneus e o rateio mensal de
seguro, IPVA e outros custos pela rodagem informada. Esse valor produz custo da
oferta, lucro líquido e lucro por minuto no overlay, mas nunca altera a decisão.
Se qualquer campo necessário estiver ausente ou inválido, a interface mostra
“não configurado” e não assume valor zero.

Serviço de acessibilidade, permissão de sobreposição e interface Lab são
habilitados somente no build `debug`. Supermercado, bloqueio geográfico e
tráfego já fazem parte do contrato injetável do bridge, mas permanecem nos
valores neutros até haver uma fonte local validada; nenhum seletor foi inferido.
