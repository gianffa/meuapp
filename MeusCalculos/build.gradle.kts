plugins {
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
}

android {
    namespace = "com.example.meuscalculos"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
publishing {
    publications {
        create<MavenPublication>("minhapubli") {
            groupId = "com.example"
            artifactId = "meuscalculos"
            version = "1.0"
            artifact("build/outputs/aar/meuscalculos-release.aar")
        }
    }
    repositories{
        maven{
            name = "GithubPackages"
            url = uri("https://maven.pkg.github.com/gianffa/gianffa/meuappTentativaPackage")
            credentials{
                username = "gianffa"
                password = "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"
            }
        }
    }
}
dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
repositories {
    google()
    mavenCentral()
}

//att20