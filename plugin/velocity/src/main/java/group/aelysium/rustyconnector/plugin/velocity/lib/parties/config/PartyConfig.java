package group.aelysium.rustyconnector.plugin.velocity.lib.parties.config;

import group.aelysium.rustyconnector.core.lib.config.YAML;
import group.aelysium.rustyconnector.core.lib.exception.NoOutputException;
import group.aelysium.rustyconnector.plugin.velocity.central.Tinder;
import group.aelysium.rustyconnector.plugin.velocity.lib.lang.VelocityLang;
import group.aelysium.rustyconnector.plugin.velocity.lib.parties.SwitchPower;
import net.kyori.adventure.text.format.NamedTextColor;

import java.io.File;

public class PartyConfig extends YAML {
    private boolean enabled = false;
    private int maxMembers = 5;

    private boolean friendsOnly = false;
    private boolean localOnly = false;

    private boolean partyLeader_onlyLeaderCanInvite = true;
    private boolean partyLeader_onlyLeaderCanKick = true;
    private boolean partyLeader_onlyLeaderCanSwitchServers = true;
    private boolean partyLeader_disbandOnLeaderQuit = true;

    private SwitchPower switchingServers_switchPower = SwitchPower.MODERATE;

    private boolean switchingServers_kickOnFailedSend = true;

    public PartyConfig(File configPointer) {
        super(configPointer);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getMaxMembers() {
        return maxMembers;
    }

    public boolean isFriendsOnly() {
        return friendsOnly;
    }
    public boolean isLocalOnly() {
        return localOnly;
    }

    public boolean isPartyLeader_onlyLeaderCanInvite() {
        return partyLeader_onlyLeaderCanInvite;
    }

    public boolean isPartyLeader_onlyLeaderCanKick() {
        return partyLeader_onlyLeaderCanKick;
    }

    public boolean isPartyLeader_onlyLeaderCanSwitchServers() {
        return partyLeader_onlyLeaderCanSwitchServers;
    }

    public boolean isPartyLeader_disbandOnLeaderQuit() {
        return partyLeader_disbandOnLeaderQuit;
    }

    public SwitchPower getSwitchingServers_switchPower() {
        return switchingServers_switchPower;
    }

    public boolean getSwitchingServers_kickOnSendFailure() {
        return switchingServers_kickOnFailedSend;
    }

    @SuppressWarnings("unchecked")
    public void register() throws IllegalStateException, NoOutputException {
        this.enabled = this.getNode(this.data, "enabled", Boolean.class);
        if(!this.enabled) return;

        this.maxMembers = this.getNode(this.data, "max-members", Integer.class);

        try {
            this.friendsOnly = this.getNode(this.data, "friends-only", Boolean.class);
            if(this.friendsOnly)
                Tinder.get().services().friendsService().orElseThrow();
        } catch (Exception ignore) {
            Tinder.get().logger().send(VelocityLang.BOXED_MESSAGE_COLORED.build("[friends-only] in `party.yml` is set to true. But the friends module isn't enabled! Ignoring...", NamedTextColor.YELLOW));
            this.friendsOnly = false;
        }
        this.localOnly = this.getNode(this.data, "local-only", Boolean.class);

        this.partyLeader_onlyLeaderCanInvite = this.getNode(this.data, "party-leader.only-leader-can-invite", Boolean.class);
        this.partyLeader_onlyLeaderCanKick = this.getNode(this.data, "party-leader.only-leader-can-kick", Boolean.class);
        this.partyLeader_onlyLeaderCanSwitchServers = this.getNode(this.data, "party-leader.only-leader-can-switch-servers", Boolean.class);
        this.partyLeader_disbandOnLeaderQuit = this.getNode(this.data, "party-leader.disband-on-leader-quit", Boolean.class);

        try {
            this.switchingServers_switchPower = Enum.valueOf(SwitchPower.class, this.getNode(this.data, "switching-servers.switch-power", String.class));
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Switch power: "+this.switchingServers_switchPower+" isn't valid! Please review `party.yml` again!");
        }

        this.switchingServers_kickOnFailedSend = this.getNode(this.data, "switching-servers.kick-on-failure", Boolean.class);
    }
}
