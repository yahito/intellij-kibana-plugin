rootProject.name = "kibana-plugin"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://packages.jetbrains.team/maven/p/ij/intellij-dependencies") }
        maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
    }

}