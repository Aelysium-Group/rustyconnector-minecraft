package group.aelysium.rustyconnector.plugin.velocity.event_handlers.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.player.Player;

import java.util.UUID;

public class OnPlayerChangeServer {
    /**
     * Also runs when a player first joins the proxy
     */
    @Subscribe(order = PostOrder.CUSTOM, priority = Short.MIN_VALUE)
    public EventTask onPlayerChangeServer(ServerPostConnectEvent event) {
            return EventTask.async(() -> {
                Player player = RC.P.Adapter().convertToRCPlayer(event.getPlayer());
                Server oldServer = null;
                try {
                    assert event.getPreviousServer() != null;
                    oldServer = RC.P.Server(event.getPreviousServer().getServerInfo().getName()).orElseThrow();
                } catch (Exception ignore) {}
                Server newServer = RC.P.Server(event.getPlayer().getCurrentServer().orElseThrow().getServerInfo().getName()).orElseThrow();

                RC.P.Adapter().onServerSwitch(player, oldServer, newServer);
            });
    }
}
