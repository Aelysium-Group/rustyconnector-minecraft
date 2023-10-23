package group.aelysium.rustyconnector.core.central;

import group.aelysium.rustyconnector.core.lib.lang.config.LangService;
import group.aelysium.rustyconnector.core.lib.serviceable.ServiceHandler;
import group.aelysium.rustyconnector.core.plugin.central.CoreServiceHandler;
import net.kyori.adventure.text.Component;

import java.io.InputStream;
import java.util.UUID;

public abstract class Tinder {
    /**
     * Gets a resource by name and returns it as a stream.
     * @param filename The name of the resource to get.
     * @return The resource as a stream.
     */
    public static InputStream resourceAsStream(String filename)  {
        return Tinder.class.getClassLoader().getResourceAsStream(filename);
    }

    //abstract public S scheduler();

    abstract public void ignite();

    abstract public Flame<CoreServiceHandler> flame();

    abstract public PluginLogger logger();

    abstract public CoreServiceHandler services();

    abstract public String dataFolder();

    abstract public LangService lang();

    abstract public void setMaxPlayers(int max);

    abstract public int onlinePlayerCount();

    abstract public UUID getPlayerUUID(String name);

    abstract public String getPlayerName(UUID uuid);

    abstract public boolean isOnline(UUID uuid);

    abstract public void teleportPlayer(UUID uuid, UUID target);

    abstract public void sendMessage(UUID uuid, Component component);
}