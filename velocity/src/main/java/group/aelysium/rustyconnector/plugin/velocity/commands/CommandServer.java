package group.aelysium.rustyconnector.plugin.velocity.commands;

import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.family.Family;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.annotations.Argument;
import org.incendo.cloud.annotations.Command;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class CommandServer {
    @Command("server")
    public void esdfdfitotgxtbmf(CommandClient.Player<?> player) {
        if(player == null) {
            RC.Adapter().log(Error.from("/server can only be used by players!").toComponent());
            return;
        }
        Optional<Server> currentServer = RC.P.PlayerFromID(player.id()).flatMap(Player::server);

        if (currentServer.isPresent()) {
            player.send(RC.Lang("velocity-serverUsage").generate(currentServer.get()));
        } else {
            player.send(RC.Lang("velocity-noServer").generate());
        }
    }

    @Command("server <family_name>")
    public void esdfdfitotgxtbmf(CommandClient.Player<?> player, @Argument(value = "family_name") String family_name) {
        if(player == null) {
            RC.Adapter().log(Error.from("/server can only be used by players!").toComponent());
            return;
        }
        try {
            Optional<Player> optionalRcPlayer = RC.P.PlayerFromID(player.id());
            if (optionalRcPlayer.isEmpty()) return;
            Player rcPlayer = optionalRcPlayer.get();

            Family family = RC.P.Family(family_name).orElseThrow();
            if (rcPlayer.family().isPresent() && rcPlayer.family().get() == family) {
                player.send(RC.Lang("velocity-alreadyInFamily").generate());
                return;
            };

            Player.Connection.Request request = family.connect(rcPlayer);
            Player.Connection.Result result = request.result().get(30, TimeUnit.SECONDS);

            if (result.connected()) return;

            player.send(result.message());
        } catch (NoSuchElementException e) {
            player.send(RC.Lang("rustyconnector-missing2").generate("family", family_name));
        } catch (Exception e) {
            player.send(Error.from(e).toComponent());
        }
    }
}
