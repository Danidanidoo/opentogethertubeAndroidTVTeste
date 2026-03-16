# OpenTogetherTube Android TV (room fixa)

App Android TV baseada em `WebView` que abre **sempre** a room:

- `https://opentogethertube.com/room/MahS2Dani`

## Funcionalidades implementadas

- Play/Pause por comando.
- Avançar e retroceder vídeo.
- Aumentar e baixar volume do player.
- Ativar/desativar legendas.
- Alternar velocidade de reprodução.
- Refresh da página.
- Chat visível, mas input de escrita desativado (somente leitura).
- Fullscreen forçado continuamente para evitar minimizar o vídeo.
- Bloqueio do botão back para evitar saída acidental da room.

## Mapeamento de teclas (comando TV)

- **Play/Pause**: `DPAD_CENTER`, `ENTER`, `MEDIA_PLAY_PAUSE`, `MEDIA_PLAY`, `MEDIA_PAUSE`
- **Avançar +10s**: `DPAD_RIGHT`, `MEDIA_FAST_FORWARD`
- **Retroceder -10s**: `DPAD_LEFT`, `MEDIA_REWIND`
- **Volume +**: `VOLUME_UP`
- **Volume -**: `VOLUME_DOWN`
- **Legendas on/off**: `CAPTIONS`, `C`, `PROG_BLUE`
- **Velocidade**: `MEDIA_STEP_FORWARD`, `MEDIA_NEXT`, `PROG_YELLOW`
- **Refresh**: `MENU`, `F5`, `REFRESH`, `PROG_RED`

## Build

Este repositório contém os ficheiros base de um projeto Android (Gradle Kotlin DSL).
