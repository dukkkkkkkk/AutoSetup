# AutoSetup

A [ZenithProxy](https://github.com/rfresh2/ZenithProxy) plugin that runs a list of commands to apply your settings — automatically when ZenithProxy starts, or whenever you ask.

## Install

1. Download the jar from the latest [release](https://github.com/dukkkkkkkk/AutoSetup/releases).
2. Put it in the `plugins` folder next to the ZenithProxy launcher.
3. Restart ZenithProxy. Plugins need the `java` release channel.

## Commands

Use them from Discord (`.autoSetup ...`), the terminal, or in-game.

| Command | What it does |
| --- | --- |
| `autoSetup` / `autoSetup status` | Shows when it last ran and whether any command wasn't recognised |
| `autoSetup run` | Runs the whole list now |
| `autoSetup list` | Shows the list, numbered |
| `autoSetup add <command>` | Adds a command to the end, e.g. `autoSetup add autoeat health 14` |
| `autoSetup insert <number> <command>` | Adds a command at a position in the list |
| `autoSetup del <number>` | Removes a command by its number from `list` |
| `autoSetup clear` | Empties the list |
| `autoSetup reset` | Puts the default list back |
| `autoSetup startup on/off` | Whether it runs every time ZenithProxy starts |

Write commands the same way you would in Discord. The leading `.` is optional.

The list is saved in `plugins/config/auto-setup.json`, so changes stick after restarts without rebuilding the plugin.

Each command's reply is printed in the ZenithProxy console, and any command ZenithProxy doesn't recognise is marked `NOT RECOGNISED` and shown in `autoSetup status`.

## Building

`./gradlew build` — the jar ends up in `build/libs`.

To publish a release: bump `plugin_version` in `gradle.properties`, then run **Actions → Publish Plugin → Run workflow**.
