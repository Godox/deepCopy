dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

include(":compiler-plugin")
project(":compiler-plugin").projectDir = file("../compiler-plugin")
include(":plugin-annotations")
project(":plugin-annotations").projectDir = file("../plugin-annotations")
