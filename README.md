# Bedrock Win Counter

`BedrockWinCounter` is a Paper plugin for Stream2Earn-style box formats.

It can track wins for both `s2e-bedrock-box` and `s2e-sand-box`, store both counters persistently, show them in-game, and provide a shared admin menu for the most important counter and box actions.

## Features

- Separate win counters for:
  - `BedrockBox`
  - `Sandbox`
- Automatic log-based win detection
- Persistent counters and optional target values in `plugins/BedrockWinCounter/data.yml`
- Shared `/bedrockmenu` hub with:
  - `BedrockBox`
  - `Sandbox`
  - `Sheep Out` placeholder
- Integrated counter management for both box types
- BossBar and sidebar display modes
- Per-box win marker configuration in `config.yml`
- Automatic merge of missing defaults into:
  - `config.yml`
  - `lang.yml`
  - `menu.yml`
- Automatic normalization of bundled default texts to proper German umlauts in `lang.yml`
- Sandbox log deduplication to prevent accidental multi-counting from repeated identical win markers

## Supported win markers

By default, the plugin listens for:

- `bedrock_win` for `s2e-bedrock-box`
- `[s2e-sand-box] win-up` for `s2e-sand-box`
- `win-up` as an additional Sandbox fallback marker

These markers can be changed in `config.yml`.

## Dependencies

This plugin is designed to work with one or both of these plugins:

- `s2e-bedrock-box`
- `s2e-sand-box`

Important:

- Automatic Bedrock win counting only works if the Bedrock plugin writes its win marker into the server log.
- Automatic Sandbox win counting only works if the Sandbox plugin writes its win marker into the server log.
- The shared menu can expose shortcuts for BedrockBox and Sandbox actions, but the underlying target plugin still needs to be installed and provide the called command.

## Requirements

- Paper `1.21` or higher
- Java `21` or higher

## Installation

1. Build the plugin with Maven or use a compiled release JAR.
2. Copy the JAR into your server's `plugins` folder.
3. Install `s2e-bedrock-box`, `s2e-sand-box`, or both.
4. Start the server once.
5. Open `plugins/BedrockWinCounter/`.
6. Review `config.yml`, `lang.yml`, and `menu.yml`.
7. Grant the required permissions to your team, streamer, or staff roles.

## Commands

### Shared menu

- `/bedrockmenu`
  - Opens the shared hub menu.

### Bedrock counter

- `/bedrockwins`
  - Shows the current Bedrock counter and target.
- `/bedrockwins add <number>`
- `/bedrockwins remove <number>`
- `/bedrockwins set <number>`
- `/bedrockwins reset`
- `/bedrockwins target <number>`
- `/bedrockwins target clear`
- `/bedrockwins display on`
- `/bedrockwins display off`
- `/bedrockwins display toggle`
- `/bedrockwins display bossbar`
- `/bedrockwins display sidebar`
- `/bedrockwins reload`

### Sandbox counter

- `/sandboxwins`
  - Shows the current Sandbox counter and target.
- `/sandboxwins add <number>`
- `/sandboxwins remove <number>`
- `/sandboxwins set <number>`
- `/sandboxwins reset`
- `/sandboxwins target <number>`
- `/sandboxwins target clear`
- `/sandboxwins display on`
- `/sandboxwins display off`
- `/sandboxwins display toggle`
- `/sandboxwins display bossbar`
- `/sandboxwins display sidebar`
- `/sandboxwins reload`

### Internal helper command

- `/bedrockwins menu <target|change|set> <init|adjust|reset|apply> [number]`
- `/sandboxwins menu <target|change|set> <init|adjust|reset|apply> [number]`

These are internal GUI helper commands and normally do not need to be used manually.

## Permissions

### Bedrock

- `bedrockwins.menu.use`
  - Allows opening the Bedrock section inside the shared menu.
- `bedrockwins.view`
  - Allows viewing the Bedrock counter and BossBar.
- `bedrockwins.admin`
  - Allows full Bedrock counter management.

### Sandbox

- `sandboxwins.menu.use`
  - Allows opening the Sandbox section inside the shared menu.
- `sandboxwins.view`
  - Allows viewing the Sandbox counter and BossBar.
- `sandboxwins.admin`
  - Allows full Sandbox counter management.

### Reserved placeholder

- `sheepout.menu.use`
- `sheepout.view`
- `sheepout.admin`

These are already registered for a future `Sheep Out` section.

## Generated files

After the first server start, the plugin creates and uses these files:

- `plugins/BedrockWinCounter/config.yml`
  - Win markers, display settings, and per-box options
- `plugins/BedrockWinCounter/lang.yml`
  - Chat messages, menu names, menu lore, and UI text
- `plugins/BedrockWinCounter/menu.yml`
  - Menu materials, button amounts, and layout slots
- `plugins/BedrockWinCounter/data.yml`
  - Stored counters, targets, and display mode state

## Configuration overview

### `config.yml`

Use this file to configure:

- win markers per box
- Sandbox log dedupe window
- display mode defaults
- scoreboard settings
- BossBar colors and progress
- chat prefix

Current structure is based on per-box sections such as:

- `boxes.bedrock.*`
- `boxes.sandbox.*`

### `lang.yml`

Use this file to configure:

- feedback messages
- permission messages
- menu titles
- menu item names
- menu lore

### `menu.yml`

Use this file to configure:

- item materials
- stack amounts for `+1`, `+5`, `+10`, `-1`, `-5`, `-10`
- layout slots
- screen sizes for all menus

## Shared menu overview

The shared `/bedrockmenu` hub currently contains:

- a root menu with `BedrockBox`, `Sandbox`, and `Sheep Out`
- a Bedrock submenu with box and counter shortcuts
- a Sandbox submenu with box and counter shortcuts
- shared target/change/set submenus for both counters

The `Sheep Out` entry is currently only a placeholder and does not track wins yet.

## Upgrade notes

If you are upgrading from an older Bedrock-only version:

1. Start the server once with the new JAR.
2. Let the plugin update missing default entries in `config.yml`, `lang.yml`, and `menu.yml`.
3. Check that `config.yml` now contains both:
   - `boxes.bedrock.win-markers`
   - `boxes.sandbox.win-markers`
4. Review your permissions for the new Sandbox commands and menu section.
5. If you customized `lang.yml`, verify the new menu entries and texts after startup.

## Typical workflow

### BedrockBox

1. `s2e-bedrock-box` writes `bedrock_win` into the server log.
2. `BedrockWinCounter` detects the marker in `latest.log`.
3. The Bedrock counter increases by `+1`.
4. The configured display updates automatically.

### Sandbox

1. `s2e-sand-box` writes its win marker into the server log.
2. `BedrockWinCounter` detects the Sandbox marker.
3. Repeated identical Sandbox win markers are deduplicated inside the configured time window.
4. The Sandbox counter increases by `+1`.
5. The configured display updates automatically.

## Notes

- The plugin does not require a database.
- Counter data survives restarts because it is stored in `data.yml`.
- Missing default entries are merged automatically on startup and reload.
- The plugin can still be used manually even if only one supported box plugin is installed.

## Contributing

Pull requests are welcome.

For contribution guidelines, see:

- `CONTRIBUTING.md`

## License

This project is released under the `Bedrock Win Counter Community License 1.0`.

See:

- `LICENSE`

## Author

- `Wick1990`
