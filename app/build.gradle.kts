plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.laleme.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.laleme.app"
        minSdk = 31
        targetSdk = 34
        versionCode = 3
        versionName = "1.1.2"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            // 打开 R8 收缩，把 Compose / 图标库的未使用代码裁掉
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ""
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.10"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // 显式声明协程，供数据层与测试使用
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // 用真实 Room + SQLite 跑数据库测试（Robolectric 提供 Android 运行环境）
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    // Compose UI 测试：用来验证「按钮真的在屏幕上、且没被盖住」
    testImplementation(composeBom)
    testImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<Test>().configureEach {
    systemProperty("robolectric.logging", "stdout")
}

/**
 * 在 Android Studio 里跑测试时，Robolectric 需要这个 jar。
 * 本机命令行环境下 Gradle 的 test worker 起不来（它依赖 stdin 管道，
 * 见 README「已知限制」），所以纯逻辑测试是手工调 JUnit 跑的。
 */
tasks.register("dumpTestClasspath") {
    group = "verification"
    description = "把 debugUnitTest 的运行时 classpath 导出到 build/test-classpath.txt"
    val outFile = layout.buildDirectory.file("test-classpath.txt")
    val conf = configurations.named("debugUnitTestRuntimeClasspath")
    val buildDir = layout.buildDirectory
    doLast {
        val list = mutableListOf<String>()
        list += buildDir.dir("tmp/kotlin-classes/debugUnitTest").get().asFile.absolutePath
        list += buildDir.dir("tmp/kotlin-classes/debug").get().asFile.absolutePath
        list += conf.get().files.map { it.absolutePath }
        outFile.get().asFile.writeText(list.joinToString("\n"))
        println("测试 classpath 已导出：${outFile.get().asFile}（共 ${list.size} 项）")
    }
}
