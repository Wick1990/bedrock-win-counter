# Bedrock Win Counter

`BedrockWinCounter` is a Paper plugin for Bedrock TNT Box setups based on `s2e-bedrock-box` (Stream2Earn).

It watches the server log for the win marker `bedrock_win`, stores the current win count persistently, displays the result in-game, and provides a permission-based integrated control menu for the most important Bedrock Box and win-counter actions.

## Features

- Automatic win detection through the `bedrock_win` log marker
- Persistent win counter stored in `plugins/BedrockWinCounter/data.yml`
- Optional win target display such as `0/10`
- BossBar display with dynamic colors:
  - red for negative wins
  - yellow/gold for `0`
  - green for positive wins
- Optional sidebar scoreboard mode
- Integrated `/bedrockmenu` GUI with permission checks
- Customizable menu texts via `lang.yml`
- Customizable menu materials, step sizes, and layout via `menu.yml`
- Automatic merge of newly added default entries into `lang.yml` and `menu.yml`

## Dependency

This plugin is designed for use with:

- `s2e-bedrock-box`

Important:

- The automatic win counter only makes sense if `s2e-bedrock-box` is installed and outputs the marker `bedrock_win`.
- Without `s2e-bedrock-box`, the plugin can still be used manually through commands, but can also listen to any other marker that might be output in the console. (Needs to be configured in config.yml)

## Requirements

- Paper `1.21` or higher
- Java `21` or higher

## Installation

1. Build the plugin with Maven or download the compiled JAR from a release.
2. Copy the JAR into your server's `plugins` folder.
3. Make sure `s2e-bedrock-box` is installed on the same server.
4. Start the server once so the plugin can generate its files.
5. Review the generated configuration files in `plugins/BedrockWinCounter/`.
6. Grant the required permissions to the appropriate staff or streamer roles.

## Commands

### Main commands

- `/bedrockmenu`
  - Opens the integrated Bedrock control menu.

- `/bedrockwins`
  - Shows the current win count and target.

### Win management

- `/bedrockwins add <number>`
  - Adds wins.

- `/bedrockwins remove <number>`
  - Removes wins.

- `/bedrockwins set <number>`
  - Sets the current win count directly.

- `/bedrockwins reset`
  - Resets the win counter to `0`.

- `/bedrockwins target <number>`
  - Sets a win target.

- `/bedrockwins target clear`
  - Removes the current win target.

### Display and maintenance

- `/bedrockwins display on`
  - Enables the display in BossBar mode.

- `/bedrockwins display off`
  - Disables the display completely.

- `/bedrockwins display toggle`
  - Toggles the display on or off.

- `/bedrockwins display bossbar`
  - Forces BossBar mode.

- `/bedrockwins display sidebar`
  - Forces sidebar scoreboard mode.

- `/bedrockwins reload`
  - Reloads `config.yml`, `lang.yml`, and `menu.yml`.

### Internal helper command

- `/bedrockwins menu <target|change|set> <init|adjust|reset|apply> [number]`
  - Internal helper command used by the integrated GUI.
  - Normally you do not need to use this manually.

## Permissions

- `bedrockwins.menu.use`
  - Allows a player to open `/bedrockmenu`.

- `bedrockwins.view`
  - Allows viewing the win counter.
  - Players with this permission can also see the BossBar display.

- `bedrockwins.admin`
  - Allows full management of the win counter and menu actions.
  - Includes `bedrockwins.view`.

## Generated files

After the first server start, the plugin creates and uses these files:

- `plugins/BedrockWinCounter/config.yml`
  - Core configuration for win markers and display behavior

- `plugins/BedrockWinCounter/lang.yml`
  - Messages, menu titles, and lore texts

- `plugins/BedrockWinCounter/menu.yml`
  - Menu materials, button sizes, and menu layout slots

- `plugins/BedrockWinCounter/data.yml`
  - Stored runtime data such as current wins, target wins, and display mode

## Configuration overview

### `config.yml`

Use this file to configure:

- the log markers that count as wins
- the default display mode
- scoreboard settings
- BossBar styling and text colors
- the chat prefix

### `lang.yml`

Use this file to configure:

- command feedback messages
- permission messages
- menu titles
- menu item names and lore

### `menu.yml`

Use this file to configure:

- menu item materials
- unified positive and negative button styles
- button stack sizes for `+1`, `+5`, `+10`, `-1`, `-5`, `-10`
- complete slot layout of all integrated submenus

## Typical workflow

1. `s2e-bedrock-box` triggers a win and writes `bedrock_win` into the server log.
2. `BedrockWinCounter` detects the marker in `latest.log`.
3. The plugin increments the stored win counter.
4. The BossBar or sidebar updates automatically.
5. Staff can adjust, reset, or target the counter through commands or `/bedrockmenu`.

## Notes

- The plugin does not require a database.
- The counter survives restarts because it is stored in `data.yml`.
- Players only see the BossBar if they have `bedrockwins.view`.
- Menu access is separated from win administration through `bedrockwins.menu.use`.

## Contributing

Pull requests are welcome.

All contributions are reviewed by the maintainers before merge. For details, see:

- `CONTRIBUTING.md`

## License

This project is released under the `Bedrock Win Counter Community License 1.0`.

In practical terms, this means:

- the source code is publicly available
- the plugin may be used, modified, and shared free of charge
- the plugin may be used on private and public servers
- selling the plugin, selling modified versions, or redistributing it as a paid product is not allowed without prior written permission

See:

- `LICENSE`

## Author

- `Wick1990`
