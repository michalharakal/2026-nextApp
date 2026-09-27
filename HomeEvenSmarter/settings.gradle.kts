rootProject.name = "HomeEvenSmarter"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

// The two cartridge blueprints (Kotlin API + build recipe, no weights) are consumed from a sibling checkout of
// SKaiNET-cartridge-blueprints as an included build. Override the location with -PblueprintsDir=<path>.
val blueprintsDir = providers.gradleProperty("blueprintsDir").getOrElse("../../SKaiNET-cartridge-blueprints")
includeBuild(blueprintsDir) {
    dependencySubstitution {
        substitute(module("sk.ainet.cartridge:asr-moonshine-v2-streaming-iree"))
            .using(project(":blueprints:asr-moonshine-v2-streaming-iree"))
        substitute(module("sk.ainet.cartridge:nlu-functiongemma-270m-iree"))
            .using(project(":blueprints:nlu-functiongemma-270m-iree"))
    }
}

include(":core")
include(":cartridges")
include(":app:shared")
include(":app:androidApp")
include(":app:desktopApp")
include(":server")
