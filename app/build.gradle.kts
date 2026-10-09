plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import org.gradle.api.tasks.Copy

// P6-A: source-of-truth asset mappings. Source filenames in docs/design remain
// untouched; aliases make Android resource names stable and valid.
// P6-A: canonical source filenames -> generated Android drawable filenames.
// Source art remains editable in docs/design; aliases are lower-case Android-safe resources.
val canonicalBackgroundAssets = mapOf(
    "bg_config_day.png" to "canonical_bg_config_day.png",
    "bg_config_night.png" to "canonical_bg_config_night.png",
    "bg_gardem_art_day.png" to "canonical_bg_gardem_art_day.png",
    "bg_gardem_art_night.png" to "canonical_bg_gardem_art_night.png",
    "bg_gardem_empty_day.png" to "canonical_bg_gardem_empty_day.png",
    "bg_gardem_empty_night.png" to "canonical_bg_gardem_empty_night.png",
    "bg_profile_day.png" to "canonical_bg_profile_day.png",
    "bg_profile_night.png" to "canonical_bg_profile_night.png",
    "bg_start_day.png" to "canonical_bg_start_day.png",
    "bg_start_night.png" to "canonical_bg_start_night.png",
)

val canonicalButtonAssets = mapOf(
    "btn_back.png" to "btn_back.png",
    "btn_confirm.png" to "btn_confirm.png",
    "btn_font_size.png" to "btn_font_size.png",
    "btn_garden_backup.png" to "btn_garden_backup.png",
    "btn_garden_collection.png" to "btn_garden_collection.png",
    "btn_garden_filter_all.png" to "btn_garden_filter_all.png",
    "btn_garden_filter_collected.png" to "btn_garden_filter_collected.png",
    "btn_garden_filter_searched.png" to "btn_garden_filter_searched.png",
    "btn_garden_filter_unkw.png" to "btn_garden_filter_unkw.png",
    "btn_quest_draw.png" to "btn_quest_draw.png",
    "btn_quest_game.png" to "btn_quest_game.png",
    "btn_quest_random.png" to "btn_quest_random.png",
    "btn_support_creator.png" to "btn_support_creator.png",
    "btn_theme_day.png" to "btn_theme_day.png",
    "btn_theme_night.png" to "btn_theme_night.png",
    "btn_view_quests.png" to "btn_view_quests.png",
    "card_flower_unknown_day.png" to "card_flower_unknown_day.png",
    "card_flower_unknown_night.png" to "card_flower_unknown_night.png",
    "category_animal.png" to "category_animal.png",
    "category_appreciation.png" to "category_appreciation.png",
    "category_creativity.png" to "category_creativity.png",
    "category_games.png" to "category_games.png",
    "category_learn.png" to "category_learn.png",
    "category_music.png" to "category_music.png",
    "category_nature.png" to "category_nature.png",
    "category_photography.png" to "category_photography.png",
    "category_science.png" to "category_science.png",
)

val canonicalCardAssets = mapOf(
    "card_garden_empty_day.png" to "card_garden_empty_day.png",
    "card_garden_empty_night.png" to "card_garden_empty_night.png",
    "card_home_day.png" to "card_home_day.png",
    "card_home_night.png" to "card_home_night.png",
    "card_profile_day.png" to "card_profile_day.png",
    "card_profile_night.png" to "card_profile_night.png",
    "card_profile_tags.png" to "card_profile_tags.png",
)

val canonicalNavBarAssets = mapOf(
    "name_bar.png" to "name_bar.png",
    "nav_bar_day.png" to "nav_bar_day.png",
    "nav_bar_night.png" to "nav_bar_night.png",
)

val canonicalAssetGroups = listOf(
    Triple(rootProject.file("docs/design/Background"), canonicalBackgroundAssets, "docs/design/Background"),
    Triple(rootProject.file("docs/design/Button"), canonicalButtonAssets, "docs/design/Button"),
    Triple(rootProject.file("docs/design/Card"), canonicalCardAssets, "docs/design/Card"),
    Triple(rootProject.file("docs/design/Navbar"), canonicalNavBarAssets, "docs/design/Navbar"),
)

val syncCanonicalUiAssets = tasks.register<Copy>("syncCanonicalUiAssets") {
    canonicalAssetGroups.forEach { (sourceDir, mappings, _) ->
        from(sourceDir) {
            include(mappings.keys)
            rename { sourceName -> mappings.getValue(sourceName) }
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

val verifyCanonicalUiAssets = tasks.register("verifyCanonicalUiAssets") {
    dependsOn(syncCanonicalUiAssets)
    val generatedDir = layout.buildDirectory.dir("generated/p6-canonical-ui-res/drawable-nodpi")
    inputs.files(canonicalAssetGroups.flatMap { (sourceDir, mappings, _) ->
        mappings.keys.map { sourceDir.resolve(it) }
    })
    doLast {
        val missingSources = canonicalAssetGroups.flatMap { (sourceDir, mappings, relativeDir) ->
            mappings.keys.filterNot { sourceDir.resolve(it).isFile }
                .map { "$relativeDir/$it" }
        }
        check(missingSources.isEmpty()) {
            "Missing canonical PinhoQuest assets:\n" + missingSources.joinToString("\n")
        }
        val outputs = canonicalAssetGroups.flatMap { (_, mappings, _) -> mappings.values }
        val generatedRoot = generatedDir.get().asFile
        val missingOutputs = outputs.filterNot { generatedRoot.resolve(it).isFile }
        check(missingOutputs.isEmpty()) {
            "Canonical asset sync did not generate:\n" + missingOutputs.joinToString("\n")
        }
        val duplicateOutputs = outputs.groupingBy { it }.eachCount().filterValues { it > 1 }.keys
        check(duplicateOutputs.isEmpty()) {
            "Duplicate canonical Android resource names: " + duplicateOutputs.joinToString()
        }
        check(outputs.size == 47) {
            "Expected 47 canonical PNG mappings but found ${outputs.size}"
        }
    }
}

tasks.named("preBuild") {
    dependsOn(verifyCanonicalUiAssets)
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
