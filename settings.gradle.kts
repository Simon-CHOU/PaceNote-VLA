pluginManagement {
    repositories {
        // 插件镜像
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 1. 优先使用阿里云镜像（应用依赖）
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        
        // 2. 注释掉或删除 LiveKit 的 GitHub 仓库，因为它需要 Token 认证且包已在 MavenCentral
        // maven { url = uri("https://maven.pkg.github.com/livekit/metadata") }
    }
}

rootProject.name = "PaceNote-VLA"
include(":app")
include(":core:database")
include(":core:domain")
include(":core:ui")
include(":feature:sensor")
include(":feature:camera")
include(":feature:media-pipe")
include(":feature:livekit")
include(":feature:telemetry")
include(":feature:auth")
include(":feature:monetization")
