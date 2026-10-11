pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal(); maven("https://jitpack.io") }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral(); maven("https://jitpack.io") }
}

rootProject.name = "Browxer"

include(":app")
include(":core:common")
include(":core:ui")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:datastore")
include(":core:network")
include(":core:media")
include(":engine:sniffer")
include(":engine:ranker")
include(":engine:resolver")
include(":engine:downloader")
include(":engine:muxer")
include(":feature:browser")
include(":feature:download_sheet")
include(":feature:library")
include(":feature:player")
include(":feature:settings")
