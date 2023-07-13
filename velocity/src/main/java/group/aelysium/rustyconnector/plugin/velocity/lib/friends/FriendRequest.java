package group.aelysium.rustyconnector.plugin.velocity.lib.friends;

import com.velocitypowered.api.proxy.Player;
import group.aelysium.rustyconnector.plugin.velocity.VelocityRustyConnector;
import group.aelysium.rustyconnector.plugin.velocity.central.VelocityAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.ref.WeakReference;

import static group.aelysium.rustyconnector.plugin.velocity.central.Processor.ValidServices.FRIENDS_SERVICE;

public class FriendRequest {
    private final WeakReference<Player> sender;
    private Player target;
    private Boolean isAcknowledged = null;

    public FriendRequest(Player sender, Player target) {
        this.sender = new WeakReference<>(sender);
        this.target = target;
    }

    public Player getSender() {
        return this.sender.get();
    }
    public Player getTarget() {
        return this.target;
    }


    /**
     * Accept the party invite.
     * This will subsequently connect the player to the party's server and then decompose the invite and remove it from the PartyService that it belongs to.
     */
    public synchronized void accept() {
        VelocityAPI api = VelocityRustyConnector.getAPI();
        if(!api.isEnabled(FRIENDS_SERVICE))
            throw new IllegalStateException("The friends module is disabled!");
        FriendsService friendsService = api.getService(FRIENDS_SERVICE).orElseThrow();

        try {
            if (friendsService.getFriendCount(this.target).orElseThrow() > friendsService.getSettings().maxFriends())
                throw new IllegalStateException("You've already maxed out the number of friends you can have.");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("There was a fatal error accepting your friend request!");
        }

        if(this.isAcknowledged != null)
            throw new IllegalStateException("This invite has already been acknowledged! You should close it using `PartyService#closeInvite`");
        if(this.sender.get() == null)
            throw new IllegalStateException("The sender of this friend request doesn't exist! (How did this happen?)");

        try {
            friendsService.getService(FriendsService.ValidServices.DATA_ENCLAVE).orElseThrow().addFriend(this.sender.get(), this.target);

            try {
                this.getTarget().sendMessage(Component.text("You and " + this.getSender().getUsername() + " are now friends!", NamedTextColor.GREEN));
                this.getSender().sendMessage(Component.text("You and " + this.getTarget().getUsername() + " are now friends!", NamedTextColor.GREEN));
            } catch (Exception ignore) {
                this.target.sendMessage(Component.text("You accepted the friend request!", NamedTextColor.GREEN));
            }

            friendsService.closeInvite(this);
            this.isAcknowledged = true;
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException("There was a fatal error accepting this friend request!");
        }
    }

    /**
     * Deny the party invite.
     * This will subsequently decompose the invite and remove it from the PartyService that it belongs to.
     */
    public synchronized void ignore() {
        VelocityAPI api = VelocityRustyConnector.getAPI();
        if(!api.isEnabled(FRIENDS_SERVICE))
            throw new IllegalStateException("The friends module is disabled!");
        FriendsService friendsService = api.getService(FRIENDS_SERVICE).orElseThrow();

        try {
            friendsService.closeInvite(this);
            this.isAcknowledged = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalStateException("There was a fatal error ignoring this friend request!");
    }

    public synchronized void decompose() {
        this.sender.clear();
        this.target = null;
    }
}