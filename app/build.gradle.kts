plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.alex.metasahorro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.alex.metasahorro"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    // Esquemas de Room versionados (carpeta app/schemas) para poder escribir migraciones.
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }

    // Los esquemas exportados de Room sirven de assets para probar las migraciones en el emulador.
    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Regenera docs/wiki a partir del código (texto, tablas, diagramas). No necesita emulador.
// Las capturas se toman aparte con tools/wiki/capture-screenshots.ps1.
tasks.register<Exec>("generateWiki") {
    group = "documentation"
    description = "Regenera el wiki interno (docs/wiki) desde el código fuente."
    workingDir = rootProject.projectDir
    commandLine(
        File(System.getProperty("java.home"), "bin/java").absolutePath,
        "tools/wiki/WikiGenerator.java",
        rootProject.projectDir.absolutePath
    )
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")

    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended")

    // Room (base de datos local, sin servidores)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // DataStore para guardar el tema elegido
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // WorkManager para el recordatorio diario
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Coil para mostrar las fotos que el usuario sube a cada meta
    implementation("io.coil-kt:coil-compose:2.6.0")

    testImplementation("junit:junit:4.13.2")

    // Prueba instrumentada que toma las capturas del wiki (ver tools/wiki)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    // Espresso 3.5 (el que trae Compose) falla en Android 16+ con "InputManager.getInstance"
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.06.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
