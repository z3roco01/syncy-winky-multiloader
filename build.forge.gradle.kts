plugins {
    id("net.neoforged.moddev.legacyforge") version "2.0.147"
}

val minecraft = sc.current.version
val mcVersion = sc.current.project.substringBeforeLast('-')

tasks.named<ProcessResources>("processResources") {
    fun prop(name: String) = project.property(name) as String

    val props = HashMap<String, String>().apply {
        this["version"] = prop("mod.version") + "+" + mcVersion
        this["minecraft"] = mcVersion
    }

    filesMatching(listOf("neoforge.mod.json", "META-INF/neoforge.mods.toml", "META-INF/mods.toml")) {
        expand(props)
    }
}

version = "${property("mod.version")}+${mcVersion}-forge"
base.archivesName = "${property("mod.id") as String}-forge"

repositories {
    mavenLocal()
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    url = uri("https://cursemaven.com")
                }
            }
            filter {
                includeGroup ("curse.maven")
            }
        }
    }
}

legacyForge {
    version = property("deps.forge") as String
    validateAccessTransformers = true

    if (hasProperty("deps.parchment")) parchment {
        val (mc, ver) = (property("deps.parchment") as String).split(':')
        mappingsVersion = ver
        minecraftVersion = mc
    }

    runs {
        register("client") {
            gameDirectory = file("run/")
            client()
        }
        register("server") {
            gameDirectory = file("run/")
            server()
        }
    }

    mods {
        register(property("mod.id") as String) {
            sourceSet(sourceSets["main"])
        }
    }
    sourceSets["main"].resources.srcDir("src/main/generated")
}


dependencies {
    modCompileOnly("io.github.llamalad7:mixinextras-common:0.5.5")
    implementation("io.github.llamalad7:mixinextras-forge:0.5.5")
    jarJar("io.github.llamalad7:mixinextras-forge:0.5.5")
    annotationProcessor("org.spongepowered:mixin:0.8.7:processor")
    // Mixin Constraints - embedded
    implementation("com.moulberry:mixinconstraints:1.0.9")
    jarJar("com.moulberry:mixinconstraints:1.0.9")

}


mixin {
    add(sourceSets["main"], "syncywinky.refmap.json")
    config("syncywinky.mixins.json")
}

dependencies {
}

tasks.named<Jar>("jar") {
    manifest {
        attributes(
            "MixinConfigs" to "syncywinky.mixins.json"
        )
    }
}

stonecutter {
    replacements.string {
        direction = eval(current.version, ">1.21.10")
        replace("ResourceLocation", "Identifier")
    }
    replacements.string {
        direction = eval(current.version, ">26")
        replace("GuiGraphics", "GuiGraphicsExtractor")
    }
    replacements.string {
        direction = eval(current.version, ">26")
        replace("guiGraphics.drawString", "guiGraphics.text")
    }
}

val requiredJava = when {
    sc.current.parsed >= "26.1" -> JavaVersion.VERSION_25
    sc.current.parsed >= "1.20.5" -> JavaVersion.VERSION_21
    sc.current.parsed >= "1.18" -> JavaVersion.VERSION_17
    sc.current.parsed >= "1.17" -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

tasks {
    processResources {
        exclude("**/fabric.mod.json", "**/*.accesswidener", "**/neoforge.mods.toml")

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }

        fun MutableMap<String, String>.register(key: String, property: String) {
            val value: String = sc.properties[property]
            inputs.property(key, value)
            set(key, value)
        }

        val props = buildMap {
            register("id", "mod.id")
            register("name", "mod.name")
            register("version", "mod.version")
            register("minecraft", "mod.mc_compat")
        }

        filesMatching("META-INF/mods.toml") { expand(props) }

        exclude("fabric.mod.json", "*.ct", "*.classtweaker", "META-INF/neoforge.mods.toml")
    }

    named("createMinecraftArtifacts") {
        dependsOn("stonecutterGenerate")
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}


java {
    withSourcesJar()
    sourceCompatibility = requiredJava
    targetCompatibility = requiredJava
}