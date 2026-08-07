package group.aelysium.rustyconnector.plugin.serverCommon;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.util.Parameter;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.Gson;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonObject;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.reflect.TypeToken;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.*;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.annotations.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Namespace("rustyconnector")
@Config("/config.yml")
public class DefaultConfig {
    private static Gson gson = new Gson();
    
    private final Map<String, Parameter> overrides = new HashMap<>();
    
    public DefaultConfig() {
        try {
            String json = System.getenv("RUSTYCONNECTOR_CONFIG");
            
            if (json != null && !json.isEmpty()) {
                JsonObject object = gson.fromJson(json, JsonObject.class);
                object.asMap().forEach((k, v) -> {
                    overrides.put(k, Parameter.fromJSON(v));
                });
            }
        } catch (Exception e) {
            RC.Error(Error.from(e));
        }
    }
    
    @Comment({
        "#",
        "# If you need help updating your configs from an older version;",
        "# take a look at our config migration docs:",
        "#",
        "# https://wiki.aelysium.group/rusty-connector/docs/updating/",
        "#"
    })
    @Node()
    private Integer version = 7;
    public int version() {
        try {
            return this.overrides.get("version").getAsInt();
        } catch(Exception ignore) {}
        return this.version;
    }

    @Comment({
        "#",
        "# The address used to connect to this server.",
        "# This address should match what a player would enter if they were trying to connect directly to this server.",
        "# Make sure you also include the port number!",
        "#",
        "# If you're in a Kubernetes or Docker environment, you can bypass this option by setting the RC_ADDRESS environment variable.",
        "#",
        "# Example: 127.0.0.1:25565",
        "#"
    })
    @Node(1)
    private String address = "127.0.0.1:25565";
    public String address() {
        try {
            return this.overrides.get("address").getAsString();
        } catch(Exception ignore) {}
        return this.address;
    }

    @Comment({
        "#",
        "# The family to register this server to.",
        "# Every server must be a member of a family.",
        "# If the server tries to register into a family that doesn't exist, the server won't register.",
        "#"
    })
    @Node(2)
    private String family = "lobby";
    public String family() {
        try {
            return this.overrides.get("family").getAsString();
        } catch(Exception ignore) {}
        return this.family;
    }

    @Comment({
        "#",
        "# The address for the Magic Link access point.",
        "# It's not necessary to provide a slash at the end.",
        "#",
        "# Example: http://127.0.0.1:8080",
        "#"
    })
    @Node(3)
    private String magicLink_accessEndpoint = "http://127.0.0.1:8080";
    public String magicLink_accessEndpoint() {
        try {
            return this.overrides.get("magicLink_accessEndpoint").getAsString();
        } catch(Exception ignore) {}
        return this.magicLink_accessEndpoint;
    }
/*
    @Comment({
        "#",
        "# This setting should only be used if endpoint broadcasting has been enabled on the Proxy.",
        "#",
        "# The address to use for Magic Link endpoint broadcasting.",
        "# This address should match what's defined on the proxy in the Magic Link config.",
        "# Make sure you also include the port number and ensure you follow the URI specifications for IPv6 addresses.",
        "#",
        "# Leave this empty to disable it.",
        "#",
        "# Example: [FF02::1]:4446",
        "#"
    })
    @Node(5)*/
    public String magicLink_broadcastingAddress = "";

    @Comment({
        "#",
        "# Should this server us a UUID for it's serverID instead of the family name + nano id?",
        "# This setting is really only necessary if you're in highly scalable environments and have",
        "# the infrastructure to properly handle UUIDs as server names.",
        "#",
        "# Enabling this setting is the same as setting this server's name in velocity.toml to a UUID.",
        "# Certain plugins aren't written to deal with server names that long and you'll have to implement",
        "# proper API support to display something other than server UUIDs to players.",
        "#",
        "# If you enable this, you'll need to delete `metadata/server.id` so that it can be regenerated with the new uuid.",
        "#"
    })
    @Node(4)
    private boolean useUUID = false;
    public boolean useUUID() {
        try {
            return this.overrides.get("useUUID").getAsBoolean();
        } catch(Exception ignore) {}
        return this.useUUID;
    }

    @Node(5)
    @Comment({
        "#",
        "# Provide additional metadata for the server",
        "# Metadata provided here is non-essential, meaning that RustyConnector is capable of running without anything provided here.",
        "# When you provide metadata here it will be sent to the proxy. Ensure that the provided metadata conforms to valid JSON syntax.",
        "#",
        "# For built-in metadata options, check the Aelysium wiki:",
        "# https://wiki.aelysium.group/rusty-connector/docs/concepts/metadata/",
        "#"
    })
    private String metadata = "{\\\"softCap\\\": 30, \\\"hardCap\\\": 40}";
    public String metadata() {
        try {
            return this.overrides.get("metadata").getAsString();
        } catch(Exception ignore) {}
        return this.metadata;
    }
    
    @Comment({
        "#",
        "# The directory that RustyConnector should scan for native modules.",
        "# Native modules are similar to Minecraft plugins except they've been written specifically for RustyConnector and do not depend on Minecraft code at all.",
        "#"
    })
    @Node(6)
    public String moduleDirectory = "/rc-module";
    public String moduleDirectory() {
        try {
            return this.overrides.get("moduleDirectory").getAsString();
        } catch(Exception ignore) {}
        return this.moduleDirectory;
    }
    
    @Comment({
        "#",
        "# The directory that RustyConnector should provide for modules to store their configs in.",
        "# Modules are able to ignore this setting if they really want, but well-written modules",
        "# will store their configs in the directory you provide inside of their own dedicated directory",
        "# Native modules are similar to Minecraft plugins except they've been written specifically for RustyConnector and do not depend on Minecraft code at all.",
        "#"
    })
    @Node(7)
    public String moduleConfigDirectory = "/rc-module";
    public String moduleConfigDirectory() {
        try {
            return this.overrides.get("moduleConfigDirectory").getAsString();
        } catch(Exception ignore) {}
        return this.moduleConfigDirectory;
    }

    public static DefaultConfig New() {
        return DeclarativeYAML.From(DefaultConfig.class);
    }
}
