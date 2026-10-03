plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.pinhoquest.inference.bridge"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile>().configureEach {
        javaCompiler.set(
            javaToolchains.compilerFor {
                languageVersion.set(JavaLanguageVersion.of(21))
            },
        )
    }
}

dependencies {
    implementation(project(":quest-core"))
    compileOnly(libs.litertlm.android)

    testImplementation(libs.litertlm.android)
    testImplementation(libs.junit)
}
