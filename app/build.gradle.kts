plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.dagger.hilt.android")
    // Comment out for mock testing (no Firebase/Google Services)
    // id("com.google.gms.google-services")
    // id("com.google.firebase.crashlytics")
    kotlin("kapt")
}

// ========== Android 16+ 16KB 页大小强制对齐解决方案 ==========
// 创建 Gradle 任务在打包前对所有 JNI 库进行 16KB 对齐处理
//
// 说明：
// 1. Android 15+ (targetSdk 35+) 强制要求所有 .so 文件按 16KB 对齐
// 2. MediaPipe 等第三方库的预编译二进制可能未对齐
// 3. 此任务在 native libraries 合并后、APK 打包前执行对齐
//
// 技术细节：
// - 使用 simple padding 方法（在文件末尾添加零填充）
// - ELF 格式允许在文件末尾添加 padding，不影响功能
// - 对齐后的文件大小为 16KB 的倍数

tasks.register("align16KBJniLibs") {
    group = "android"
    description = "Align all JNI libraries to 16KB page boundaries for Android 15+ compatibility"

    doLast {
        val PAGE_SIZE = 16384 // 16KB in bytes
        val buildDir = project.layout.buildDirectory.get().asFile

        // 查找包含 .so 文件的多个可能路径
        val possiblePaths = listOf(
            "intermediates/stripped_native_libs/debug/out/lib",
            "intermediates/merged_native_libs/debug/out/lib",
            "intermediates/library_and_local_jars_jni/debug/out/jni"
        )

        val jniLibsDirs = possiblePaths.mapNotNull { path ->
            File(buildDir, path).takeIf { it.exists() }
        }.ifEmpty {
            // 尝试查找所有可能的 lib 目录
            buildDir.walkTopDown()
                .filter { it.isDirectory && it.name == "lib" }
                .filter { libDir -> libDir.walkTopDown().any { it.extension == "so" } }
                .toList()
        }

        if (jniLibsDirs.isEmpty()) {
            println("⚠️ No JNI lib directories found in build output")
            println("→ Build might not have completed native lib processing yet")
            return@doLast
        }

        println("→ Aligning JNI libraries to 16KB page boundaries...")
        println("→ Found ${jniLibsDirs.size} lib directories")

        var alignedCount = 0
        var skippedCount = 0
        var totalCount = 0
        val failedFiles = mutableListOf<String>()

        jniLibsDirs.forEach { libDir ->
            println("  Processing: $libDir")

            libDir.walkTopDown()
                .filter { it.extension == "so" }
                .forEach { soFile ->
                    totalCount++
                    try {
                        val fileSize = soFile.length()
                        val paddingNeeded = (PAGE_SIZE - (fileSize % PAGE_SIZE)) % PAGE_SIZE

                        if (paddingNeeded > 0) {
                            println("    Aligning: ${soFile.name} (${(fileSize / 1024)}KB -> +${paddingNeeded}B padding)")

                            // 创建临时文件并添加零填充
                            val tempFile = File(soFile.parent, "${soFile.name}.aligned.$${System.currentTimeMillis()}")

                            soFile.inputStream().use { input ->
                                tempFile.outputStream().use { output ->
                                    // 复制原始内容
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    while (input.read(buffer).also { bytesRead = it } > 0) {
                                        output.write(buffer, 0, bytesRead)
                                    }
                                    // 写入零填充到 16KB 边界
                                    output.write(ByteArray(paddingNeeded.toInt()))
                                }
                            }

                            // 验证新文件大小是否正确对齐
                            val newFileSize = tempFile.length()
                            if (newFileSize % PAGE_SIZE == 0L) {
                                // 替换原文件
                                if (soFile.delete()) {
                                    if (tempFile.renameTo(soFile)) {
                                        alignedCount++
                                    } else {
                                        failedFiles.add("${soFile.name} (rename failed)")
                                    }
                                } else {
                                    failedFiles.add("${soFile.name} (delete failed)")
                                }
                            } else {
                                failedFiles.add("${soFile.name} (alignment verification failed)")
                                tempFile.delete()
                            }
                        } else {
                            skippedCount++
                        }
                    } catch (e: Exception) {
                        println("    ✗ Failed to align ${soFile.name}: ${e.message}")
                        failedFiles.add("${soFile.name} (${e.message})")
                    }
                }
        }

        println("\n========== Alignment Summary ==========")
        println("Total files processed: $totalCount")
        println("Successfully aligned: $alignedCount")
        println("Already aligned: $skippedCount")
        println("Failed: ${failedFiles.size}")

        if (failedFiles.isNotEmpty()) {
            println("\n⚠️ Failed files:")
            failedFiles.forEach { println("  - $it") }
        }

        if (totalCount == 0) {
            println("\n⚠️ Warning: No .so files found. This might indicate:")
            println("  1. Build hasn't completed native library processing")
            println("  2. Task is running too early in the build process")
            println("  3. No native libraries are included in the build")
        } else {
            println("\n✓ Alignment task completed")
        }
    }
}

// 关键：确保对齐任务在正确的时机运行
// 需要在 mergeNativeLibs 之后、package 之前
afterEvaluate {
    // 让对齐任务在 mergeDebugNativeLibs 之后运行
    tasks.findByName("mergeDebugNativeLibs")?.let {
        tasks.named("align16KBJniLibs") { mustRunAfter(it) }
    }

    tasks.findByName("mergeReleaseNativeLibs")?.let {
        tasks.named("align16KBJniLibs") { mustRunAfter(it) }
    }

    // 确保在打包前完成对齐
    tasks.named("packageDebug") {
        dependsOn("align16KBJniLibs")
    }
    tasks.named("packageRelease") {
        dependsOn("align16KBJniLibs")
    }
}

android {
    namespace = "com.pacenote.vla"
    compileSdk = 36  // Android 15 compatibility

    defaultConfig {
        applicationId = "com.pacenote.vla"
        minSdk = 28
        // 恢复 targetSdk 到 35 以支持 Android 15+ 设备
        // 配合对齐任务解决 16KB 分页问题
        targetSdk = 35

        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
            "-opt-in=kotlinx.coroutines.FlowPreview"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // 关键配置：禁用传统压缩打包，保持库未压缩
            // 配合对齐任务确保 16KB 页对齐
            useLegacyPackaging = false

            // 可选：保留调试符号以便诊断（发布版可移除）
            keepDebugSymbols += "**/libmediapipe*.so"
        }
    }
}

dependencies {
    // Project modules
    implementation(project(":core:database"))
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(project(":feature:sensor"))
    implementation(project(":feature:camera"))
    implementation(project(":feature:media-pipe"))
    implementation(project(":feature:livekit"))
    implementation(project(":feature:telemetry"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:monetization"))

    // Core Android
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.8.3")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")

    // CameraX
    implementation("androidx.camera:camera-core:1.4.1")
    implementation("androidx.camera:camera-camera2:1.4.1")
    implementation("androidx.camera:camera-lifecycle:1.4.1")
    implementation("androidx.camera:camera-video:1.4.1")
    implementation("androidx.camera:camera-view:1.4.1")
    implementation("androidx.camera:camera-extensions:1.4.1")

    // MediaPipe Tasks Vision
    // 注意：0.10.20 版本可能不支持 16KB 页对齐
    // 如需在 Android 15+ (targetSdk 35+) 上运行，请等待官方更新
    implementation("com.google.mediapipe:tasks-vision:0.10.20")

    // LiveKit
    implementation("io.livekit:livekit-android:2.23.1")

    // Coroutines & Flow
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Mock dependencies
    implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("com.google.accompanist:accompanist-permissions:0.36.0")
    implementation("com.jakewharton.timber:timber:5.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
