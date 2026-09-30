dependencyResolutionManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

pluginManagement {
    repositories {
        mavenCentral()
        mavenLocal()
    }
}

