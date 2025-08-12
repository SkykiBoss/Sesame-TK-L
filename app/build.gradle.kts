import java.text.SimpleDateFormat
import java.util.*

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "fansirsqi.xposed.sesame"
    compileSdk = 36

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "fansirsqi.xposed.sesame"
        minSdk = 23
        targetSdk = 36

        if (!System.getenv("CI").toBoolean()) {
            ndk {
                abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
            }
        }

        // 版本号信息
        val major = 0
        val minor = 2
        val patch = 6
        val buildTag = "alpha"

        val buildDate = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        val buildTime = SimpleDateFormat("HH:mm:ss", Locale.CHINA).apply {
            timeZone = TimeZone.getTimeZone("GMT+8")
        }.format(Date())

        val buildTargetCode = runCatching {
            "${buildDate.replace("-", ".")}.${buildTime.replace(":", ".")}"
        }.getOrDefault("0000")

        val gitCommitCount = System.getenv("GIT_COMMIT_COUNT")?.toIntOrNull() ?: runCatching {
            val process = Runtime.getRuntime().exec(arrayOf("git", "rev-list", "--count", "HEAD"))
            val output = process.inputStream.bufferedReader().readText().trim()
            process.waitFor()
            if (process.exitValue() == 0) output.toInt() else 1
        }.getOrDefault(1)

        versionCode = gitCommitCount
        versionName = if (buildTag.contains("alpha") || buildTag.contains("beta")) {
            "v$major.$minor.$patch-$buildTag.$buildTargetCode"
        } else {
            "v$major.$minor.$patch-$buildTag"
        }

        buildConfigField("String", "BUILD_DATE", "\"$buildDate\"")
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        buildConfigField("String", "BUILD_NUMBER", "\"$buildTargetCode\"")
        buildConfigField("String", "BUILD_TAG", "\"$buildTag\"")
        buildConfigField("String", "VERSION", "\"v$major.$minor.$patch\"")

        testOptions {
            unitTests.all { it.enabled = false }
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    flavorDimensions += "default"
    productFlavors {
        create("normal") {
            dimension = "default"
            extra.set("applicationType", "Normal")
        }
        create("compatible") {
            dimension = "default"
            extra.set("applicationType", "Compatible")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
        }
    }

    // flavor-specific 编译配置
    productFlavors.all {
        when (name) {
            "normal" -> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                kotlin {
                    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17 }
                }
            }
            "compatible" -> {
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_11
                    targetCompatibility = JavaVersion.VERSION_11
                }
                kotlin {
                    compilerOptions { jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11 }
                }
            }
        }
    }

    signingConfigs {
        getByName("debug") {}
    }

    buildTypes {
        getByName("debug") {
            isDebuggable = true
            versionNameSuffix = "-debug"
            isShrinkResources = false
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    val cmakeFile = file("src/main/cpp/CMakeLists.txt")
    if (!System.getenv("CI").toBoolean() && cmakeFile.exists()) {
        externalNativeBuild {
            cmake {
                path = cmakeFile
                version = "3.31.6"
                ndkVersion = "29.0.13113456"
            }
        }
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val flavorName = variant.flavorName.replaceFirstChar { it.uppercase() }
            val dateCode = runCatching {
                variant.versionName.substringAfterLast("-").substring(0, 6)
            }.getOrDefault("000000")
            val fileName = "Sesame-TK-$flavorName-$dateCode-ALLG.apk"
            (this as com.android.build.gradle.internal.api.BaseVariantOutputImpl).outputFileName = fileName
        }
    }
}

dependencies {
    // Compose BOM 控制所有 Compose 版本
    val composeBom = platform("androidx.compose:compose-bom:2025.05.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    testImplementation(composeBom)

    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.5")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.compose.runtime:runtime-livedata")

    // 其他依赖
    implementation(libs.ui.tooling.preview.android)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)
    implementation(libs.kotlin.stdlib)
    implementation(libs.slf4j.api)
    implementation(libs.logback.android)
    implementation(libs.appcompat)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.material)
    implementation(libs.webkit)
    implementation(libs.lombok)
    annotationProcessor(libs.lombok)
    implementation(libs.okhttp)
    implementation(libs.dexkit)
    implementation(libs.jackson.kotlin)
    implementation("com.tencent:mmkv:2.2.2")
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    coreLibraryDesugaring(libs.desugar)

    // Flavor 特定依赖
    add("normalImplementation", libs.jackson.core)
    add("normalImplementation", libs.jackson.databind)
    add("normalImplementation", libs.jackson.annotations)
    add("normalCompileOnly", libs.libxposed.api)
    add("normalImplementation", libs.libxposed.service)

    add("compatibleImplementation", libs.jackson.core.compatible)
    add("compatibleImplementation", libs.jackson.databind.compatible)
    add("compatibleImplementation", libs.jackson.annotations.compatible)
    add("compatibleCompileOnly", libs.xposed.api)
}
