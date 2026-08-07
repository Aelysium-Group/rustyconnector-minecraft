package group.aelysium.rustyconnector.plugin.velocity.lang;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.lang.Lang;
import group.aelysium.rustyconnector.plugin.common.lang.CommonLang;
import group.aelysium.rustyconnector.proxy.ProxyKernel;
import group.aelysium.rustyconnector.proxy.family.Family;
import group.aelysium.rustyconnector.proxy.family.FamilyRegistry;
import group.aelysium.rustyconnector.proxy.family.Server;
import group.aelysium.rustyconnector.proxy.family.load_balancing.LoadBalancer;
import group.aelysium.rustyconnector.proxy.family.scalar_family.ScalarFamily;
import group.aelysium.rustyconnector.proxy.player.PlayerRegistry;
import group.aelysium.rustyconnector.proxy.util.AddressUtil;
import group.aelysium.rustyconnector.shaded.group.aelysium.ara.Flux;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.Component.space;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;

public class VelocityLang extends CommonLang {
    public static String alreadyConnected() {
        return "You're already connected to this server.";
    }

    @Lang("rustyconnector-kernelDetails")
    public static Component usage(ProxyKernel kernel) {
        return RC.Lang("rustyconnector-headerBox").generate(
            "proxy",
            join(
                newlines(),
                text("Details:", DARK_GRAY),
                kernel.details(),
                empty(),
                text("Commands:", DARK_GRAY),
                text("rc families", BLUE),
                text("View all families.", DARK_GRAY),
                empty(),
                text("rc servers", BLUE),
                text("View all servers.", DARK_GRAY),
                empty(),
                text("rc send", BLUE),
                text("Send a player a family or server.", DARK_GRAY),
                empty(),
                text("rc packets", BLUE),
                text("Access recently sent MagicLink packets.", DARK_GRAY),
                empty(),
                text("rc reload", BLUE),
                text("Reload RustyConnector.", DARK_GRAY),
                empty(),
                text("rc plugins", BLUE),
                text("Get details for RustyConnector modules.", DARK_GRAY),
                empty(),
                text("rc errors", BLUE),
                text("Fetches the recent errors thrown by RustyConnector.", DARK_GRAY)
            )
        );
    }

    @Lang("rustyconnector-offlineMode")
    public static Component offlineMode() {
        return RC.Lang("rustyconnector-box").generate(Component.text("Your network is running in offline mode! YOU WILL RECEIVE NO SUPPORT AT ALL WITH RUSTYCONNECTOR!", RED));
    }

    @Lang("rustyconnector-serverRegister")
    public static Component serverRegister(Server.Configuration server, Family family) {
        Optional<String> displayName = Optional.ofNullable((String) server.metadata().get("displayName"));
        if(family == null)
            return join(
                JoinConfiguration.separator(empty()),
                text("[", DARK_GRAY),
                text(displayName.orElse(server.id()), BLUE),
                space(),
                text(AddressUtil.addressToString(server.address()), YELLOW),
                text("]", DARK_GRAY),
                space(),
                text("Registered", GREEN)
            );
        else
            return join(
                JoinConfiguration.separator(empty()),
                text("[", DARK_GRAY),
                text(displayName.orElse(server.id()), BLUE),
                space(),
                text(AddressUtil.addressToString(server.address()), YELLOW),
                text("]", DARK_GRAY),
                space(),
                text("->", GREEN),
                space(),
                text(family.id(), GRAY)
            );
    }

    @Lang("rustyconnector-serverUnregister")
    public static Component serverUnregister(Server server, Family family) {
        return join(
                JoinConfiguration.separator(empty()),
                text("[", DARK_GRAY),
                text(server.id(), BLUE),
                space(),
                text(AddressUtil.addressToString(server.address()), YELLOW),
                text("]", DARK_GRAY),
                space(),
                text("-x", RED),
                space(),
                text(family.id(), GRAY)
        );
    }

    @Lang("rustyconnector-hybrid")
    public static Component hybrid() {
        return RC.Lang("rustyconnector-box").generate(
            Component.join(
                JoinConfiguration.newlines(),
                Component.text("Your network is identified as having multiple, pre-defined, non-RC servers in it!", NamedTextColor.RED),
                Component.text("Please note that you will receive no help in regards to making RC work with predefined servers!", NamedTextColor.RED)
            )
        );
    }

    @Lang("rustyconnector-servers")
    public static Component servers() {
        return RC.Lang("rustyconnector-headerBox").generate(
                "servers",
                (
                    RC.P.Servers().isEmpty() ?
                        text("There are no servers to show.", DARK_GRAY)
                        :
                        join(
                            newlines(),
                            RC.P.Servers().stream().map(s->{
                                String familyName = null;
                                try {
                                    familyName = s.family().orElseThrow().id();
                                } catch (Exception ignore) {}

                                return join(
                                        JoinConfiguration.separator(empty()),
                                        text("[", DARK_GRAY),
                                        text(s.id(), BLUE),
                                        space(),
                                        text(AddressUtil.addressToString(s.address()), YELLOW),
                                        text("]:", DARK_GRAY),
                                        space(),
                                        (
                                                s.displayName() == null ? empty() :
                                                text(Objects.requireNonNull(s.displayName()), AQUA)
                                                .append(space())
                                        ),
                                        text("(Players: ", DARK_GRAY),
                                        text(s.players(), DARK_AQUA),
                                        text(")", DARK_GRAY),
                                        space(),
                                        (
                                                familyName == null ? text("This server has no family", DARK_GRAY) : join(
                                                        JoinConfiguration.separator(empty()),
                                                        text("(Family: ", DARK_GRAY),
                                                        text(familyName, DARK_AQUA),
                                                        text(")", DARK_GRAY)
                                                )
                                        )
                                );
                            }).toList()
                        )
                )
        );
    }

    @Lang("rustyconnector-serverDetails")
    public static Component serverDetails(Server server) {
        boolean missing = server.family().isEmpty();
        Family family = null;
        try {
            family = server.family().orElseThrow();
        } catch (Exception ignore) {}
        boolean locked = false;
        try {
            if(family == null) throw new NullPointerException();
            locked = family.isLocked(server);
        } catch (Exception ignore) {}

        return RC.Lang("rustyconnector-headerBox").generate(
                "server",
                join(
                        newlines(),
                        text("Details:", DARK_GRAY),
                        keyValue("ID",           server.id()),
                        keyValue("Address",        AddressUtil.addressToString(server.address())),
                        keyValue("Family",         (family == null ? (missing ? "Unavailable" : "None") : family.id())),
                        keyValue("Online Players", server.players()),
                        keyValue("Weight",         server.weight()),
                        keyValue("Locked",         locked),
                        keyValue("Stale",          server.stale()),
                        empty(),
                        text("Extra Properties:", DARK_GRAY),
                        (
                            server.metadata().isEmpty() ?
                                text("There are no properties to show.", DARK_GRAY)
                            :
                                join(
                                        newlines(),
                                        server.metadata().entrySet().stream().map(e -> keyValue(e.getKey(), e.getValue())).toList()
                                )
                        )

                )
        );
    };

    @Lang("velocity-alreadyInFamily")
    public static Component velocityAlreadyInFamily() {
        return text("You are already connected to this server!", RED);
    }

    @Lang("velocity-noServer")
    public static Component velocityNoServer() {
        return text("You are not connected to any server!", RED);
    }

    @Lang("velocity-serverUsage")
    public static Component velocityServer(Server server) {
        List<String> families = new ArrayList<>();
        RC.P.Families().modules().values().forEach(flux -> flux.ifPresent(f -> families.add(f.id())));
        try {
            Family family = server.family().orElseThrow();

            return join(
                    newlines(),
                    text("You are currently on server: "+family.id()),
                    text("Available servers: "+families),
                    text("Type /server <server_name> to switch to another server.")
            );
        } catch (Exception e) {
            return join(
                    newlines(),
                    text("You are currently on an unknown server."),
                    text("Available servers: "+String.join(", ", families)),
                    text("Type /server <server_name> to switch to another server.")
            );
        }
    }
}
