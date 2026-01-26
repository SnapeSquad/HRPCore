plugins {
    java
    `java-library`
}

group = "ru.hrp"
version = project.property("pluginVersion") as String

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(project.property("javaVersion") as String))
    }
}

repositories {
    mavenCentral()
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "jitpack"
        url = uri("https://jitpack.io")
    }
    maven {
        name = "placeholderapi"
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:${project.property("paperApiVersion")}")
    compileOnly("com.github.MilkBowl:VaultAPI:${project.property("vaultApiVersion")}")
    compileOnly("me.clip:placeholderapi:${project.property("placeholderApiVersion")}")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    processResources {
        val props = mapOf(
            "version" to version,
            "apiVersion" to (project.property("paperApiVersion") as String).split("-")[0]
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
