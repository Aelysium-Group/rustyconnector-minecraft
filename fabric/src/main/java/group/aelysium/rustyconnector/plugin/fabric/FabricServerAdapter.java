package group.aelysium.rustyconnector.plugin.fabric;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.server.ServerAdapter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.minecraft.text.Text;
import org.incendo.cloud.CommandManager;
import org.jetbrains.annotations.NotNull;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FabricServerAdapter extends ServerAdapter {
    private final MinecraftServer server;
    private final CommandManager<CommandClient> commandManager;

    public FabricServerAdapter(@NotNull MinecraftServer server, @NotNull CommandManager<CommandClient> commandManager) {
        this.server = server;
        this.commandManager = commandManager;
    }

    @Override
    public void setMaxPlayers(int max) {
    }

    @Override
    public int onlinePlayerCount() {
        return this.server.getCurrentPlayerCount();
    }

    @Override
    public Optional<String> playerID(@NotNull String username) {
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(username);
        if(player == null) return Optional.empty();
        return Optional.of(player.getUuid().toString());
    }

    @Override
    public Optional<String> playerUsername(@NotNull String id) {
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(UUID.fromString(id));
        if(player == null) return Optional.empty();
        return Optional.of(player.getName().toString());
    }

    @Override
    public boolean isOnline(@NotNull String uuid) {
        ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(UUID.fromString(uuid));
        if(player == null) return false;
        if(player.isDisconnected()) return false;
        return true;
    }
    
    @Override
    public void teleport(@NotNull String fromPlayer, @NotNull String toPlayer) {
        try {
            ServerPlayerEntity player1 = server.getPlayerManager().getPlayer(UUID.fromString(fromPlayer));
            if(player1 == null) throw new NullPointerException("Player "+fromPlayer+" could not be found.");
            
            ServerPlayerEntity player2 = server.getPlayerManager().getPlayer(UUID.fromString(toPlayer));
            if(player2 == null) throw new NullPointerException("Player "+toPlayer+" could not be found.");
            player2.teleport(
                player1.getWorld(),
                player1.getX(),
                player1.getY(),
                player1.getZ(),
                Set.of(),
                player2.getPitch(),
                player2.getYaw(),
                false
            );
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To teleport player "+fromPlayer+" to player "+toPlayer));
        }
    }
    
    @Override
    public void teleport(@NotNull String from, @Nullable String world, @Nullable Double x, @Nullable Double y, @Nullable Double z, @Nullable Float pitch, @Nullable Float yaw) {
        try {
            ServerPlayerEntity player = server.getPlayerManager().getPlayer(UUID.fromString(from));
            if(player == null) throw new NullPointerException("Player "+from+" could not be found.");
            
            player.teleport(
                world == null ? player.getWorld() : this.server.getWorld(this.server.getWorldRegistryKeys().stream().filter(w->w.getValue().getPath().equals(world)).findAny().orElse(null)),
                x == null ? player.getX() : x,
                y == null ? player.getY() : y,
                z == null ? player.getZ() : z,
                Set.of(),
                pitch == null ? player.getPitch() : pitch,
                yaw == null ? player.getYaw() : yaw,
                false
            );
        } catch (Exception e) {
            RC.Error(Error.from(e).whileAttempting("To teleport player "+from+" to "+x+", "+y+", "+z+", ("+pitch+", "+yaw+")"));
        }
    }
    
    @Override
    public void messagePlayer(@NotNull String playerID, @NotNull Component message) {
        try {
            ServerPlayerEntity player = this.server.getPlayerManager().getPlayer(UUID.fromString(playerID));
            if(player == null) return;
            if(player.isDisconnected()) return;
            player.sendMessage(Text.literal(LegacyComponentSerializer.legacySection().serialize(message)));
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
    }
    
    @Override
    public CommandManager<CommandClient> commandManager() {
        return this.commandManager;
    }
    
    @Override
    public void log(@NotNull Component message) {
        this.server.sendMessage(Text.literal(LegacyComponentSerializer.legacySection().serialize(message)));
    }
}
