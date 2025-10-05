plugins {
    java
}

group = "com.lootcrates"
version = "1.2.0"
description = "Crates + Keys with block binding, preview and rolling GUI"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    // JitPack nur für wirklich verfügbare Artefakte
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.extendedclip.com/content/repositories/public/")
}

dependencies {
    // Spigot/Paper API
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    // Vault API
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    // SpecialItems (falls als JAR vorhanden)
    compileOnly(files("libs/SpecialItems-1.2.2.jar"))
    // PlaceholderAPI, PlayerPoints, Oraxen, LandsAPI NICHT als Dependency!
    // compileOnly für lokale JARs möglich:
    // compileOnly(files("libs/PlaceholderAPI.jar"))
    // compileOnly(files("libs/PlayerPoints.jar"))
    // compileOnly(files("libs/Oraxen.jar"))
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
