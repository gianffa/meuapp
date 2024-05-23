plugins {
    alias(libs.plugins.androidLibrary)
    id("maven-publish")
    //`maven-publish`
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
repositories {
    google()
    mavenCentral()
}
dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
publishing {
    publications {
        create<MavenPublication>("minhapubli") {
            groupId = "com.example"
            artifactId = "MeusCalculos"
            version = "1.0"
            artifact("build/outputs/aar/MeusCalculos-release.aar")
        }
    }
}
repositories{
    maven{
        name = "GithubPackages"
        url = uri("https://maven.pkg.github.com/gianffa/meuapp")
        credentials{
            username = "gianffa"
            password = "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"
        }
    }
}
//publishing {
//    publications {
//        register<MavenPublication>("release") {
//            afterEvaluate{
//                from(components["release"])
//            }
//        }
//    }
//}