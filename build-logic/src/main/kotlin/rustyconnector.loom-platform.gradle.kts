import java.util.zip.ZipFile

// Fabric packaging: Loom remap + Jar-in-Jar. No Shadow.
plugins {
    id("rustyconnector.java-conventions")
    id("fabric-loom")
}

base {
    archivesName.set("rustyconnector-${project.name}")
}

// ---------------------------------------------------------------------------
// Jar-in-Jar closure guard.
//
// Loom's `include` is NOT transitive: every runtime artifact must be listed by
// hand in the module's dependencies block. When :common / :serverCommon / core
// gain a dependency, nothing fails at build time — the Fabric jar just throws
// NoClassDefFoundError in production. This task turns that drift into a build
// failure.
//
// Model: a runtime class can come from exactly three places on Fabric —
//   1. the platform (loader / Fabric API / Minecraft's bundled libs),
//   2. an include()'d artifact (ours, or nested inside an include()'d mod:
//      cloud-fabric JiJs cloud-core, cloud-services, geantyref, ...),
//   3. nowhere → NoClassDefFoundError.
// So: walk the resolved runtime graph from every include()'d NON-mod
// dependency (projects + plain java libs), and demand each reachable module is
// itself include()'d, nested inside an include()'d mod's META-INF/jars
// (case 2 — read from the actual artifact, so it can't go stale), or
// platform-provided (case 1).
//
// Loom flattens remapped-mod dependency edges into the root of the graph, so
// "reachable from the mod" is NOT answerable from metadata — the mod's
// fabric.mod.json `jars` list inside the artifact is the only source of truth.
// ---------------------------------------------------------------------------

// Provided by the Fabric runtime — never JiJ'd, never reported missing.
val platformProvidedPrefixes = setOf(
    "net.fabricmc",          // loader, fabric-api umbrella
    "com.mojang",            // minecraft + brigadier/datafixerupper/authlib
    "org.ow2.asm",           // shipped by loader
    "com.google.guava",      // bundled with minecraft
    "com.google.code.gson",  // bundled with minecraft
    "org.apache",            // commons/log4j, bundled with minecraft
    "org.slf4j",             // bundled with minecraft
    "org.jetbrains",         // annotations: compile-time only
)

// Declared coordinates ("group:name" or "project :path") of a configuration.
fun declaredCoordinates(name: String): Provider<Set<String>> = provider {
    configurations.getByName(name).allDependencies.map { dep ->
        when (dep) {
            is ProjectDependency -> "project ${dep.path}"
            else -> "${dep.group}:${dep.name}"
        }
    }.toSet()
}

val includedCoordinates = declaredCoordinates("include")
val modCoordinates = declaredCoordinates("modImplementation")

// Artifacts of include()'d MODS (the intersection with modImplementation),
// e.g. cloud-fabric's remapped jar — scanned for nested META-INF/jars.
val includedModJars = configurations.named("runtimeClasspath").map { rc ->
    rc.incoming.artifactView {
        componentFilter { id ->
            id is ModuleComponentIdentifier && id.group.startsWith("remapped.") &&
                "${id.group.removePrefix("remapped.")}:${id.module.replace(Regex("-[0-9a-f]{8}$"), "")}"
                    .let { coord ->
                        val includes = configurations.getByName("include").allDependencies
                            .map { "${it.group}:${it.name}" }.toSet()
                        coord in includes
                    }
        }
    }.files
}

// The resolved runtime graph root. resolutionResult is metadata-only: safe to
// walk in a task action without artifact/task-dependency plumbing.
val runtimeGraphRoot = configurations.named("runtimeClasspath")
    .flatMap { it.incoming.resolutionResult.rootComponent }

// Loom remaps mod artifacts to "remapped.<group>:<name>-<8-hex-hash>"; strip
// both so coordinates match what the build script declares.
val remapHash = Regex("-[0-9a-f]{8}$")

fun coordinateOf(component: ResolvedComponentResult): String? =
    when (val id = component.id) {
        is ModuleComponentIdentifier ->
            if (id.group.startsWith("remapped."))
                "${id.group.removePrefix("remapped.")}:${id.module.replace(remapHash, "")}"
            else "${id.group}:${id.module}"
        is ProjectComponentIdentifier -> "project ${id.projectPath}"
        else -> null
    }

// BOMs (category=platform) contribute constraints, not classes — skip them.
fun isPlatform(dep: ResolvedDependencyResult): Boolean =
    dep.resolvedVariant.attributes.keySet()
        .firstOrNull { it.name == Category.CATEGORY_ATTRIBUTE.name }
        ?.let { dep.resolvedVariant.attributes.getAttribute(it).toString().contains("platform") } == true

// BFS closure over the resolved graph from the given first-level coordinates.
fun closureFrom(root: ResolvedComponentResult, seeds: Set<String>): Set<String> {
    val queue = ArrayDeque(
        root.dependencies.filterIsInstance<ResolvedDependencyResult>()
            .filterNot(::isPlatform)
            .filter { coordinateOf(it.selected)?.let { c -> c in seeds } == true }
    )
    val seen = mutableSetOf<String>()
    while (queue.isNotEmpty()) {
        val node = queue.removeFirst().selected
        val coord = coordinateOf(node) ?: continue
        if (!seen.add(coord)) continue
        queue.addAll(node.dependencies.filterIsInstance<ResolvedDependencyResult>().filterNot(::isPlatform))
    }
    return seen
}

val verifyJarInJar = tasks.register("verifyJarInJar") {
    group = "verification"
    description = "Fails if a runtime dependency is missing from Loom's include (Jar-in-Jar) set."
    // Input edge: the included-mod jars must exist (and be remapped) before we
    // can scan their fabric.mod.json for nested entries.
    inputs.files(includedModJars)
    val rootProvider = runtimeGraphRoot
    val included = includedCoordinates
    val mods = modCoordinates
    val allowed = platformProvidedPrefixes
    val modJars = includedModJars
    doLast {
        val root = rootProvider.get()
        val includeSet = included.get()
        // Everything reachable from OUR include()'d projects/libraries must be
        // accounted for. (Mods are excluded as seeds: Loom flattened their
        // edges away, and their needs are their own nested jars' business.)
        val required = closureFrom(root, includeSet - mods.get())
        // Nested-jar filenames inside every include()'d mod ("cloud-core-2.0.0.jar", ...).
        // Loom strips the fabric.mod.json `jars` FIELD when remapping for the dev
        // classpath, but the physical META-INF/jars/*.jar entries survive — and the
        // artifact we actually nest at package time is the ORIGINAL, intact one. So
        // physical entries are the reliable signal here.
        val nestedJarNames = modJars.get().flatMap { jar ->
            ZipFile(jar).use { zip ->
                zip.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith("META-INF/jars/") && it.endsWith(".jar") }
                    .map { it.substringAfterLast('/') }
                    .toList()
            }
        }.toSet()
        // "org.incendo:cloud-core" is nested-provided when some mod nests
        // "cloud-core-<version>.jar".
        fun nestedProvided(module: String): Boolean {
            val name = module.substringAfter(':')
            return nestedJarNames.any { it.startsWith("$name-") }
        }
        val missing = required
            .filterNot { it in includeSet }
            .filterNot(::nestedProvided)
            .filterNot { module -> allowed.any { module.startsWith(it) } }
            .sorted()
        if (missing.isNotEmpty()) {
            throw GradleException(
                "Runtime dependencies missing from Jar-in-Jar (`include`) — they will " +
                "NoClassDefFoundError at runtime on Fabric:\n" +
                missing.joinToString("\n") { "  - $it" } +
                "\nEither `include()` them in fabric/build.gradle.kts or, if the " +
                "platform provides them, add the group to platformProvidedPrefixes " +
                "in rustyconnector.loom-platform.gradle.kts."
            )
        }
    }
}

tasks.named("check") { dependsOn(verifyJarInJar) }
