package group.aelysium.rustyconnector.plugin.fabric;

import group.aelysium.rustyconnector.common.haze.HazeProvider;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.common.modules.ModuleLoader;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.common.util.Parameter;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.crypt.AES;
import group.aelysium.rustyconnector.common.crypt.NanoID;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.EnglishAlphabet;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.magic_link.PacketCache;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.util.URL;
import group.aelysium.rustyconnector.plugin.common.command.CommonCommands;
import group.aelysium.rustyconnector.plugin.common.config.PrivateKeyConfig;
import group.aelysium.rustyconnector.plugin.common.config.ServerIDConfig;
import group.aelysium.rustyconnector.plugin.serverCommon.CommandRusty;
import group.aelysium.rustyconnector.plugin.serverCommon.DefaultConfig;
import group.aelysium.rustyconnector.plugin.serverCommon.ServerLang;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import group.aelysium.rustyconnector.server.ServerAdapter;
import group.aelysium.rustyconnector.server.ServerKernel;
import group.aelysium.rustyconnector.server.magic_link.WebSocketMagicLink;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.Gson;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonObject;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.text.Text;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.fabric.FabricServerCommandManager;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class FabricRustyConnector implements DedicatedServerModInitializer {
    private final ModuleLoader loader = new ModuleLoader();
    private final Gson gson = new Gson();
    private FabricServerCommandManager<CommandClient> commandManager;
    
    @Override
    public void onInitializeServer() {
        DeclarativeYAML.basePath("rustyconnector", "mods/rustyconnector");
        DeclarativeYAML.basePath("rustyconnector-modules", "rc-modules");

        this.commandManager = new FabricServerCommandManager<>(
                ExecutionCoordinator.asyncCoordinator(),
                SenderMapper.create(
                        sender -> {
                            if(sender.isExecutedByPlayer()) return new FabricClient.Player(sender);
                            if(sender.getEntity() == null && sender.getServer() != null) return new FabricClient.Console(sender);
                            return new FabricClient.Other(sender);
                        },
                        CommandClient::toSender
                )
        );
        
        AnnotationParser<CommandClient> annotationParser = new AnnotationParser<>(this.commandManager, CommandClient.class);
        annotationParser.parse(new CommonCommands());
        annotationParser.parse(new CommandRusty());

        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            s.sendMessage(Text.of("Initializing RustyConnector..."));
            
            ServerAdapter adapter = new FabricServerAdapter(s, commandManager);

            try {
                if(PrivateKeyConfig.Load().isEmpty()) {
                    adapter.log(Component.join(
                            JoinConfiguration.newlines(),
                            Component.empty(),
                            Component.empty(),
                            Component.empty(),
                            Component.empty(),
                            Component.text("Looks like I'm still waiting on a private.key from the proxy!", NamedTextColor.BLUE),
                            Component.text("You'll need to copy ", NamedTextColor.BLUE).append(Component.text("mods/rustyconnector/metadata/aes.private", NamedTextColor.YELLOW)).append(Component.text(" and paste it into this server in that same folder!", NamedTextColor.BLUE)),
                            Component.text("Both the proxy and I need to have the same aes.private!", NamedTextColor.BLUE),
                            Component.empty(),
                            Component.empty(),
                            Component.empty()
                    ));
                    return;
                }
                
                RustyConnector.registerAndIgnite(Flux.using(()->{
                    try {
                        DefaultConfig config = DefaultConfig.New();
                        
                        String family = config.family();
                        if(family.isBlank()) throw new IllegalArgumentException("Please provide a valid family name to target.");
                        if(family.length() > 16) throw new IllegalArgumentException("Family names are not allowed to be larger than 16 characters.");
                        
                        ServerIDConfig idConfig = ServerIDConfig.Read();
                        String id = (idConfig == null ? null : idConfig.id());
                        if(id == null) {
                            if (config.useUUID()) {
                                id = UUID.randomUUID().toString();
                            } else {
                                int extra = 16 - family.length();
                                NanoID nanoID = NanoID.randomNanoID(15 + extra); // 15 because there's a `-` separator between family name and nanoid
                                id = family + "-" + nanoID;
                            }
                            
                            ServerIDConfig.Load(id);
                        }
                        
                        ServerKernel kernel = new ServerKernel(
                            id,
                            new FabricServerAdapter(s, this.commandManager),
                            Path.of(DeclarativeYAML.basePath("rustyconnector")),
                            Path.of(DeclarativeYAML.basePath("rustyconnector-modules")),
                            AddressUtil.parseAddress(config.address()),
                            config.family()
                        );
                        
                        JsonObject metadataJson = gson.fromJson(config.metadata(), JsonObject.class);
                        metadataJson.entrySet().forEach(e->kernel.storeMetadata(e.getKey(), Parameter.fromJSON(e.getValue())));
                        
                        return kernel;
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }));
                
                RustyConnector.Kernel(flux->{
                    flux.metadata("name", "RCKernel");
                    flux.metadata("description", "The root kernel for RustyConnector where all additional modules build off of.");
                    
                    flux.onStart(kernel -> {
                        loader.queue(new ModuleLoader.ModuleRegistrar("LangLibrary", k->{
                            try {
                                k.registerModule(new Module.Builder<>("LangLibrary", "Provides translatable lang messages that can be replaced and repurposed.") {
                                    @Override
                                    public Module get() {
                                        LangLibrary l = new LangLibrary(new EnglishAlphabet());
                                        l.registerLangNodes(ServerLang.class);
                                        return l;
                                    }
                                });
                            } catch (Exception e) {
                                RC.Error(Error.from(e));
                            }
                        }, List.of(), List.of("nothingThisIsTheStartOfTheChainIgnoreItYouGoober")));
                        loader.queue(new ModuleLoader.ModuleRegistrar("ErrorRegistry", k->{
                            try {
                                k.registerModule(new Module.Builder<>("ErrorRegistry", "Provides error handling services.") {
                                    @Override
                                    public Module get() {
                                        return new ErrorRegistry(false, 200);
                                    }
                                });
                            } catch (Exception e) {
                                RC.Error(Error.from(e));
                            }
                        }, List.of("LangLibrary"), List.of()));
                        loader.queue(new ModuleLoader.ModuleRegistrar("EventManager", k->{
                            try {
                                k.registerModule(new Module.Builder<>("EventManager", "Provides event handling services.") {
                                    @Override
                                    public Module get() {
                                        return new EventManager();
                                    }
                                });
                            } catch (Exception e) {
                                RC.Error(Error.from(e));
                            }
                        }, List.of("ErrorRegistry"), List.of()));
                        loader.queue(new ModuleLoader.ModuleRegistrar("MagicLink", k->{
                            try {
                                k.registerModule(new Module.Builder<>("MagicLink", "Provides cross-node packet communication via WebSockets.") {
                                    @Override
                                    public Module get() {
                                        try {
                                            DefaultConfig config = DefaultConfig.New();
                                            ServerIDConfig idConfig = ServerIDConfig.Read();
                                            
                                            AES aes = PrivateKeyConfig.New().cryptor();
                                            return new WebSocketMagicLink(
                                                URL.parseURL(config.magicLink_accessEndpoint()),
                                                Packet.SourceIdentifier.server(idConfig.id()),
                                                aes,
                                                new PacketCache(100),
                                                null
                                            );
                                        } catch (Exception e) {
                                            RC.Error(Error.from(e).whileAttempting("To initialize MagicLink.").urgent(true));
                                        }
                                        return null;
                                    }
                                });
                            } catch (Exception e) {
                                RC.Error(Error.from(e));
                            }
                        }, List.of("EventManager"), List.of()));
                        loader.queue(new ModuleLoader.ModuleRegistrar("Haze", k->{
                            try {
                                k.registerModule(new Module.Builder<>("Haze", "Provides access to remote persistent storage services.") {
                                    @Override
                                    public Module get() {
                                        return new HazeProvider();
                                    }
                                });
                            } catch (Exception e) {
                                RC.Error(Error.from(e));
                            }
                        }, List.of("EventManager"), List.of()));
                        
                        loader.queueFromFolder("rc-modules");
                        
                        loader.resolveAndRegister(kernel);
                    });
                });

                RC.Lang("rustyconnector-wordmark").send(RC.Kernel().version());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(s -> {
            try {
                RustyConnector.unregister();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}