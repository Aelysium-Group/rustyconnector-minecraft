package group.aelysium.rustyconnector.plugin.paper.lang;

import group.aelysium.rustyconnector.common.lang.Lang;
import group.aelysium.rustyconnector.plugin.common.lang.CommonLang;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.concurrent.TimeUnit;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public class PaperLang extends CommonLang {
    public PaperLang() {}

    @Lang("rustyconnector-kernelDetails")
    public static Component usage() {
        return join(
                newlines(),
                space(),
                text("rc send <player> <target>", BLUE),
                text("Send a player to a family or server.", DARK_GRAY),
                space(),
                text("rc packets ['clear' | <packet_id>]", BLUE),
                text("Access recently sent MagicLink packets.", DARK_GRAY),
                space(),
                text("rc reload", BLUE),
                text("Reload RustyConnector.", DARK_GRAY),
                space(),
                text("rc plugins [plugin_node] ['start' | 'stop' | 'reload']", BLUE),
                text("Get details for RustyConnector modules.", DARK_GRAY),
                space(),
                text("rc errors [error_uuid]", BLUE),
                text("Fetches the recent errors thrown by RustyConnector.", DARK_GRAY)
        );
    }

    @Lang("rustyconnector-magicLinkHandshake")
    public static Component magicLink() {
        return join(
                newlines(),
                space(),
                text("              /(¯`·._.·´¯`·._.·´¯`·._.·´¯`·._.·´¯)\\"),
                text("          --<||(     MAGIC LINK -- CONNECTED     )||>--"),
                text("              \\(_.·´¯`·._.·´¯`·._.·´¯`·._.·´¯`·._)/")
        ).color(DARK_PURPLE);
    }

    @Lang("rustyconnector-consoleOnly")
    public static final String consoleOnly = "This command can only be executed from the console.";

    public static Component paperFolia(boolean isFolia) {
        if(isFolia) return text("RustyConnector-Folia");
        return text("RustyConnector-Paper");
    };

    public static Component paperFoliaLowercase(boolean isFolia) {
        if(isFolia) return text("rustyconnector-folia");
        return text("rustyconnector-paper");
    };

    @Lang("rustyconnector-magicLinkHandshakeFailure")
    public static Component magicLinkHandshakeFailure(String reason, int delayAmount, TimeUnit delayUnit) {
        return join(
                newlines(),
                text(reason, NamedTextColor.RED),
                text("Waiting "+delayAmount+" "+delayUnit+" before trying again...", NamedTextColor.GRAY)
        );
    }
}
