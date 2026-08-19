package group.aelysium.rustyconnector.plugin.velocity;

import com.google.inject.Inject;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.PluginContainer;
import com.velocitypowered.api.plugin.PluginDescription;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ConsoleCommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerInfo;

import group.aelysium.rustyconnector.common.haze.HazeProvider;
import group.aelysium.rustyconnector.common.util.CommandClient;
import group.aelysium.rustyconnector.common.util.Parameter;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.scalar_family.ScalarFamily;
import group.aelysium.rustyconnector.proxy.player.OnlinePlayersRegistry;
import group.aelysium.rustyconnector.proxy.player.PersistentPlayerRegistry;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.Gson;
import group.aelysium.rustyconnector.shaded.com.google.code.gson.gson.JsonObject;
import group.aelysium.rustyconnector.shaded.group.aelysium.declarative_yaml.DeclarativeYAML;
import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.RustyConnector;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.errors.ErrorRegistry;
import group.aelysium.rustyconnector.common.events.EventManager;
import group.aelysium.rustyconnector.common.lang.EnglishAlphabet;
import group.aelysium.rustyconnector.common.lang.LangLibrary;
import group.aelysium.rustyconnector.common.modules.Module.Builder;
import group.aelysium.rustyconnector.common.modules.ModuleLoader;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.plugin.common.command.CommonCommands;
import group.aelysium.rustyconnector.plugin.common.config.ServerIDConfig;
import group.aelysium.rustyconnector.plugin.velocity.commands.CommandRusty;
import group.aelysium.rustyconnector.plugin.velocity.commands.CommandServer;
import group.aelysium.rustyconnector.plugin.velocity.config.*;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.rc.OnFamilyLifecycle;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.rc.OnServerRegister;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.rc.OnServerTimeout;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.rc.OnServerUnregister;
import group.aelysium.rustyconnector.plugin.velocity.event_handlers.velocity.*;
import group.aelysium.rustyconnector.plugin.velocity.lang.VelocityLang;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.family.FamilyRegistry;
import group.aelysium.rustyconnector.proxy.family.load_balancing.*;
import group.aelysium.rustyconnector.proxy.util.LiquidTimestamp;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import org.bstats.velocity.Metrics;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.SenderMapper;
import org.incendo.cloud.annotations.AnnotationParser;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.velocity.VelocityCommandManager;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class VelocityRustyConnector implements PluginContainer {
    private final ExecutorService commandExecutor = Executors.newCachedThreadPool();
    private final Metrics.Factory metricsFactory;
    private final PluginLogger logger;
    private final ProxyServer server;
    private final Path dataFolder;
    private final AnnotationParser<CommandClient> annotationParser;
    private final CommandManager<CommandClient> commandManager;
    private final ModuleLoader loader = new ModuleLoader();

    @Inject
    public VelocityRustyConnector(ProxyServer server, Logger logger, @DataDirectory Path dataFolder, Metrics.Factory metricsFactory) {
        DeclarativeYAML.basePath("rustyconnector", "plugins/rustyconnector");
        DeclarativeYAML.basePath("rustyconnector-modules", "rc-modules");
        this.logger = new PluginLogger(logger, server);
        this.server = server;
        this.dataFolder = dataFolder;
        this.metricsFactory = metricsFactory;

        this.commandManager = new VelocityCommandManager<>(
            this,
            server,
            ExecutionCoordinator.asyncCoordinator(),
            SenderMapper.create(
                sender -> {
                    if(sender instanceof ConsoleCommandSource console) return new VelocityClient.Console(console);
                    if(sender instanceof Player player) return new VelocityClient.Player(player);
                    return new VelocityClient.Other(sender);
                },
                CommandClient::toSender
            )
        );
        this.annotationParser = new AnnotationParser<>(
            this.commandManager,
            CommandClient.class
        );

        this.server.getCommandManager().unregister("server");
        this.annotationParser.parse(new CommonCommands());
        this.annotationParser.parse(new CommandRusty());
        this.annotationParser.parse(new CommandServer());
    }

    @Subscribe
    public void onLoad(ProxyInitializeEvent event) throws Exception {
        this.logger.log("Initializing RustyConnector...");

        if(!this.server.getConfiguration().isOnlineMode())
            logger.log("Offline mode detected");

        try {
            metricsFactory.make(this, 17972);
            this.logger.log("Registered to bstats!");
        } catch (Exception e) {
            e.printStackTrace();
            this.logger.log("Failed to register to bstats!");
        }

        try {
            this.logger.log("Building configuration...");
//            {
//                GitOpsConfig config = GitOpsConfig.New();
//                if(config != null) DeclarativeYAML.registerRepository("rustyconnector", config.config());
//            }

            RustyConnector.registerAndIgnite(Flux.using(()->{
                try {
                    DefaultConfig config = DefaultConfig.New();
                    Gson gson = new Gson();
                    JsonObject metadataJson = gson.fromJson(config.metadata, JsonObject.class);
                    ProxyKernel kernel = new ProxyKernel(
                        ServerIDConfig.Load(UUID.randomUUID().toString()).id(),
                        new VelocityProxyAdapter(server, logger, this.commandManager),
                        this.dataFolder,
                        this.dataFolder.resolve("../../rc-modules").normalize()
                    );
                    
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
                                    l.registerLangNodes(VelocityLang.class);
                                    return l;
                                }
                            });
                        } catch (Exception e) {
                            RC.Error(Error.from(e));
                        }
                    }, List.of(), List.of("nothingThisIsTheStartOfTheChainIgnoreItYouGoober")));
                    loader.queue(new ModuleLoader.ModuleRegistrar("ErrorRegistry", k->{
                        try {
                            k.registerModule(new Builder<>("ErrorRegistry", "Provides error handling services.") {
                                @Override
                                public Module get() {
                                    return new ErrorRegistry(false, 200);
                                }
                            });
                        } catch (Exception e) {
                            RC.Error(Error.from(e));
                        }
                    }, List.of("LangLibrary"), List.of()));
                    loader.queue(new ModuleLoader.ModuleRegistrar("PlayerRegistry", k->{
                        try {
                            k.registerModule(new Module.Builder<>("PlayerRegistry", "Provides uuid-username mappings for players connected to the network.") {
                                @Override
                                public Module get() {
                                    try {
                                        DefaultConfig config = DefaultConfig.New();
                                        return new PersistentPlayerRegistry(config.persistPlayerMappings, LiquidTimestamp.from(10, TimeUnit.MINUTES));
                                    } catch (Exception ignore) {}
                                    return new OnlinePlayersRegistry();
                                }
                            });
                        } catch (Exception e) {
                            RC.Error(Error.from(e));
                        }
                    }, List.of("ErrorRegistry"), List.of("Haze")));
                    loader.queue(new ModuleLoader.ModuleRegistrar("LoadBalancerRegistry", k->{
                        try {
                            k.registerModule(new Builder<>("LoadBalancerRegistry", "Provides access to available LoadBalancer algorithms.") {
                                @Override
                                public LoadBalancerRegistry get() {
                                    LoadBalancerRegistry registry = new LoadBalancerRegistry(name -> {
                                        try {
                                            return LoadBalancerConfig.New(name);
                                        } catch (Exception e) {
                                            RC.Error(Error.from(e).whileAttempting("To build a new load balancer using config "+name+". Provided a ROUND_ROBIN as a fallback.").urgent(true));
                                        }
                                        return new Builder<>("LoadBalancer", "Provides load balancing using the RoundRobin sorting algorithm.") {
                                            @Override
                                            public LoadBalancer get() {
                                                return new RoundRobin(false, false, 0, Map.of());
                                            }
                                        };
                                    });
                                    
                                    try {
                                        registry.register("ROUND_ROBIN", config -> new Builder<>("LoadBalancer", "Provides load balancing using the RoundRobin sorting algorithm.") {
                                            @Override
                                            public LoadBalancer get() {
                                                System.out.println("Build " + this.name);
                                                return new RoundRobin(
                                                    config.weighted(),
                                                    config.persistence(),
                                                    config.attempts(),
                                                    config.metadata()
                                                );
                                            }
                                        });
                                    } catch (Exception e) {
                                        RC.Error(Error.from(e).whileAttempting("To build the ROUND_ROBIN load balancer algorithm"));
                                    }
                                    try {
                                        registry.register("LEAST_CONNECTION", config -> new Builder<>("LoadBalancer", "Provides load balancing using the LeastConnection sorting algorithm.") {
                                            @Override
                                            public LoadBalancer get() {
                                                System.out.println("Build " + this.name);
                                                return new LeastConnection(
                                                    config.weighted(),
                                                    config.persistence(),
                                                    config.attempts(),
                                                    config.rebalance() == null ? LiquidTimestamp.from(15, TimeUnit.SECONDS) : config.rebalance(),
                                                    config.metadata()
                                                );
                                            }
                                        });
                                    } catch (Exception e) {
                                        RC.Error(Error.from(e).whileAttempting("To build the LEAST_CONNECTION load balancer algorithm"));
                                    }
                                    try {
                                        registry.register("MOST_CONNECTION", config -> new Builder<>("LoadBalancer", "Provides load balancing using the MostConnection sorting algorithm.") {
                                            @Override
                                            public LoadBalancer get() {
                                                System.out.println("Build " + this.name);
                                                return new MostConnection(
                                                    config.weighted(),
                                                    config.persistence(),
                                                    config.attempts(),
                                                    config.rebalance() == null ? LiquidTimestamp.from(15, TimeUnit.SECONDS) : config.rebalance(),
                                                    config.metadata()
                                                );
                                            }
                                        });
                                    } catch (Exception e) {
                                        RC.Error(Error.from(e).whileAttempting("To build the MOST_CONNECTION load balancer algorithm"));
                                    }
                                    return registry;
                                }
                            });
                        } catch (Exception e) {
                            RC.Error(Error.from(e));
                        }
                    }, List.of("ErrorRegistry"), List.of()));
                    
                    loader.queue(new ModuleLoader.ModuleRegistrar("FamilyRegistry", k->{
                        try {
                            k.registerModule(new Module.Builder<>("FamilyRegistry", "Provides itemized access for all families available on the RustyConnector kernel.") {
                                @Override
                                public Module get() {
                                    FamilyRegistry f = new FamilyRegistry();
                                    try {
                                        DefaultConfig config = DefaultConfig.New();
                                        f.rootFamily(config.rootFamily);
                                        
                                        File directory = new File(DeclarativeYAML.basePath("rustyconnector")+"/scalar_families");
                                        if(!directory.exists()) directory.mkdirs();
                                        
                                        {
                                            File[] files = directory.listFiles();
                                            if (files == null || files.length == 0)
                                                ScalarFamilyConfig.New("lobby");
                                        }
                                        
                                        File[] files = directory.listFiles();
                                        if (files == null) return f;
                                        if (files.length == 0) return f;
                                        
                                        for (File file : files) {
                                            if(!(file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"))) continue;
                                            int extensionIndex = file.getName().lastIndexOf(".");
                                            String name = file.getName().substring(0, extensionIndex);
                                            f.register(name, new Module.Builder<>(name, "Provides stateless server connectivity between players and it's child servers. Players that join this family may be routed to any server without regard for server details.") {
                                                @Override
                                                public Family get() {
                                                    try {
                                                        ScalarFamilyConfig scalarFamilyConfig = ScalarFamilyConfig.New(name);
                                                        
                                                        Gson gson = new Gson();
                                                        JsonObject metadataJson = gson.fromJson(scalarFamilyConfig.metadata, JsonObject.class);
                                                        Map<String, Object> mt = new HashMap<>();
                                                        metadataJson.entrySet().forEach(e->mt.put(e.getKey(), Parameter.fromJSON(e.getValue()).getOriginalValue()));
                                                        
                                                        LoadBalancerRegistry registry = (LoadBalancerRegistry) k.fetchModule("LoadBalancerRegistry").orElse(null);
                                                        if(registry == null) {
                                                            RC.Error(Error.from("Unable to register "+scalarFamilyConfig.id+" because LoadBalancerRegistry is null.").urgent(true));
                                                            return null;
                                                        }

                                                        // Create a Velocity Virtual Server for the family
                                                        ServerInfo familyVirtualServer = new ServerInfo(
                                                            scalarFamilyConfig.id, 
                                                            new InetSocketAddress("127.0.0.1", 0)
                                                        );
                                                        server.registerServer(familyVirtualServer);
                                                        
                                                        return new ScalarFamily(
                                                            scalarFamilyConfig.id,
                                                            scalarFamilyConfig.displayName.isEmpty() ? null : scalarFamilyConfig.displayName,
                                                            scalarFamilyConfig.parentFamily.isEmpty() ? null : scalarFamilyConfig.parentFamily,
                                                            mt,
                                                            registry.generate(scalarFamilyConfig.loadBalancer)
                                                        );
                                                    } catch (Exception e) {
                                                        RC.Error(Error.from(e).whileAttempting("To generate the scalar family "+name));
                                                    }
                                                    return null;
                                                }
                                            });
                                        }
                                    } catch (Exception e) {
                                        RC.Error(Error.from(e).whileAttempting("To boot up the FamilyRegistry."));
                                    }
                                    return f;
                                }
                            });
                        } catch (Exception e) {
                            RC.Error(Error.from(e));
                        }
                    }, List.of("LoadBalancerRegistry"), List.of()));
                    
                    loader.queue(new ModuleLoader.ModuleRegistrar("EventManager", k->{
                        try {
                            k.registerModule(new Module.Builder<>("EventManager", "Provides event handling services.") {
                                @Override
                                public Module get() {
                                    EventManager e = new EventManager();
                                    e.listen(OnServerRegister.class);
                                    e.listen(OnServerUnregister.class);
                                    e.listen(OnServerTimeout.class);
                                    e.listen(new OnFamilyLifecycle(VelocityRustyConnector.this.server));
                                    return e;
                                }
                            });
                        } catch (Exception e) {
                            RC.Error(Error.from(e));
                        }
                    }, List.of("FamilyRegistry"), List.of()));

                    loader.queue(new ModuleLoader.ModuleRegistrar("MagicLink", k->{
                        try {
                            k.registerModule(new Module.Builder<>("MagicLink", "Provides cross-node packet communication via WebSockets.") {
                                @Override
                                public Module get() {
                                    try {
                                        return MagicLinkConfig.New().build();
                                    } catch (Exception e) {
                                        throw new RuntimeException(e);
                                    }
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

        if(!this.server.getConfiguration().isOnlineMode())
            RC.P.Lang().lang("rustyconnector-offlineMode").send();

        // Velocity requires that at least one server is always defined in velocity.toml
        if(this.server.getConfiguration().getServers().size() > 1)
            RC.P.Lang().lang("rustyconnector-hybrid").send();

        this.server.getEventManager().register(this, new OnPlayerChangeServer());
        this.server.getEventManager().register(this, new OnPlayerChooseInitialServer());
        this.server.getEventManager().register(this, new OnPlayerDisconnect());
        this.server.getEventManager().register(this, new OnPlayerKicked(this.server));
        this.server.getEventManager().register(this, new OnPlayerPreConnectServer(this.server));
    }

    @Subscribe
    public void onUnload(ProxyShutdownEvent event) {
        try {
            RustyConnector.unregister();
            loader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public PluginDescription getDescription() {
        return () -> "rustyconnector-velocity";
    }

    @Override
    public Optional<?> getInstance() {
        return Optional.of(this);
    }

    @Override
    public ExecutorService getExecutorService() {
        return this.commandExecutor;
    }
}
