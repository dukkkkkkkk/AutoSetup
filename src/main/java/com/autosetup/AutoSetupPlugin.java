package com.autosetup;

import com.zenith.command.api.CommandContext;
import com.zenith.command.api.CommandOutputHelper;
import com.zenith.command.api.CommandSources;
import com.zenith.plugin.api.Plugin;
import com.zenith.plugin.api.PluginAPI;
import com.zenith.plugin.api.ZenithProxyPlugin;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

import java.util.List;

import static com.zenith.Globals.COMMAND;
import static com.zenith.Globals.PLUGIN_MANAGER;

@Plugin(
    id = BuildConstants.PLUGIN_ID,
    version = BuildConstants.VERSION,
    description = "Runs a list of setup commands every time ZenithProxy starts",
    url = "https://github.com/dukkkkkkkk/AutoSetup",
    authors = {"dukkkkkkkk"},
    mcVersions = {BuildConstants.MC_VERSION}
)
public class AutoSetupPlugin implements ZenithProxyPlugin {

    // ===== EDIT THIS LIST =====
    // Write them exactly like you would in Discord. The leading "." is optional.
    private static final List<String> COMMANDS = List.of(
        ".b allowbreak off",
        ".b allowplace off",
        ".antiafk walk off",
        ".serverconnection ping off",
        ".chatrelay publicchat off",
        ".autodisconnect on",
        ".autodisconnect totempop on",
        ".autoDisconnect totemPop minTotemsRemaining 1",
        ".autototem popalert on",
        ".autototem popalert mention on",
        ".autototem nototemsalert mention on",
        ".chatrelay deathmessages off",
        ".alerts death mention on",
        ".visualrange replayrecording on",
        ".replay discordupload on",
        ".replay autorecord mode health",
        ".replay autorecord health 10",
        ".ka on",
        ".ka targethostilemobs onlyaggressive on",
        ".chatrelay whispermentions off",
        ".chatrelay namementions off",
        ".alerts disconnect mention off",
        ".visualrange on",
        ".chatrelay servermessages off",
        ".autoeat hunger 8",
        ".autoeat health 14",
        ".antiafk rotate off"
    );
    // ==========================

    private static ComponentLogger LOG;

    @Override
    public void onLoad(PluginAPI pluginAPI) {
        LOG = pluginAPI.getLogger();
        // ZenithProxy refuses to run commands until every plugin has finished loading,
        // so wait on a background thread instead of running them right here.
        Thread waiter = new Thread(AutoSetupPlugin::waitThenRun, "AutoSetup");
        waiter.setDaemon(true);
        waiter.start();
    }

    private static void waitThenRun() {
        try {
            long deadline = System.currentTimeMillis() + 60_000;
            while (!PLUGIN_MANAGER.isInitialized()) {
                if (System.currentTimeMillis() > deadline) {
                    LOG.warn("Plugins never finished loading, AutoSetup gave up");
                    return;
                }
                Thread.sleep(250);
            }
            Thread.sleep(1000); // small buffer so every module is ready

            LOG.info("AutoSetup: applying {} commands...", COMMANDS.size());
            for (String raw : COMMANDS) {
                String cmd = raw.trim();
                if (cmd.startsWith(".")) cmd = cmd.substring(1);
                if (cmd.isEmpty()) continue;
                runCommand(cmd);
            }
            LOG.info("AutoSetup: done!");
        } catch (InterruptedException ignored) {
        } catch (Throwable t) {
            LOG.error("AutoSetup failed", t);
        }
    }

    private static void runCommand(String cmd) {
        try {
            CommandContext ctx = CommandContext.create(cmd, CommandSources.TERMINAL);
            COMMAND.execute(ctx);
            LOG.info("AutoSetup ran: {}", cmd);
            // Print the command's normal reply in the console so you can spot typos
            CommandOutputHelper.logEmbedOutputToTerminal(ctx.getEmbed());
            CommandOutputHelper.logMultiLineOutputToTerminal(ctx.getMultiLineOutput());
        } catch (Throwable t) {
            LOG.warn("AutoSetup: command failed: {}", cmd, t);
        }
    }
}
