# AutoSetup

A [ZenithProxy](https://github.com/rfresh2/ZenithProxy) plugin that runs the commands in a `setup.txt` file — automatically when ZenithProxy starts, or whenever you ask.

## Install

1. Download the jar from the latest [release](https://github.com/dukkkkkkkk/AutoSetup/releases).
2. Put it in the `plugins` folder next to the ZenithProxy launcher.
3. Restart ZenithProxy. Plugins need the `java` release channel.
4. In your Zenith Discord channel, send `.autoSetup upload` with your `setup.txt` attached to the same message, then `.autoSetup run`.

You can also put `setup.txt` in the ZenithProxy folder by hand (next to the launcher, **not** inside `plugins`).

The plugin has no commands built in. Without a `setup.txt` it does nothing.

## setup.txt

One command per line, written the same way you would in Discord. The leading `.` is optional. Blank lines and lines starting with `#` are ignored.

```
# my bot settings
.autoeat hunger 8
.autoeat health 14
.ka on
```

There's a full example in [`setup.txt`].

The file is read fresh every run, so after uploading a new one just use `autoSetup run`, no restart needed.

## Commands

Use them from Discord (`.autoSetup ...`), the terminal, or in-game. **Account owner only**, because the commands in setup.txt run with full console permissions.

| Command | What it does |
| --- | --- |
| `autoSetup upload` | Replaces setup.txt with the `.txt` attached to the message (Discord only). The old one is kept as `setup.txt.bak` |
| `autoSetup download` | The bot sends your current setup.txt back (Discord only) |
| `autoSetup` / `autoSetup status` | Shows whether setup.txt is there, when it last ran, and any command that wasn't recognised |
| `autoSetup run` | Runs setup.txt now |
| `autoSetup list` | Shows the commands in setup.txt, numbered |
| `autoSetup add <command>` | Adds a line to the end of setup.txt (creates it if missing) |
| `autoSetup insert <number> <command>` | Adds a line at a position |
| `autoSetup del <number>` | Removes a command by its number from `list` |
| `autoSetup clear` | Removes every command from setup.txt (keeps `#` comments) |
| `autoSetup startup on/off` | Whether it runs every time ZenithProxy starts |

Each command's reply is printed in the ZenithProxy console, and any command ZenithProxy doesn't recognise is marked `NOT RECOGNISED` and shown in `autoSetup status`.

## Building

`./gradlew build` — the jar ends up in `build/libs`.

To publish a release: bump `plugin_version` in `gradle.properties`, then run **Actions → Publish Plugin → Run workflow**.
