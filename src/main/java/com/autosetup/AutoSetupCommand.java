package com.autosetup;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.zenith.command.api.Command;
import com.zenith.command.api.CommandCategory;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandUsage;
import com.zenith.discord.Embed;

import java.util.ArrayList;
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
                Runs a list of commands to set up your settings.
                Commands are written like in Discord, the leading . is optional.
                """)
            .usageLines(
                "run",
                "status",
                "list",
                "add <command>",
                "insert <number> <command>",
                "del <number>",
                "clear",
                "reset",
                "startup on/off"
            )
            .build();
    }

    @Override
    public LiteralArgumentBuilder<CommandContext> register() {
        return command("autoSetup")
            .executes(c -> {
                c.getSource().getEmbed()
                    .title("AutoSetup")
                    .description(statusText());
            })
            .then(literal("run").executes(c -> {
                if (CONFIG.commands.isEmpty()) {
                    c.getSource().getEmbed()
                        .title("Nothing to run")
                        .description("The list is empty. Add commands with `autoSetup add <command>`")
                        .errorColor();
                } else if (!AutoSetupPlugin.runLater("manual")) {
                    c.getSource().getEmbed()
                        .title("Already running")
                        .description("Wait for the current run to finish, then check `autoSetup status`")
                        .errorColor();
                } else {
                    c.getSource().getEmbed()
                        .title("Running " + CONFIG.commands.size() + " commands")
                        .description("Check `autoSetup status` in a few seconds to see if they all worked")
                        .successColor();
                }
            }))
            .then(literal("status").executes(c -> {
                c.getSource().getEmbed()
                    .title("AutoSetup Status")
                    .description(statusText());
            }))
            .then(literal("list").executes(c -> {
                c.getSource().getEmbed()
                    .title("AutoSetup Commands")
                    .description(listText());
            }))
            .then(literal("add").then(argument("command", greedyString()).executes(c -> {
                String cmd = AutoSetupPlugin.clean(getString(c, "command"));
                if (!checkAddable(c.getSource(), cmd)) return;
                CONFIG.commands.add(cmd);
                c.getSource().getEmbed()
                    .title("Added #" + CONFIG.commands.size())
                    .description("`" + cmd + "`")
                    .successColor();
            })))
            .then(literal("insert").then(argument("number", integer(1)).then(argument("command", greedyString()).executes(c -> {
                int number = getInteger(c, "number");
                String cmd = AutoSetupPlugin.clean(getString(c, "command"));
                if (!checkAddable(c.getSource(), cmd)) return;
                int index = Math.min(number, CONFIG.commands.size() + 1) - 1;
                CONFIG.commands.add(index, cmd);
                c.getSource().getEmbed()
                    .title("Inserted as #" + (index + 1))
                    .description("`" + cmd + "`")
                    .successColor();
            }))))
            .then(literal("del").then(argument("number", integer(1)).executes(c -> {
                int number = getInteger(c, "number");
                if (number > CONFIG.commands.size()) {
                    c.getSource().getEmbed()
                        .title("No command #" + number)
                        .description("There are only " + CONFIG.commands.size() + " commands. See `autoSetup list`")
                        .errorColor();
                    return;
                }
                String removed = CONFIG.commands.remove(number - 1);
                c.getSource().getEmbed()
                    .title("Removed #" + number)
                    .description("`" + removed + "`")
                    .successColor();
            })))
            .then(literal("clear").executes(c -> {
                CONFIG.commands.clear();
                c.getSource().getEmbed()
                    .title("List cleared")
                    .successColor();
            }))
            .then(literal("reset").executes(c -> {
                CONFIG.commands = new ArrayList<>(AutoSetupConfig.DEFAULT_COMMANDS);
                c.getSource().getEmbed()
                    .title("List reset to the default " + CONFIG.commands.size() + " commands")
                    .successColor();
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
            .addField("Commands", CONFIG.commands.size());
    }

    private static boolean checkAddable(CommandContext ctx, String cmd) {
        if (cmd.isEmpty()) {
            ctx.getEmbed().title("Empty command").errorColor();
            return false;
        }
        if (AutoSetupPlugin.isSelf(cmd)) {
            ctx.getEmbed()
                .title("Can't add that")
                .description("An autoSetup command in the list would make it run itself forever")
                .errorColor();
            return false;
        }
        return true;
    }

    private static String listText() {
        if (CONFIG.commands.isEmpty()) return "The list is empty. Add commands with `autoSetup add <command>`";
        StringBuilder sb = new StringBuilder();
        List<String> commands = CONFIG.commands;
        for (int i = 0; i < commands.size(); i++) {
            sb.append(i + 1).append(". `").append(commands.get(i)).append("`\n");
        }
        return sb.toString();
    }

    private static String statusText() {
        if (AutoSetupPlugin.isRunning()) return "Running right now...";
        if (AutoSetupPlugin.lastRunAt == 0) {
            return CONFIG.runOnStartup
                ? "Hasn't run yet. It runs a few seconds after ZenithProxy starts, or use `autoSetup run`"
                : "Hasn't run since ZenithProxy started. Use `autoSetup run`";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Last ran **").append(timeAgo(AutoSetupPlugin.lastRunAt)).append("** (")
            .append(AutoSetupPlugin.lastRunReason).append(")\n");
        List<String> failed = AutoSetupPlugin.lastRunFailed;
        if (failed.isEmpty()) {
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
