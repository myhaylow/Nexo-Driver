# Configuração do Codex Cloud — Nexo Driver

## 1. Criar o ambiente

Abra **Codex > Settings > Environments**, conecte o GitHub e selecione `myhaylow/Nexo-Driver`.

Use estas opções:

| Campo | Valor recomendado |
| --- | --- |
| Nome | `Nexo Driver Cloud` |
| Repositório | `myhaylow/Nexo-Driver` |
| Imagem | Universal |
| Setup script | `bash scripts/codex-cloud-setup.sh` |
| Maintenance script | `bash scripts/codex-cloud-maintenance.sh` |
| Cache | Ativado |
| Segredos | Nenhum nesta fase |

Enquanto o PR de bootstrap não for incorporado, abra as tarefas a partir da branch `agent/bootstrap-cloud-harness`. Depois do merge, use `main` como base.

## 2. Acesso de rede

O setup precisa baixar JDK/Android/Gradle e dependências. Durante a execução do agente, use acesso limitado e permita somente métodos `GET` e `HEAD` para:

- `dl.google.com`
- `services.gradle.org`
- `plugins.gradle.org`
- `repo.maven.apache.org`
- `maven.google.com`

O setup aquece o cache. Se um build posterior solicitar um domínio novo, valide que se trata de um repositório oficial de dependências antes de adicioná-lo.

## 3. Variáveis e versões

O script configura e persiste:

- Node.js fornecido pela imagem universal, versão mínima 20;
- JDK 17;
- Gradle 9.4.1;
- Android SDK/API 36 e Build Tools 36.0.0;
- `JAVA_HOME`, `ANDROID_HOME`, `ANDROID_SDK_ROOT` e `GRADLE_HOME`.

Não adicione tokens do GitHub, chaves de mapas ou dados de motorista como variáveis comuns. Quando uma integração realmente precisar de segredo, cadastre-o em **Secrets** e mantenha-o fora de logs e testes.

## 4. Primeiro teste na nuvem

Crie uma tarefa na branch de bootstrap com o prompt:

> Leia o AGENTS.md. Não altere arquivos. Execute os testes Node, valide as fixtures e confirme as versões de Java, Gradle e Android SDK. Informe qualquer dependência ausente.

O resultado esperado é:

- testes Node aprovados;
- fixtures válidas;
- Gradle capaz de carregar o projeto Android;
- nenhum arquivo modificado.

## 5. Estratégia de agentes

| Papel | Quando usar | Pode editar? |
| --- | --- | --- |
| Agente principal | Planejamento, integração e conclusão | Sim |
| `explorer` | Mapear código e dependências | Preferencialmente não |
| `android-engineer` | Implementação Android isolada | Sim |
| `decision-reviewer` | Fórmulas, regressões e cobertura | Não |
| `log-analyst` | Logs anonimizados e propostas de fixtures | Não |

O limite é de três threads e uma camada de delegação. Isso permite, por exemplo, implementar uma tela enquanto outro agente revisa o motor e um terceiro examina fixtures, sem criar uma árvore difícil de auditar.

## 6. Skills necessárias

Neste estágio, `AGENTS.md`, os agentes de projeto e os fluxos do GitHub cobrem o trabalho recorrente. Não é necessário criar uma skill própria ainda.

Crie uma skill de repositório somente quando um procedimento reaparecer com entradas e saídas estáveis, por exemplo:

- transformar logs anonimizados do aparelho em fixtures;
- validar uma nova variante de card da Uber;
- preparar um pacote noturno de testes físicos.

Até esse padrão existir, uma skill adicionaria manutenção sem reduzir erros.

## 7. Limite entre nuvem e aparelho

O Codex Cloud implementa motor, parser, telas, fixtures, testes e builds. O aparelho físico fica reservado para validar acessibilidade, overlay, latência, consumo e variantes reais da Uber. Resultados físicos devem voltar ao repositório como logs anonimizados e cenários reproduzíveis.
