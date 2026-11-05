package com.autosetup;

import com.mojang.brigadier.ParseResults;
import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandOutputHelper;
import com.zenith.command.api.CommandSources;
import com.zenith.plugin.api.Plugin;
import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.zenith.Globals.COMMAND;
import static com.zenith.Globals.PLUGIN_MANAGER;

@Plugin(
    id = BuildConstants.PLUGIN_ID,
    version = BuildConstants.VERSION,
    description = "Runs a list of setup commands when ZenithProxy starts, or on demand",
    url = "https://github.com/dukkkkkkkk/AutoSetup",
    authors = {"dukkkkkkkk"},
    mcVersions = {BuildConstants.MC_VERSION}
)
public class AutoSetupPlugin implements ZenithProxyPlugin {
    public static AutoSetupConfig CONFIG;
    public static ComponentLogger LOG;

    // ---- info about the last run, shown by "autoSetup status" ----
    public static volatile long lastRunAt = 0;          // 0 = hasn't run since ZenithProxy started
    public static volatile String lastRunReason = "";   // "startup" or "manual"
    public static volatile int lastRunCount = 0;
    public static volatile List<String> lastRunFailed = List.of();
    private static final AtomicBoolean RUNNING = new AtomicBoolean(false);

    @Override
    public void onLoad(PluginAPI pluginAPI) {
        LOG = pluginAPI.getLogger();
        CONFIG = pluginAPI.registerConfig(BuildConstants.PLUGIN_ID, AutoSetupConfig.class);
        pluginAPI.registerCommand(new AutoSetupCommand());

        if (CONFIG.runOnStartup) {
            // ZenithProxy refuses to run commands until every plugin has finished loading,
            // so wait on a background thread instead of running them right here.
            startThread(AutoSetupPlugin::waitForPluginsThenRun);
        } else {
            LOG.info("AutoSetup: run on startup is off, use 'autoSetup run' to apply");
        }
    }

    public static boolean isRunning() {
        return RUNNING.get();
    }

    /** Runs the list in the background. Returns false if a run is already going. */
    public static boolean runLater(String reason) {
        if (isRunning()) return false;
        startThread(() -> {
            try {
                // let the command that asked for this finish replying first
                Thread.sleep(500);
            } catch (InterruptedException e) {
                return;
            }
            runAll(reason);
        });
        return true;
    }

    private static void waitForPluginsThenRun() {
        try {
            long deadline = System.currentTimeMillis() + 60_000;
            while (!PLUGIN_MANAGER.isInitialized()) {
                if (System.currentTimeMillis() > deadline) {
                    LOG.warn("AutoSetup: plugins never finished loading, gave up");
                    return;
                }
                Thread.sleep(250);
            }
            Thread.sleep(1000); // small buffer so every module is ready
        } catch (InterruptedException e) {
            return;
        }
        runAll("startup");
    }

    private static void runAll(String reason) {
        if (!RUNNING.compareAndSet(false, true)) return;
        try {
            List<String> commands = List.copyOf(CONFIG.commands);
            List<String> failed = new ArrayList<>();
            LOG.info("AutoSetup: applying {} commands ({})...", commands.size(), reason);
            for (String raw : commands) {
                String cmd = clean(raw);
                if (cmd.isEmpty()) continue;
                if (isSelf(cmd)) {
                    LOG.warn("AutoSetup: skipped '{}' (it would run itself)", cmd);
                    continue;
                }
                if (!runCommand(cmd)) failed.add(cmd);
            }
            lastRunAt = System.currentTimeMillis();
            lastRunReason = reason;
            lastRunCount = commands.size();
            lastRunFailed = List.copyOf(failed);
            if (failed.isEmpty()) {
                LOG.info("AutoSetup: done! All {} commands ran", commands.size());
            } else {
                LOG.warn("AutoSetup: done, but {} didn't work: {}", failed.size(), failed);
            }
        } catch (Throwable t) {
            LOG.error("AutoSetup failed", t);
        } finally {
            RUNNING.set(false);
        }
    }

    /** Returns false if ZenithProxy didn't recognise the command (typo, wrong args, etc). */
    private static boolean runCommand(String cmd) {
        try {
            CommandContext ctx = CommandContext.create(cmd, CommandSources.TERMINAL);
            ParseResults<CommandContext> parse = COMMAND.parse(ctx);
            boolean valid = COMMAND.hasCommandNode(parse)
                && parse.getExceptions().isEmpty()
                && !parse.getReader().canRead();
            COMMAND.execute(ctx, parse);
            LOG.info("AutoSetup ran: {}{}", cmd, valid ? "" : "  <-- NOT RECOGNISED");
            // print the command's normal reply in the console
            CommandOutputHelper.logEmbedOutputToTerminal(ctx.getEmbed());
            CommandOutputHelper.logMultiLineOutputToTerminal(ctx.getMultiLineOutput());
            return valid;
        } catch (Throwable t) {
            LOG.warn("AutoSetup: command failed: {}", cmd, t);
            return false;
        }
    }

    /** Strips spaces and a leading "." so commands can be written like in Discord. */
    public static String clean(String cmd) {
        String s = cmd.trim();
        if (s.startsWith(".")) s = s.substring(1).trim();
        return s;
    }

    /** True for "autoSetup ..." commands, which would make the list run itself forever. */
    public static boolean isSelf(String cmd) {
        String first = cmd.split(" ", 2)[0];
        return first.equalsIgnoreCase("autosetup");
    }

    private static void startThread(Runnable r) {
        Thread t = new Thread(r, "AutoSetup");
        t.setDaemon(true);
        t.start();
    }
}
