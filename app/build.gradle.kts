plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.onyx.dailydiary"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.onyx.dailydiary"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/groovy/org.codehaus.groovy.runtime.ExtensionModule"
        }
        jniLibs {
            pickFirsts += "lib/x86/libc++_shared.so"
            pickFirsts += "lib/x86_64/libc++_shared.so"
            pickFirsts += "lib/armeabi-v7a/libc++_shared.so"
            pickFirsts += "lib/arm64-v8a/libc++_shared.so"
        }

        resources.excludes.add("META-INF/*")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

android {
    buildFeatures {
        dataBinding = true
        viewBinding = true
    }
}

dependencies {
    implementation("org.mnode.ical4j:ical4j:3.0.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.onyx.android.sdk:onyxsdk-device:1.2.26") {
        exclude(group = "com.android.support", module = "support-compat")
    }
    implementation("com.onyx.android.sdk:onyxsdk-pen:1.4.7"){
        exclude(group = "com.android.support", module = "support-compat")
    }
    implementation("com.onyx.android.sdk:onyxsdk-base:1.6.50"){
        exclude(group = "com.android.support", module = "support-compat")
    }
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}
