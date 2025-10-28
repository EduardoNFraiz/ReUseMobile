plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
    id("androidx.navigation.safeargs.kotlin")

}

android {
    namespace = "com.projetointegrador.reuse"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.projetointegrador.reuse"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    viewBinding{
        enable=true
    }

}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // ✅ Firebase KTX via Version Catalog (SEM a duplicação manual)
    implementation(libs.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Outras libs
    implementation ("io.github.chaosleung:pinview:1.4.4")

    // 🌟 Firebase BOM (34.3.0) - CRUCIAL
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))

    // ✅ Firebase Auth (Mantido como String pois não está no libs.toml)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database-ktx:21.0.0")
    implementation("com.google.firebase:firebase-storage-ktx:21.0.2")

    // Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor ("com.github.bumptech.glide:compiler:4.16.0")
}