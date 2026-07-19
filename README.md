# Nexo Driver

Assistente Android para analisar ofertas de corridas em tempo real e classifica-las como **ACEITAR**, **ANALISAR** ou **RECUSAR**.

O projeto esta em fase de desenvolvimento pessoal. A prioridade e a Uber Driver; a 99 sera iniciada somente depois que a leitura da Uber estiver validada.

## Estrutura inicial

- `cloud/`: motor de decisao e simulador executaveis com Node.js, sem dependencias externas.
- `android/`: esqueleto do aplicativo Android pessoal.
- `fixtures/`: cenarios versionados que devem produzir o mesmo resultado na nuvem e no Android.
- `docs/`: plano vivo, arquitetura e rotina de validacao no aparelho.

## Executar na nuvem

Requer Node.js 20 ou superior.

```bash
npm test
npm run simulate
```

O motor usa, por enquanto, o perfil inicial confirmado:

- minimo de R$ 1,80 por km;
- meta de R$ 40,00 por hora;
- coleta de ate 3,5 km;
- coleta de ate 8 minutos;
- tolerancia interna de aproximadamente 10% para coleta;
- compensacao interna quando o valor por km supera a meta e o valor por hora fica ligeiramente abaixo.

## Regra de seguranca do desenvolvimento

Gestos e automacoes ficam restritos ao futuro aplicativo simulador. A versao pessoal real inicia em modo de leitura: calcula, mostra o overlay e registra o que o motor faria, sem clicar na Uber Driver.

Consulte [docs/cloud-device-workflow.md](docs/cloud-device-workflow.md) para o ciclo entre a nuvem e o aparelho fisico.
