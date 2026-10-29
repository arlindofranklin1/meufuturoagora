import java.util.Properties
import org.gradle.testing.jacoco.plugins.JacocoTaskExtension

// URL e chave pública do Supabase ficam no local.properties (fora do Git).
// Só a chave pública (anon / sb_publishable_...) pode ir para o app.
val propriedadesLocais = Properties().apply {
    val arquivo = rootProject.file("local.properties")
    if (arquivo.exists()) arquivo.inputStream().use { load(it) }
}

fun propriedadeObrigatoria(nome: String): String =
    propriedadesLocais.getProperty(nome)?.trim()?.takeIf { it.isNotEmpty() }
        ?: throw GradleException(
            "Falta $nome no local.properties. Veja PROMPT_MIGRACAO_SUPABASE.md, Seção 3.1."
        )

val supabaseUrl = propriedadeObrigatoria("SUPABASE_URL")
val supabaseChavePublica = propriedadeObrigatoria("SUPABASE_ANON_KEY")

if (supabaseChavePublica.startsWith("sb_secret_") || supabaseChavePublica.contains("service_role")) {
    throw GradleException("SUPABASE_ANON_KEY precisa ser a chave pública, nunca a secreta.")
}

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "br.com.example.meufuturoagora"
    compileSdk {
        // 37: exigido pelo OkHttp 5.5. O targetSdk continua 36 (comportamento no aparelho não muda)
        version = release(37)
    }

    defaultConfig {
        applicationId = "br.com.example.meufuturoagora"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_CHAVE_PUBLICA", "\"$supabaseChavePublica\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // Keystore de debug compartilhado (no Git) para que todos os computadores
    // gerem o mesmo SHA-1 já cadastrado no Firebase (login com Google).
    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
            // Relatório de cobertura: ./gradlew createDebugUnitTestCoverageReport
            enableUnitTestCoverage = true
        }
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

    testCoverage {
        jacocoVersion = libs.versions.jacoco.get()
    }

    // Robolectric executa as tarefas do Firestore na thread principal simulada
    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric acessa internos do FileDescriptor, fechados por padrão no JDK 17+
            it.jvmArgs(
                "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                "--add-opens=java.base/java.io=ALL-UNNAMED"
            )
            // Sem isto o JaCoCo não enxerga as classes carregadas pelo Robolectric
            it.extensions.configure(JacocoTaskExtension::class.java) {
                isIncludeNoLocationClasses = true
                excludes = listOf("jdk.internal.*")
            }
        }
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockwebserver)
    // O org.json do android.jar é só um esqueleto nos testes JVM
    testImplementation(libs.org.json)

    implementation(libs.okhttp)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.intents)
    androidTestImplementation(libs.test.rules)
    implementation(platform("com.google.firebase:firebase-bom:34.18.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // Google Login
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Verificação periódica de novidades para as notificações
    implementation("androidx.work:work-runtime:2.10.1")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}