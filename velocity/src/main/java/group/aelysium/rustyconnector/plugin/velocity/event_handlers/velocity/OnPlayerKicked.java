package group.aelysium.rustyconnector.plugin.velocity.event_handlers.velocity;

import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.KickedFromServerEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.proxy.player.Player;
import group.aelysium.rustyconnector.proxy.ProxyAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class OnPlayerKicked {

    private final ProxyServer proxyServer;

    public OnPlayerKicked(ProxyServer proxyServer) {
        this.proxyServer = proxyServer;
    }

    /**
     * Runs when a player disconnects from a player server
     */

    @Subscribe(order = PostOrder.CUSTOM, priority = Short.MIN_VALUE)
    public EventTask onPlayerKicked(KickedFromServerEvent event) {
        return EventTask.async(() -> {
            Player player = RC.P.Adapter().convertToRCPlayer(event.getPlayer());
            net.kyori.adventure.text.Component reason = ((net.kyori.adventure.text.Component) event.getServerKickReason().orElse(Component.text("Kicked by server.", NamedTextColor.RED)));
            ProxyAdapter.PlayerKickedResponse r = RC.P.Adapter().onKicked(player, reason);

            if(r.redirect() == null) {
                event.setResult(KickedFromServerEvent.DisconnectPlayer.create(r.reason()));
                return;
            }

            proxyServer.getServer(r.redirect().id()).ifPresent(registeredServer -> {
                event.setResult(KickedFromServerEvent.RedirectPlayer.create(registeredServer, r.reason()));
            });
        });
    }
}
