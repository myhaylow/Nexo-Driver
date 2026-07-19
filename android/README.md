# Aplicativo Android

Esqueleto pessoal do Nexo Driver, configurado para Android 16 (`compileSdk` e `targetSdk` 36), AGP 9.2.1 e JDK 17.

O repositorio ainda nao inclui os binarios do Gradle Wrapper. Na maquina com Android Studio/Gradle 9.4.1, gere o wrapper antes do primeiro build:

```bash
cd android
gradle wrapper --gradle-version 9.4.1
./gradlew assembleDebug
```

O primeiro build fisico deve apenas abrir a tela de fundacao. Permissoes de acessibilidade, overlay e gestos serao adicionadas em fases separadas e testadas contra o simulador.
