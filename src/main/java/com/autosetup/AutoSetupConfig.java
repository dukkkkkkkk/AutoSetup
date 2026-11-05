package com.autosetup;

import java.util.ArrayList;
import java.util.List;

/**
 * Saved to plugins/config/auto-setup.json and loaded automatically.
 * Edit it with the autoSetup command instead of rebuilding the plugin.
 */
public class AutoSetupConfig {
    // run the list every time ZenithProxy starts
    public boolean runOnStartup = true;

    // the commands to run, written like in Discord (leading "." optional)
    public List<String> commands = new ArrayList<>(DEFAULT_COMMANDS);

    // used by "autoSetup reset"
    public static final List<String> DEFAULT_COMMANDS = List.of(
        "b allowbreak off",
        "b allowplace off",
        "antiafk walk off",
        "serverconnection ping off",
        "chatrelay publicchat off",
        "autodisconnect on",
        "autodisconnect totempop on",
        "autoDisconnect totemPop minTotemsRemaining 1",
        "autototem popalert on",
        "autototem popalert mention on",
        "autototem nototemsalert mention on",
        "chatrelay deathmessages off",
        "alerts death mention on",
        "visualrange replayrecording on",
        "replay discordupload on",
        "replay autorecord mode health",
        "replay autorecord health 10",
        "ka on",
        "ka targethostilemobs onlyaggressive on",
        "chatrelay whispermentions off",
        "chatrelay namementions off",
        "alerts disconnect mention off",
        "visualrange on",
        "chatrelay servermessages off",
        "autoeat hunger 8",
        "autoeat health 14",
        "antiafk rotate off"
    );
}
