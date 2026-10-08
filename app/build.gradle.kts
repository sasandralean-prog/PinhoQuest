plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import org.gradle.api.tasks.Copy

val syncCanonicalUiAssets = tasks.register<Copy>("syncCanonicalUiAssets") {
    val canonicalDir = rootProject.file("docs/design/Button")
    from(canonicalDir) {
        include(
            "BtnBack.png",
            "BtnQuestGame.png",
            "BtnQuestRandom.png",
            "BtnSeeQuests.png",
            "BtnSortQuest.png",
            "BtnStart.png",
            "BtnTagAnimais.png",
            "BtnTagAventuras.png",
            "BtnTagCriar.png",
            "BtnTagFantasia.png",
            "BtnTagFotografia.png",
            "BtnTagMusica.png",
            "BtnTagNatureza.png",
            "BtnTagRelaxar.png",
            "BtnTagTecnologia.png",
            "CardFlowerUnknownA.png",
            "CardFlowerUnknownB.png",
        )
        rename { name ->
            name
                .removeSuffix(".png")
                .replace(Regex("([a-z])([A-Z])"), "$1_$2")
                .lowercase() + ".png"
        }
    }
    into(layout.buildDirectory.dir("generated/p6-canonical-ui-res/drawable-nodpi"))
}

android {
    namespace = "com.pinhoquest"
    sourceSets["main"].res.srcDir(layout.buildDirectory.dir("generated/p6-canonical-ui-res"))

    compileSdk = 35

    defaultConfig {
        applicationId = "com.pinhoquest"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    testOptions {
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        jvmToolchain(17)
    }
}

tasks.matching { it.name.endsWith("PreBuild") }.configureEach {
    dependsOn(syncCanonicalUiAssets)
}

dependencies {
    implementation(project(":quest-domain"))
    implementation(project(":quest-core"))
    implementation(project(":android-data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(project(":litertlm-bridge"))
    // The production app does not compile against LiteRT-LM APIs, but it must package
    // the JNI runtime as an application dependency so NativeLibraryLoader can load it.
    runtimeOnly(libs.litertlm.android)

    androidTestImplementation(libs.androidx.work.testing)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestUtil(libs.androidx.test.orchestrator)
}

configurations.matching { it.name.endsWith("CompileClasspath") }.configureEach {
    resolutionStrategy.force(
        "org.jetbrains.kotlin:kotlin-stdlib:2.1.21",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.1.21",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.21",
    )
}
