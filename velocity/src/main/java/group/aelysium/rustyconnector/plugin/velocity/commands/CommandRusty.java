package group.aelysium.rustyconnector.plugin.velocity.commands;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.magic_link.MagicLinkCore;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import org.incendo.cloud.annotation.specifier.Greedy;
import org.incendo.cloud.annotations.Command;
import org.incendo.cloud.annotations.Permission;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.BLUE;

@Command("rc")
@Permission("rustyconnector.commands.rc")
public final class CommandRusty {
    @Command("send <playerTarget> <target>")
    public void qxeafgbinengqytu(CommandClient.Console<?> client, String playerTarget, String target) {
        Player player = null;
        try {
            try {
                player = RC.P.PlayerFromID(playerTarget).orElseThrow();
            } catch (Exception ignore) {}
            player = RC.P.PlayerFromUsername(playerTarget).orElseThrow();
        } catch (Exception ignore) {}
        if (player == null) {
            client.send(text("No player "+playerTarget+" could be found.", DARK_BLUE));
            return;
        }
        if (!player.online()) {
            client.send(text(player.username()+" isn't online.", DARK_BLUE));
            return;
        }

        boolean isServer = RC.P.Server(target).isPresent();
        boolean isFamily = RC.P.Family(target).isPresent();

        if(isServer && isFamily) {
            client.send(text("Both a server and family have the id `"+target+"`. Please clarify if you want to send the player to a family or a server.", DARK_BLUE));
            return;
        }

        Player.Connectable connectable = null;
        if(isServer) connectable = RC.P.Server(target).orElseThrow();
        if(isFamily) connectable = RC.P.Family(target).orElseThrow();

        if(connectable == null) {
            client.send(RC.Lang("rustyconnector-sendFail").generate(target));
            return;
        }

        try {
            Player.Connection.Request request = connectable.connect(player);
            Player.Connection.Result result = request.result().get(30, TimeUnit.SECONDS);

            if (result.connected()) return;

            client.send(result.message());
        } catch (Exception e) {
            RC.Error(Error.from(e).urgent(true));
        }
    }
    @Command("send <playerTarget> <target> <flags>")
    private void mgwsedhgsmudghug(
        CommandClient.Console<?> client,
        String playerTarget,
        String target,
        @Greedy String flags
    ) {
        if(flags.equals("family") || flags.equals("server")) {
            RC.Error(Error.from("The '/rc send <playerTarget> <target> <family|server>' syntax has been replaced with a flags system. To specifically target a family or server use -f or -s respectively.").urgent(true));
            return;
        }
        
        Player player = null;
        try {
            try {
                player = RC.P.PlayerFromID(playerTarget).orElseThrow();
            } catch (Exception ignore) {}
            player = RC.P.PlayerFromUsername(playerTarget).orElseThrow();
        } catch (Exception ignore) {}
        if (player == null) {
            client.send(text("No player "+playerTarget+" could be found.", DARK_BLUE));
            return;
        }
        if (!player.online()) {
            client.send(text(player.username()+" isn't online.", DARK_BLUE));
            return;
        }
        
        Set<MagicLinkCore.Packets.SendPlayer.Flag> flagSet = Arrays.stream(
                flags.replaceAll("\\s*-+([a-zA-Z])\\s*", "$1").split("")
            )
            .map(f -> switch (f) {
                case "f" -> MagicLinkCore.Packets.SendPlayer.Flag.FAMILY;
                case "s" -> MagicLinkCore.Packets.SendPlayer.Flag.SERVER;
                case "i" -> MagicLinkCore.Packets.SendPlayer.Flag.MINIMAL;
                case "o" -> MagicLinkCore.Packets.SendPlayer.Flag.MODERATE;
                case "a" -> MagicLinkCore.Packets.SendPlayer.Flag.AGGRESSIVE;
                default -> null;
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        Player.Connectable connectable = null;
        if(flagSet.contains(MagicLinkCore.Packets.SendPlayer.Flag.FAMILY)) connectable = RC.P.Family(target).orElseThrow();
        else if(flagSet.contains(MagicLinkCore.Packets.SendPlayer.Flag.SERVER)) connectable = RC.P.Server(target).orElseThrow();
        else {
            try {
                connectable = RC.P.Family(target).orElseThrow();
            } catch(Exception ignore) {}
            
            if(connectable == null) try {
                connectable = RC.P.Server(target).orElseThrow();
            } catch (Exception ignore2) {}
        }
        
        try {
            if(connectable == null) throw new NullPointerException("The requested server or family "+target+" could not be found.");
            
            Player.Connection.Request request = connectable.connect(player);
            Player.Connection.Result result = request.result().get(30, TimeUnit.SECONDS);
            
            if (result.connected()) return;
            
            client.send(result.message());
        } catch (Exception e) {
            RC.Error(Error.from(e).urgent(true));
        }
    }

    @Command("server")
    @Command("servers")
    public void ftuynemwdiuemhid(CommandClient.Console<?> client) {
        client.send(RC.Lang("rustyconnector-servers").generate());
    }

    @Command("server <serverID>")
    @Command("servers <serverID>")
    public void fneriygwehmigimh(CommandClient.Console<?> client, String serverID) {
        try {
            Server server = RC.P.Server(serverID)
                    .orElseThrow(()->new NoSuchElementException("No server with the id '"+serverID+"' exists."));
            client.send(RC.Lang("rustyconnector-serverDetails").generate(server));
        } catch (Exception e) {
            RC.Error(Error.from(e).urgent(true));
        }
    }

    @Command("family")
    @Command("families")
    public void tdrdolhxvcjhaskb(CommandClient.Console<?> client) {
        client.send(RC.Lang("rustyconnector-details").generate("Families", "All of the families currently registered on the proxy.", Optional.of(RC.P.Families())));
    }

    @Command("family <id>")
    @Command("families <id>")
    public void mfndwqqzuiqmesyn(CommandClient.Console<?> client, String id) {
        try {
            Family family = RC.P.Family(id)
                    .orElseThrow(()->new NoSuchElementException("No family with the id ["+id+"] exists."));
            
            client.send(RC.Lang("rustyconnector-details").generate(family.id(), "", Optional.of(family)));
        } catch (Exception e) {
            RC.Error(Error.from(e).urgent(true));
        }
    }
}