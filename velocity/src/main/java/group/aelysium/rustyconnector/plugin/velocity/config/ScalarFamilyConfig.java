package group.aelysium.rustyconnector.plugin.velocity.config;

import group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.declarative_yaml.annotations.*;
import group.aelysium.declarative_yaml.lib.Printer;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.modules.ModuleBuilder;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancerGeneratorExchange;
import group.aelysium.rustyconnector.proxy.family.scalar_family.ScalarFamily;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.Gson;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonObject;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;

import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

@Namespace("rustyconnector")
@Config("/scalar_families/{id}.yml")
@Comment({
        "############################################################",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "#                      Scalar Family                       #",
        "#                                                          #",
        "#               ---------------------------                #",
        "# | Scalar families are optimized for stateless            #",
        "# | minecraft gamemodes.                                   #",
        "#               ---------------------------                #",
        "#                                                          #",
        "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
        "############################################################",
})
public class ScalarFamilyConfig {
    @PathParameter("id")
    private String id;

    @Comment({
            "############################################################",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "#                       Display Name                       #",
            "#                                                          #",
            "#               ---------------------------                #",
            "# | Display name is the name of your family, as players    #",
            "# | will see it, in-game.                                  #",
            "# | Display name can appear as a result of multiple        #",
            "# | factors such as the friends module being enabled.      #",
            "#                                                          #",
            "# | Multiple families are allowed to have the              #",
            "# | same display name.                                     #",
            "#                                                          #",
            "#               ---------------------------                #",
            "#                                                          #",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "############################################################"
    })
    @Node(0)
    private String displayName = "";

    @Comment({
            "############################################################",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "#                      Parent Family                       #",
            "#                                                          #",
            "#               ---------------------------                #",
            "# | The parent family is the family that players will      #",
            "# | be sent to when they run /hub, or when a fallback      #",
            "# | occurs. If the parent family is unavailable, the       #",
            "# | root family is used instead.                           #",
            "#                                                          #",
            "#   NOTE: If this value is set for the root family         #",
            "#         it will be ignored.                              #",
            "#                                                          #",
            "#               ---------------------------                #",
            "#                                                          #",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "############################################################"
    })
    @Node(1)
    private String parentFamily = "";

    @Node(2)
    @Comment({
            "############################################################",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "#                      Load Balancing                      #",
            "#                                                          #",
            "#               ---------------------------                #",
            "#                                                          #",
            "# | Load balancing is the system through which networks    #",
            "# | manage player influxes by spreading out players        #",
            "# | across various server nodes.                           #",
            "#                                                          #",
            "#               ---------------------------                #",
            "#                                                          #",
            "#||||||||||||||||||||||||||||||||||||||||||||||||||||||||||#",
            "############################################################"
    })
    private String loadBalancer = "default";

    @Node(3)
    @Comment({
            "#",
            "# Provide additional metadata for the family.",
            "# Metadata provided here is non-essential, meaning that RustyConnector is capable of running without anything provided here.",
            "# Ensure that the provided metadata conforms to valid JSON syntax.",
            "#",
            "# For built-in metadata options, check the Aelysium wiki:",
            "# https://wiki.aelysium.group/rusty-connector/docs/concepts/metadata/",
            "#"
    })
    private String metadata = "{\\\"serverSoftCap\\\": 30, \\\"serverHardCap\\\": 40}";

    public ModuleBuilder<Family> builder() throws Exception {
        return new ModuleBuilder<>("ScalarFamily", "Provides stateless server connectivity between players and it's child servers. Players that join this family may be routed to any server without regard for server details.") {
            @Override
            public ScalarFamily get() {
                Gson gson = new Gson();
                JsonObject metadataJson = gson.fromJson(metadata, JsonObject.class);
                Map<String, Object> mt = new HashMap<>();
                metadataJson.entrySet().forEach(e->mt.put(e.getKey(), Packet.Parameter.fromJSON(e.getValue()).getOriginalValue()));
                
                try {
                    return new ScalarFamily(
                        id,
                        displayName.isEmpty() ? null : displayName,
                        parentFamily.isEmpty() ? null : parentFamily,
                        mt,
                        LoadBalancerConfig.New(loadBalancer)
                    );
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    public static ScalarFamilyConfig New(String familyID) throws IOException {
        Printer printer = new Printer()
                .pathReplacements(Map.of("id", familyID))
                .commentReplacements(Map.of("id", familyID));
        return DeclarativeYAML.From(ScalarFamilyConfig.class, printer);
    }
}