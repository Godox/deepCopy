plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.node.gradle) apply false
    alias(libs.plugins.buildconfig) apply false
}

allprojects {
    group = "fr.godox.deepcopy"
    version = "2.4.20-0.1.1"
}
