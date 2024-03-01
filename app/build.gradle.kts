plugins {
    id("com.android.application")
}

android {
    namespace = "com.rex.proto.kirin"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rex.proto.kirin"

        minSdk = 23
        targetSdk = 34

        versionName = "1.0"
        versionCode = Integer.parseInt((['git', '-C', projectDir.toString(), 'rev-list', '--count', 'HEAD'].execute().text).trim())

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            versionNameSuffix = "-debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment:2.6.0")
    implementation("androidx.navigation:navigation-ui:2.6.0")
    implementation("androidx.preference:preference:1.2.1")
    implementation("com.google.android.material:material:1.9.0")

    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("com.github.tony19:logback-android:3.0.0")

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.10")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.10")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.6.0")

    testImplementation("ch.qos.logback:logback-core:1.4.14")
    testImplementation("ch.qos.logback:logback-classic:1.4.14")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("com.github.tony19:logback-android:3.0.0")
    androidTestImplementation("org.mockito:mockito-android:5.6.0")
}