# MAF Car Launcher — Android 10 + GitHub Actions

Este é o código-fonte do protótipo, com workflow para compilar o APK na nuvem pelo GitHub Actions.

## Como gerar o APK somente pelo celular
1. No navegador do celular, entre em https://github.com e entre na sua conta (ou crie uma conta, se necessário).
2. Crie um repositório novo chamado `MAF-Car-Launcher-Android10`. Para facilitar o primeiro teste, marque-o como **Public**; não envie senhas, dados pessoais nem chaves.
3. No repositório, use **Add file > Upload files** e envie o conteúdo desta pasta, incluindo a pasta oculta `.github/workflows/build-apk.yml`. Se o celular não permitir enviar a pasta oculta, crie o arquivo manualmente pelo site com o caminho `.github/workflows/build-apk.yml` e cole o conteúdo do arquivo incluído no ZIP.
4. Faça **Commit changes** na branch `main`.
5. Abra a aba **Actions** no repositório e selecione **Build MAF Car Launcher APK**. A compilação também inicia automaticamente após o commit.
6. Abra a execução mais recente. Quando aparecer sucesso, procure **Artifacts** e baixe `MAF-Car-Launcher-Android10-APK`.
7. Extraia o ZIP do artifact e instale `app-debug.apk` no celular Android 10. Se o Android pedir, permita instalação dessa fonte nas configurações.

## Configuração
- minSdk 29 (Android 10)
- targetSdk 29
- compileSdk 35
- Gradle 8.9 / Java 17

## Limite funcional
O aplicativo pode solicitar abertura adjacente, mas a tela dividida real depende do suporte do Android e do firmware do dispositivo. O launcher não consegue forçar o modo se o fabricante o desativou.

## Privacidade
Para este teste, não inclua tokens, senhas, chaves de assinatura ou outros segredos no repositório. O APK de depuração é apropriado para teste local, não para publicação oficial.
