package group.aelysium.rustyconnector.plugin.fabric;

import group.aelysium.rustyconnector.api.RustyConnectorAPI;
import group.aelysium.rustyconnector.core.TinderAdapterForCore;
import group.aelysium.rustyconnector.core.plugin.lib.lang.PluginLang;
import group.aelysium.rustyconnector.plugin.fabric.central.Tinder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.apache.logging.log4j.LogManager;
import org.slf4j.Logger;

public final class FabricRustyConnector implements ModInitializer {

    private MinecraftServer server;

    @Override
    public void onInitialize() {
        Tinder api = Tinder.gather(this, (Logger) LogManager.getLogger());
        TinderAdapterForCore.init(api);

        api.logger().log("Initializing RustyConnector...");
        api.ignite();
        PluginLang.WORDMARK_RUSTY_CONNECTOR.send(api.logger(), api.flame().versionAsString());

        RustyConnectorAPI.register(api);

        ServerLifecycleEvents.SERVER_STARTING.register(server1 -> server = server1);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            RustyConnectorAPI.unregister();
            Tinder.get().flame().exhaust();
        });
    }

    public MinecraftServer getServer() {
        return server;
    }
}