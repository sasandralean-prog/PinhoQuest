plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

import org.gradle.api.tasks.Copy

// P6-A: source-of-truth asset mappings. Source filenames in docs/design remain
// untouched; aliases make Android resource names stable and valid.
val canonicalBackgroundAssets = mapOf(
    "ConfigBGDIA.png" to "canonical_bg_settings_day.png",
    "JArdimVazioBG.png" to "canonical_bg_garden_empty_alt.png",
    "JardimArteDia.png" to "canonical_bg_garden_art_day.png",
    "JardimArteNoite.png" to "canonical_bg_garden_art_night.png",
    "JardimVazioDiaBG.png" to "canonical_bg_garden_empty_day.png",
    "PerfilBGDIA.png" to "canonical_bg_profile_day.png",
    "PerfilBGNoite.png" to "canonical_bg_profile_night.png",
    "StartBG.png" to "canonical_bg_start.png",
    "StartBGDIa.png" to "canonical_bg_start_day.png",
)

val canonicalButtonAssets = mapOf(
    "BackButton.png" to "btn_back.png",
    "BackupButton.png" to "canonical_backup_button.png",
    "ButtonTopBarConfig.png" to "canonical_topbar_config.png",
    "DOnateButton.png" to "canonical_donate_button.png",
    "DayButton.png" to "canonical_day_button.png",
    "FlowerCardDay.png" to "canonical_flower_card_day.png",
    "FlowerCardNight.png" to "canonical_flower_card_night.png",
    "FlowerColectionButton.png" to "canonical_flower_collection_button.png",
    "FlowerFilter.png" to "canonical_flower_filter.png",
    "FlowerFilterSearched.png" to "canonical_flower_filter_searched.png",
    "FlowerFilterUnkw.png" to "canonical_flower_filter_unknown.png",
    "FlowersFilterAll.png" to "canonical_flower_filter_all.png",
    "GameQuestButton.png" to "btn_quest_game.png",
    "JardimDeDay.png" to "canonical_garden_day_element.png",
    "JardimDeNight.png" to "canonical_garden_night_element.png",
    "JardimVAzioNight.png" to "canonical_garden_empty_night_element.png",
    "JardimVazioDIa.png" to "canonical_garden_empty_day_element.png",
    "MaiorButton.png" to "canonical_font_larger_button.png",
    "MedioButton.png" to "canonical_font_medium_button.png",
    "MenorButton.png" to "canonical_font_smaller_button.png",
    "NameBar.png" to "canonical_name_bar.png",
    "NavBarDay.png" to "canonical_nav_bar_day.png",
    "NavBarNight.png" to "canonical_nav_bar_night.png",
    "NightButton.png" to "canonical_night_button.png",
    "RandomQuestButton.png" to "btn_quest_random.png",
    "SortQuest.png" to "btn_sort_quest.png",
    "StartButton.png" to "btn_start.png",
    "TagAnimals.png" to "btn_tag_animais.png",
    "TagCreate.png" to "btn_tag_criar.png",
    "TagFantasy.png" to "btn_tag_fantasia.png",
    "TagLearn.png" to "btn_tag_learn.png",
    "TagMusic.png" to "btn_tag_musica.png",
    "TagNature.png" to "btn_tag_natureza.png",
    "TagPhoto.png" to "btn_tag_fotografia.png",
    "TagRelax.png" to "btn_tag_relaxar.png",
    "TagTech.png" to "btn_tag_tecnologia.png",
    "VerQuestButton.png" to "canonical_see_quests_button.png",
)

val syncCanonicalUiAssets = tasks.register<Copy>("syncCanonicalUiAssets") {
    val canonicalBackgroundDir = rootProject.file("docs/design/BackGround")
    val canonicalButtonDir = rootProject.file("docs/design/Button")

    from(canonicalBackgroundDir) {
        include(canonicalBackgroundAssets.keys)
        rename { sourceName -> canonicalBackgroundAssets.getValue(sourceName) }
    }
    from(canonicalButtonDir) {
        include(canonicalButtonAssets.keys)
        rename { sourceName -> canonicalButtonAssets.getValue(sourceName) }
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
    val canonicalBackgroundDir = rootProject.file("docs/design/BackGround")
    val canonicalButtonDir = rootProject.file("docs/design/Button")
    val generatedDir = layout.buildDirectory.dir("generated/p6-canonical-ui-res/drawable-nodpi")
    inputs.files(
        canonicalBackgroundAssets.keys.map { canonicalBackgroundDir.resolve(it) } +
            canonicalButtonAssets.keys.map { canonicalButtonDir.resolve(it) },
    )
    doLast {
        val missingSources = buildList {
            canonicalBackgroundAssets.keys
                .filterNot { canonicalBackgroundDir.resolve(it).isFile }
                .forEach { add("docs/design/BackGround/$it") }
            canonicalButtonAssets.keys
                .filterNot { canonicalButtonDir.resolve(it).isFile }
                .forEach { add("docs/design/Button/$it") }
        }
        check(missingSources.isEmpty()) {
            "Missing canonical PinhoQuest assets:\n" + missingSources.joinToString("\n")
        }
        val generatedRoot = generatedDir.get().asFile
        val missingOutputs = (canonicalBackgroundAssets.values + canonicalButtonAssets.values)
            .filterNot { generatedRoot.resolve(it).isFile }
        check(missingOutputs.isEmpty()) {
            "Canonical asset sync did not generate:\n" + missingOutputs.joinToString("\n")
        }
        val duplicateOutputs = (canonicalBackgroundAssets.values + canonicalButtonAssets.values)
            .groupingBy { it }
            .eachCount()
            .filterValues { it > 1 }
            .keys
        check(duplicateOutputs.isEmpty()) {
            "Duplicate canonical Android resource names: " + duplicateOutputs.joinToString()
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
