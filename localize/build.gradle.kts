
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.kit.localize"
    compileSdk = 34

    defaultConfig {
        minSdk = 25

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    //读写XML文件
    implementation(libs.dom4j){
        exclude(group = "pull-parser", module = "pull-parser")
    }
    //读写CSV文件
    implementation(libs.commons.csv)
    //读写Excel文件
    implementation(libs.poi)
    implementation(libs.poi.ooxml)
    implementation(libs.poi.ooxml.schemas)
}


tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

