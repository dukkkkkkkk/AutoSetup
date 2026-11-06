package com.autosetup;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

import java.io.IOException;
import java.util.List;

import static com.autosetup.AutoSetupPlugin.CONFIG;
import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.getString;
import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.zenith.command.brigadier.ToggleArgumentType.getToggle;
import static com.zenith.command.brigadier.ToggleArgumentType.toggle;

public class AutoSetupCommand extends Command {
    @Override
    public CommandUsage commandUsage() {
        return CommandUsage.builder()
            .name("autoSetup")
            .category(CommandCategory.MODULE)
            .description("""
                Runs the commands in setup.txt (in the ZenithProxy folder).
                One command per line, written like in Discord, the leading . is optional.
                Upload a new setup.txt by attaching it to `autoSetup upload` in Discord.
                Account owner only.
                """)
            .usageLines(
                "run",
                "status",
                "upload (attach setup.txt)",
                "download",
                "list",
                "add <command>",
                "insert <number> <command>",
                "del <number>",
                "clear",
                "startup on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        // Commands in setup.txt run with full terminal permissions, so only the
        // account owner may touch AutoSetup. Otherwise anyone could upload a file
        // containing owner-only commands and have them run.
        return command("autoSetup")
            .requires(Command::validateAccountOwner)
            .executes(c -> {
                c.getSource().getEmbed()
                    .title("AutoSetup")
                    .description(statusText());
            })
            .then(literal("run").executes(c -> {
                Embed embed = c.getSource().getEmbed();
                if (!SetupFile.exists()) {
                    noFile(embed);
                    return;
                }
                List<String> commands;
                try {
                    commands = SetupFile.readCommands();
                } catch (IOException e) {
                    fileError(embed, e);
                    return;
                }
                if (commands.isEmpty()) {
                    embed.title("Nothing to run")
                        .description("setup.txt has no commands in it")
                        .errorColor();
                } else if (!AutoSetupPlugin.runLater("manual")) {
                    embed.title("Already running")
                        .description("Wait for the current run to finish, then check `autoSetup status`")
                        .errorColor();
                } else {
                    embed.title("Running " + commands.size() + " commands from setup.txt")
                        .description("Check `autoSetup status` in a few seconds to see if they all worked")
                        .successColor();
                }
            }))
            .then(literal("upload").executes(c -> {
                DiscordFiles.upload(c.getSource());
            }))
            .then(literal("download").executes(c -> {
                DiscordFiles.download(c.getSource());
            }))
            .then(literal("status").executes(c -> {
                c.getSource().getEmbed()
                    .title("AutoSetup Status")
                    .description(statusText());
            }))
            .then(literal("list").executes(c -> {
                Embed embed = c.getSource().getEmbed();
                if (!SetupFile.exists()) {
                    noFile(embed);
                    return;
                }
                try {
                    embed.title("setup.txt").description(listText(SetupFile.readCommands()));
                } catch (IOException e) {
                    fileError(embed, e);
                }
            }))
            .then(literal("add").then(argument("command", greedyString()).executes(c -> {
                Embed embed = c.getSource().getEmbed();
                String cmd = AutoSetupPlugin.clean(getString(c, "command"));
                if (!checkAddable(embed, cmd)) return;
                try {
                    int number = SetupFile.add(cmd);
                    embed.title("Added #" + number + " to setup.txt")
                        .description("`" + cmd + "`")
                        .successColor();
                } catch (IOException e) {
                    fileError(embed, e);
                }
            })))
            .then(literal("insert").then(argument("number", integer(1)).then(argument("command", greedyString()).executes(c -> {
                Embed embed = c.getSource().getEmbed();
                String cmd = AutoSetupPlugin.clean(getString(c, "command"));
                if (!checkAddable(embed, cmd)) return;
                try {
                    int number = SetupFile.insert(getInteger(c, "number"), cmd);
                    embed.title("Inserted as #" + number + " in setup.txt")
                        .description("`" + cmd + "`")
                        .successColor();
                } catch (IOException e) {
                    fileError(embed, e);
                }
            }))))
            .then(literal("del").then(argument("number", integer(1)).executes(c -> {
                Embed embed = c.getSource().getEmbed();
                int number = getInteger(c, "number");
                try {
                    String removed = SetupFile.remove(number);
                    if (removed == null) {
                        embed.title("No command #" + number)
                            .description("See the numbers with `autoSetup list`")
                            .errorColor();
                    } else {
                        embed.title("Removed #" + number + " from setup.txt")
                            .description("`" + removed + "`")
                            .successColor();
                    }
                } catch (IOException e) {
                    fileError(embed, e);
                }
            })))
            .then(literal("clear").executes(c -> {
                Embed embed = c.getSource().getEmbed();
                try {
                    SetupFile.clear();
                    embed.title("Removed every command from setup.txt").successColor();
                } catch (IOException e) {
                    fileError(embed, e);
                }
            }))
            .then(literal("startup").then(argument("toggle", toggle()).executes(c -> {
                CONFIG.runOnStartup = getToggle(c, "toggle");
                c.getSource().getEmbed()
                    .title("Run on startup " + toggleStrCaps(CONFIG.runOnStartup))
                    .successColor();
            })));
    }

    @Override
    public void defaultEmbed(Embed embed) {
        embed
            .primaryColor()
            .addField("Run on startup", toggleStr(CONFIG.runOnStartup))
            .addField("setup.txt", SetupFile.exists() ? "Found" : "Missing");
    }

    private static boolean checkAddable(Embed embed, String cmd) {
        if (cmd.isEmpty()) {
            embed.title("Empty command").errorColor();
            return false;
        }
        if (AutoSetupPlugin.isSelf(cmd)) {
            embed.title("Can't add that")
                .description("An autoSetup command in setup.txt would make it run itself forever")
                .errorColor();
            return false;
        }
        return true;
    }

    private static void noFile(Embed embed) {
        embed.title("No setup.txt")
            .description("Send `.autoSetup upload` in Discord with your setup.txt attached, or put it here:\n`"
                + SetupFile.location() + "`\nYou can also start one with `autoSetup add <command>`")
            .errorColor();
    }

    private static void fileError(Embed embed, IOException e) {
        AutoSetupPlugin.LOG.warn("AutoSetup: problem with setup.txt", e);
        embed.title("Couldn't use setup.txt")
            .description(String.valueOf(e.getMessage()))
            .errorColor();
    }

    private static String listText(List<String> commands) {
        if (commands.isEmpty()) return "setup.txt has no commands in it";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < commands.size(); i++) {
            sb.append(i + 1).append(". `").append(commands.get(i)).append("`\n");
        }
        return sb.toString();
    }

    private static String statusText() {
        if (AutoSetupPlugin.isRunning()) return "Running right now...";
        String fileLine = SetupFile.exists()
            ? "setup.txt: found\n"
            : "setup.txt: **missing**. Attach one to `.autoSetup upload` in Discord\n";
        if (AutoSetupPlugin.lastRunAt == 0) {
            return fileLine + (CONFIG.runOnStartup
                ? "Hasn't run yet. It runs a few seconds after ZenithProxy starts, or use `autoSetup run`"
                : "Hasn't run since ZenithProxy started. Use `autoSetup run`");
        }
        StringBuilder sb = new StringBuilder(fileLine);
        sb.append("Last ran **").append(timeAgo(AutoSetupPlugin.lastRunAt)).append("** (")
            .append(AutoSetupPlugin.lastRunReason).append(")\n");
        String problem = AutoSetupPlugin.lastRunProblem;
        List<String> failed = AutoSetupPlugin.lastRunFailed;
        if (problem != null) {
            sb.append("Nothing was run: ").append(problem);
        } else if (failed.isEmpty()) {
            sb.append("All ").append(AutoSetupPlugin.lastRunCount).append(" commands worked");
        } else {
            sb.append(failed.size()).append(" of ").append(AutoSetupPlugin.lastRunCount)
                .append(" weren't recognised (typo?):\n");
            for (String f : failed) sb.append("- `").append(f).append("`\n");
        }
        return sb.toString();
    }

    private static String timeAgo(long millis) {
        long secs = (System.currentTimeMillis() - millis) / 1000;
        if (secs < 60) return secs + "s ago";
        long mins = secs / 60;
        if (mins < 60) return mins + "m ago";
        long hours = mins / 60;
        if (hours < 24) return hours + "h " + (mins % 60) + "m ago";
        return (hours / 24) + "d " + (hours % 24) + "h ago";
    }
}
