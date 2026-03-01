# Token Finder

A client-side Fabric mod that extracts and copies session tokens from [Prism Launcher](https://prismlauncher.org/) accounts.

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-62B47A?style=flat-square&logo=mojangstudios&logoColor=white)
![Fabric](https://img.shields.io/badge/Fabric_API-0.138.4-DBD0B4?style=flat-square)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)
![Java](https://img.shields.io/badge/Java-21-ED8B00?style=flat-square&logo=openjdk&logoColor=white)

## Features

- Adds a **Session Tokens** button to the Minecraft title screen
- Lists all Prism Launcher accounts with a scrollable UI
- One-click copy of session tokens to clipboard
- Clean dark UI with alternating row colors and scroll bar

## Screenshots

<!-- Add screenshots here if you want -->
<!-- ![Title Screen](screenshots/title.png) -->
<!-- ![Token List](screenshots/list.png) -->

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) for Minecraft 1.21.10
2. Install [Fabric API](https://modrinth.com/mod/fabric-api)
3. Download the latest `.jar` from [Releases](https://github.com/bombsnbiscuits/token-finder/releases)
4. Drop it into your `.minecraft/mods` folder
5. Launch the game — the **Session Tokens** button will appear on the title screen

## Building from Source

```bash
git clone https://github.com/bombsnbiscuits/token-finder.git
cd token-finder
./gradlew build
```

The compiled `.jar` will be in `build/libs/`.

## How It Works

The mod reads Prism Launcher's `accounts.json` from `%APPDATA%/PrismLauncher/` and extracts the session token (`ygg.token`) for each account. Tokens can be copied to the clipboard from an in-game GUI.

> **Note:** This mod is Windows-only since it reads from `%APPDATA%`.

## License

[MIT](LICENSE)
