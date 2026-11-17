plugins {
    id("net.neoforged.moddev")
}

// Every Minecraft version is built from this script as the Gradle project :<minecraft> (versions/<minecraft>).
// Shared values are in the root gradle.properties; versions/<minecraft>/gradle.properties has the version's own
// (NeoForge, Java, Parchment and the optional mod APIs). Code that differs between versions uses Stonecutter
// conditions (//? if >=26.1 {); resources that differ live outside src/main/resources (see sourceSets below).

val modId = property("mod_id") as String
// gradle.properties holds the base version of the open update (mod_version=0.3.0); a release build passes the
// full version from its tag (-Prelease_version=0.3.0-beta.1), so alphas and betas need no version commit.
val releaseVersion = providers.gradleProperty("release_version").orElse(property("mod_version") as String).get()
val minecraftVersion = sc.current.version
val neoVersion = property("neo_version") as String
val javaVersion = (property("java_version") as String).toInt()
val mc26 = sc.eval(minecraftVersion, ">=26.1")

/** An optional property of this Minecraft version (null when the version does not use that mod). */
fun optional(name: String): String? = findProperty(name) as String?

version = releaseVersion
group = property("mod_group_id") as String

base {
    // The Minecraft version is part of the file name, so builds of different versions never look alike.
    archivesName = "$modId-$minecraftVersion"
}

java.toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)

sourceSets.main {
    java {
        // Optional integrations whose mod has no build for this Minecraft version stay out of it.
        if (optional("emi_version") == null) {
            exclude("dimensionalfluxminer/compat/emi/**")
        }
        if (optional("polymorph_version") == null) {
            exclude("dimensionalfluxminer/compat/polymorph/**")
        }
        if (optional("rei_version") == null) {
            exclude("dimensionalfluxminer/compat/rei/**")
        }
    }
    resources {
        // src/main/resources: every version. src/mc26/resources: 26.1.2 and 26.3 (formats of the 26.x data and
        // assets). versions/<minecraft>/src/main/resources (added by Stonecutter): this version only.
        if (mc26) {
            srcDir(rootProject.file("src/mc26/resources"))
        }
        exclude("**/*.bbmodel")
    }
}

neoForge {
    version = neoVersion

    // The access transformer is shared by every version (the default path would be versions/<minecraft>/src).
    accessTransformers.from(rootProject.file("src/main/resources/META-INF/accesstransformer.cfg"))

    optional("parchment_mappings_version")?.let { mappings ->
        parchment {
            mappingsVersion = mappings
            minecraftVersion = property("parchment_minecraft_version") as String
        }
    }

    runs {
        register("client") {
            client()
            // Each client has its own mods, settings and saves, selected by :<minecraft>:runClient.
            gameDirectory = rootProject.file("run/$minecraftVersion")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        register("server") {
            server()
            programArgument("--nogui")
            gameDirectory = rootProject.file("run/mc-$minecraftVersion/server")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        // Only Minecraft, NeoForge and this mod, as on CI. CI and releases look for "required tests passed".
        register("gameTestServer") {
            type = "gameTestServer"
            gameDirectory = rootProject.file("run/mc-$minecraftVersion/gametest")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        // Same GameTests with a copy of the optional mods in run/mc-<minecraft>/gametest-compat/mods (never bundled).
        register("gameTestCompat") {
            type = "gameTestServer"
            gameDirectory = rootProject.file("run/mc-$minecraftVersion/gametest-compat")
            systemProperty("neoforge.enabledGameTestNamespaces", modId)
        }
        configureEach {
            systemProperty("forge.logging.markers", "REGISTRIES")
            logLevel = org.slf4j.event.Level.INFO
        }
    }

    mods {
        register(modId) {
            sourceSet(sourceSets.main.get())
        }
    }
}

val localRuntime = configurations.maybeCreate("localRuntime")
configurations.runtimeClasspath { extendsFrom(localRuntime) }

repositories {
    // Optional recipe viewer integrations (compile-only APIs; nothing is bundled).
    exclusiveContent {
        forRepository { maven("https://maven.shedaniel.me/") { name = "Shedaniel" } }
        filter {
            includeGroup("me.shedaniel")
            includeGroup("me.shedaniel.cloth")
            includeGroup("dev.architectury")
        }
    }
    exclusiveContent {
        forRepository { maven("https://maven.terraformersmc.com/") { name = "TerraformersMC" } }
        filter { includeGroup("dev.emi") }
    }
    exclusiveContent {
        forRepository { maven("https://maven.blamejared.com/") { name = "BlameJared" } }
        filter { includeGroup("mezz.jei") }
    }
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
    // Optional scripting integration (compile-only API; the mod runs without it).
    exclusiveContent {
        forRepository { maven("https://maven.latvian.dev/releases") { name = "LatvianModder" } }
        filter { includeGroup("dev.latvian.mods") }
    }
}

dependencies {
    compileOnly("mezz.jei:jei-$minecraftVersion-neoforge-api:${property("jei_version")}")
    compileOnly("maven.modrinth:jade:${property("jade_version")}")
    compileOnly("maven.modrinth:ae2:${property("ae2_version")}") { isTransitive = false }
    optional("rei_version")?.let { rei ->
        compileOnly("me.shedaniel:RoughlyEnoughItems-neoforge:$rei")
        compileOnly("dev.architectury:architectury-neoforge:${property("architectury_version")}")
    }
    optional("emi_version")?.let { compileOnly("dev.emi:emi-neoforge:$it:api") }
    optional("polymorph_version")?.let { compileOnly("maven.modrinth:polymorph:$it") }
    compileOnly("dev.latvian.mods:kubejs-neoforge:${property("kubejs_version")}") { isTransitive = false }
    compileOnly("dev.latvian.mods:rhino:${property("rhino_version")}") { isTransitive = false }

    testImplementation(platform("org.junit:junit-bom:5.11.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    from(rootProject.file("THIRD_PARTY_NOTICES.md"))
}

// Stonecutter writes this version's sources before NeoForge prepares the game for them.
tasks.named("createMinecraftArtifacts") {
    dependsOn("stonecutterGenerate")
}

val replaceProperties = mapOf(
    "minecraft_version" to minecraftVersion,
    "minecraft_version_range" to property("minecraft_version_range") as String,
    "neo_version" to neoVersion,
    "loader_version_range" to property("loader_version_range") as String,
    "mod_id" to modId,
    "mod_name" to property("mod_name") as String,
    "mod_license" to property("mod_license") as String,
    "mod_version" to releaseVersion,
    "mod_logo" to property("mod_logo") as String,
)
val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from(rootProject.file("src/main/templates"))
    into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}
sourceSets.main.get().resources.srcDir(generateModMetadata)
neoForge.ideSyncTask(generateModMetadata)

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-Xmaxerrs", "10000"))
}
