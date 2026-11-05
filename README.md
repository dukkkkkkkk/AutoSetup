# AutoSetup

A [ZenithProxy](https://github.com/rfresh2/ZenithProxy) plugin that runs a list of commands every time ZenithProxy starts, so your settings are always applied.

## Install

1. Download the jar from the latest build in the **Actions** tab (or a release).
2. Put it in the `plugins` folder next to the ZenithProxy launcher.
3. Restart ZenithProxy. Plugins need the `java` release channel.

## Changing the commands

Edit the list in `src/main/java/com/autosetup/AutoSetupPlugin.java`, between the `EDIT THIS LIST` lines.
Write each command the same way you would in Discord. The leading `.` is optional.

Each command's reply is printed in the ZenithProxy console, so typos are easy to spot.

## Building

`./gradlew build` — the jar ends up in `build/libs`.
