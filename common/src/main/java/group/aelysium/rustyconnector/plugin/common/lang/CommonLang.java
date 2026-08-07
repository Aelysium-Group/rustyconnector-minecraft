package group.aelysium.rustyconnector.plugin.common.lang;

import group.aelysium.rustyconnector.RC;
import group.aelysium.rustyconnector.common.errors.Error;
import group.aelysium.rustyconnector.common.lang.Lang;
import group.aelysium.rustyconnector.common.magic_link.packet.Packet;
import group.aelysium.rustyconnector.common.modules.Module;
import group.aelysium.rustyconnector.common.util.Parameter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import org.jetbrains.annotations.NotNull;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static net.kyori.adventure.text.Component.*;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.format.NamedTextColor.*;
import static net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY;

public class CommonLang {
    public static JoinConfiguration newlines() {
        return JoinConfiguration.separator(newline());
    }

    @Lang("rustyconnector-finished")
    public static final Component finished = text("Finished!", DARK_AQUA);
    @Lang("rustyconnector-waiting")
    public static final Component waiting = text("Working on it...", BLUE);

    @Lang("rustyconnector-unknownCommand")
    public static final String unknownCommand = "Unknown command. Type \"/help\" for help.";

    @Lang("rustyconnector-noPermission")
    public static final String noPermission = "You do not have permission to do this.";

    @Lang("rustyconnector-internalError")
    public static final String internalError = "There was an internal error while trying to complete that request.";

    @Lang("rustyconnector-pluginList")
    public static Component moduleReloadList(Set<String> validModules) {
        return RC.Lang("rustyconnector-box").generate(
            join(
                    newlines(),
                    text("Please provide the name of the module you want to see details for. Valid options are:", GRAY),
                    text(String.join(", ", validModules), BLUE),
                    text("If a module has sub-modules, you can view those using using `module.submodule.submodule`.", GRAY),
                    space(),
                    text("rc module <target_module>", BLUE),
                    text("Returns details for specific modules.", DARK_GRAY),
                    space(),
                    text("rc reload <target_module>", BLUE),
                    text("Reloads a specific module.", DARK_GRAY)
            )
        );
    }

    @Lang("rustyconnector-formatInstant")
    public static String formatDate(Instant instant) {
        ZoneId zoneId = ZoneId.systemDefault();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mma");

        // Convert Instant to LocalDateTime
        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, zoneId);
        LocalDate date = dateTime.toLocalDate();

        // Get the current date
        LocalDate today = LocalDate.now(zoneId);
        LocalDate yesterday = today.minusDays(1);

        // Determine if the date is today or yesterday
        String prefix = dateFormatter.format(date);
        if (date.equals(today)) prefix = "Today at";
        if (date.equals(yesterday)) prefix = "Yesterday at";

        // Format the time with the time zone
        String formattedTime = timeFormatter.format(dateTime)+" ("+zoneId.getId()+")";

        // Combine the prefix and the formatted time
        return prefix + " " + formattedTime;
    }

    @Lang("rustyconnector-exception")
    public static Component exception(Throwable e) {
        List<Component> stackTrace = new ArrayList<>();

        AtomicReference<Throwable> current = new AtomicReference<>(e);
        while(current.get() != null) {
            if(current.get().getMessage() != null) stackTrace.add(text(current.get().getMessage(), AQUA));
            stackTrace.add(text(e.getClass().getName(), BLUE));
            stackTrace.addAll(Arrays.stream(current.get().getStackTrace()).map(s->text("        "+s.toString(), BLUE)).toList());
            current.set(current.get().getCause());
        }

        return RC.Lang("rustyconnector-box").generate(
                join(
                    newlines(),
                    stackTrace
                )
        );
    }

    @Lang("rustyconnector-error")
    public static Component error(Error error) {
        try {
            List<Component> extras = new ArrayList<>();

            ZonedDateTime zonedDateTime = error.createdAt().atZone(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDateTime = zonedDateTime.format(formatter);

            extras.add(
                    join(
                            newlines(),
                            text("RustyConnector", BLUE).append(text(" (" + error.uuid() + ") [" + formattedDateTime +"]", DARK_GRAY)),
                            text(error.message(), GRAY)
                    )
            );

            Component hintOrSolution = space();
            if (error.hint() != null)
                hintOrSolution = hintOrSolution.appendNewline().append(text("Hint: ", BLUE).append(text(error.hint(), GRAY)));
            if (error.solution() != null)
                hintOrSolution = hintOrSolution.appendNewline().append(text("Solution: ", BLUE).append(text(error.solution(), GRAY)));
            extras.add(hintOrSolution);

            if (error.throwable() != null) error.causedBy(error.throwable().getClass().getSimpleName());
            if (!error.details().isEmpty()) {
                extras.add(text("Details: ", BLUE));
                extras.add(join(
                        newlines(),
                        error.details().stream().map(e ->
                                text(" • ", DARK_GRAY)
                                        .append(text(e.key + ": ", BLUE)
                                                .append(typedValue(e.value))
                                        )
                        ).toList()
                ));
            }

            return join(
                    newlines(),
                    space(),
                    space(),
                    join(
                            newlines(),
                            extras
                    ),
                    space()
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Component.space();
    }

    @Lang("rustyconnector-bullet")
    public static Component bullet(String value) {
        return text(" • "+value, DARK_GRAY);
    }

    @Lang("rustyconnector-keyValue")
    public static Component keyValueLang(String key, Object value) {
        return RC.Lang("rustyconnector-bullet").generate(key+": ").append(RC.Lang("rustyconnector-typedValue").generate(value));
    }

    // Exists as a shorthand way to use the lang entry "rustyconnector-keyValue" cause the nature of that lang value adds a lot of bloat to other places trying to use it.
    public static Component keyValue(String key, Object value) {
        return RC.Lang("rustyconnector-keyValue").generate(key, value);
    }

    @Lang("rustyconnector-typedValue")
    public static Component typedValue(Object value) {
        if(value == null)
            return text("null", GRAY);

        Class<?> clazz = value.getClass();
        if(Long.class.isAssignableFrom(clazz) || long.class.isAssignableFrom(clazz) ||
                Double.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz) ||
                Integer.class.isAssignableFrom(clazz) || int.class.isAssignableFrom(clazz) ||
                Float.class.isAssignableFrom(clazz) || float.class.isAssignableFrom(clazz)
        )
            return text(value.toString(), YELLOW);
        if(Boolean.class.isAssignableFrom(clazz) || boolean.class.isAssignableFrom(clazz))
            return text(value.toString(), BLUE);
        if(String.class.isAssignableFrom(clazz))
            return text("\""+value+"\"", GOLD);
        if(Parameter.class.isAssignableFrom(clazz))
            return typedValue(((Parameter) value).getOriginalValue());
        if(Component.class.isAssignableFrom(clazz))
            return (Component) value;
        return text(value.toString(), GRAY);
    }

    @Lang("rustyconnector-wordmark")
    public static Component wordmark(String version) {// font: ANSI Shadow
        Component versionComponent = space();

        if(version != null && !version.isEmpty())
            versionComponent = versionComponent.append(text("Version "+version, DARK_AQUA));

        return RC.Lang("rustyconnector-box").generate(
                join(
                        newlines(),
                        text(" /███████                        /██", BLUE),
                        text("| ██__  ██                      | ██", BLUE),
                        text("| ██  \\ ██ /██   /██  /███████ /██████   /██   /██", BLUE),
                        text("| ███████/| ██  | ██ /██_____/|_  ██_/  | ██  | ██", BLUE),
                        text("| ██__  ██| ██  | ██|  ██████   | ██    | ██  | ██", BLUE),
                        text("| ██  \\ ██| ██  | ██ \\____  ██  | ██ /██| ██  | ██", BLUE),
                        text("| ██  | ██|  ██████/ /███████/  |  ████/|  ███████", BLUE),
                        text("|__/  |__/ \\______/ |_______/    \\___/   \\____  ██", BLUE),
                        text("                                         /██  | ██  ", BLUE).append(versionComponent),
                        text("                                        |  ██████/", BLUE),
                        text("  /██████                                \\______/             /██", BLUE),
                        text(" /██__  ██                                                    | ██", BLUE),
                        text("| ██  \\__/  /██████  /███████  /███████   /██████   /███████ /██████    /██████   /██████", BLUE),
                        text("| ██       /██__  ██| ██__  ██| ██__  ██ /██__  ██ /██_____/|_  ██_/   /██__  ██ /██__  ██", BLUE),
                        text("| ██      | ██  \\ ██| ██  \\ ██| ██  \\ ██| ████████| ██        | ██    | ██  \\ ██| ██  \\__/", BLUE),
                        text("| ██    ██| ██  | ██| ██  | ██| ██  | ██| ██_____/| ██        | ██ /██| ██  | ██| ██", BLUE),
                        text("|  ██████/|  ██████/| ██  | ██| ██  | ██|  ███████|  ███████  |  ████/|  ██████/| ██", BLUE),
                        text(" \\______/  \\______/ |__/  |__/|__/  |__/ \\_______/ \\_______/   \\___/   \\______/ |__/", BLUE),
                        space(),
                        space(),
                        text("Developed by Aelysium | Juice", DARK_AQUA),
                        text("Use: `/rc` to get started", DARK_AQUA)
                )
        );
    };

    @Lang("rustyconnector-box")
    public static Component box(Component component) {
        return join(
                newlines(),
                empty(),
                empty(),
                component,
                empty()
        );
    }

    @Lang("rustyconnector-headerBox")
    public static Component headerBox(@NotNull String header, @NotNull Component content) {
        return RC.Lang("rustyconnector-box").generate(
                join(
                        newlines(),
                        RC.Lang().asciiAlphabet().generate(header, BLUE),
                        empty(),
                        content
                )
        );
    }

    @Lang("rustyconnector-packet")
    public static Component message(Packet packet) {
        return join(
                JoinConfiguration.separator(empty()),
                (
                    packet.isLocal() ?
                            text("<<<", DARK_BLUE) :
                            text(">>>", DARK_GREEN)
                ),
                space(),
                text("[", DARK_GRAY),
                RC.Lang("rustyconnector-formatInstant").generate(packet.created()).color(YELLOW),
                text("]", DARK_GRAY),
                text("[", DARK_GRAY),
                text(packet.local().replyEndpoint().orElseThrow().toString(), GRAY),
                space(),
                text(packet.type().toString(), GRAY),
                text("]: ", DARK_GRAY),
                packet.successful() ? text("SUCCESS", GREEN) : text("ERROR", RED)
        );
    }

    @Lang("rustyconnector-packetDetails")
    public static Component messageDetails(Packet packet) {
        String serverDisplayName = null;
        try {
            serverDisplayName = RC.P.Server(packet.local().id()).orElseThrow().displayName();
        } catch (Exception ignore) {}
        return RC.Lang("rustyconnector-box").generate(
                join(
                        newlines(),
                        text("Details:", DARK_GRAY),
                        keyValue("ID", packet.local().replyEndpoint().orElseThrow().toString()),
                        keyValue("Type", packet.type().toString()),
                        keyValue("Direction", packet.isLocal() ? "Outgoing" : "Incoming"),
                        keyValue(packet.isLocal() ? "Created" : "Received", RC.Lang("rustyconnector-formatInstant").generate(packet.created())),
                        keyValue("Version", packet.messageVersion()),
                        keyValue("Status", packet.successful() ? text("SUCCESS", GREEN) : text("ERROR", RED)),
                        keyValue("Reason", packet.statusMessage()),
                        keyValue("Responding To", packet.replying() ? text("Packet "+packet.remote().replyEndpoint().orElseThrow(), DARK_GRAY) : "Nothing"),
                        keyValue("Sender", join(
                                JoinConfiguration.spaces(),
                                text(packet.local().origin().name(), DARK_GRAY),
                                    text(packet.local().id(), DARK_GRAY),
                                serverDisplayName == null ? empty() : text("("+serverDisplayName+")", DARK_GRAY)
                        )),
                        empty(),
                        text("Properties:", DARK_GRAY),
                        join(
                                newlines(),
                                packet.parameters().entrySet().stream().map(e->keyValue(e.getKey(), e.getValue().getOriginalValue())).toList()
                        ),
                        empty(),
                        text("Raw Packet:", DARK_GRAY),
                        text(packet.toString(), DARK_GRAY)
                )
        );
    }

    @Lang("rustyconnector-packets")
    public static Component messages(List<Packet> packets) {

        return join(
                newlines(),
                space(),
                space(),
                RC.Lang().asciiAlphabet().generate("Packets", BLUE),
                space(),
                packets.isEmpty() ?
                    text("There are no packets to show.", DARK_GRAY) :
                    join(
                            newlines(),
                            packets.stream().map(packet -> RC.Lang("rustyconnector-packet").generate(packet)).toList()
                    )
        );
    };

    @Lang("rustyconnector-sendUsage")
    public static Component sendUsage() {
        return RC.Lang("rustyconnector-box").generate(
                join(
                        newlines(),
                        text("rc send <username | uuid> <target> [flags]", BLUE),
                        text("Sends the user to the specific family!", DARK_GRAY),
                        empty(),
                        text("Extra Properties:", DARK_GRAY),
                        RC.Lang("rustyconnector-bullet").generate("-f | Marks the target as a family."),
                        RC.Lang("rustyconnector-bullet").generate("-s | Marks the target as a server."),
                        RC.Lang("rustyconnector-bullet").generate("-i | Uses minimal connection power. (Server softCap will be enforced)"),
                        RC.Lang("rustyconnector-bullet").generate("-o | Uses moderate connection power. (Server softCap will be ignored, hardCap will be enforced)"),
                        RC.Lang("rustyconnector-bullet").generate("-a | Uses aggressive connection power. (All caps will be ignored)")
                )
        );
    }

    @Lang("rustyconnector-noSendTarget")
    public static Component noSendTarget(String target) {
        return text("No server or family exists with the identifier `"+target+"`. Servers must be targeted using the server uuid and families must be targeted using the family id.", BLUE);
    }

    @Lang("rustyconnector-pluginAlreadyStarted")
    public static Component pluginAlreadyStarted() {
        return text("This plugin is already running!", BLUE);
    }

    @Lang("rustyconnector-pluginAlreadyStopped")
    public static Component pluginAlreadyStopped() {
        return text("This plugin is already stopped!", BLUE);
    }

    @Lang("rustyconnector-details")
    public static Component details(String name, String description, Optional<Module> plugin) {
        if(name == null) throw new IllegalArgumentException("`rustyconnector-details` requires that `name` and `description` are not null.");
        if(description == null) throw new IllegalArgumentException("`rustyconnector-details` requires that `name` and `description` are not null.");

        Component details = null;
        try {
             details = plugin.orElseThrow().details();
        } catch(Exception ignore) {}
        
        return join(
                newlines(),
                space(),
                space(),
                RC.Lang().asciiAlphabet().generate(name, BLUE),
                text(description, GRAY),
                space(),
                (
                    plugin.isEmpty() ?
                        text("⬤", RED).append(text(" Stopped", GRAY))
                    :
                    details == null ?
                        text("There are no other details to show.", GRAY)
                    :
                        join(
                                newlines(),
                                text("Details:", DARK_GRAY),
                                details
                        )
                ),
                space()
        );
    }
}
