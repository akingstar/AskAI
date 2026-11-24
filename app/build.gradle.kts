plugins { alias(libs.plugins.android.application); alias(libs.plugins.kotlin.android); alias(libs.plugins.kotlin.compose); alias(libs.plugins.kotlin.serialization); alias(libs.plugins.ksp) }
android {
 namespace = "com.akingstar.askai"; compileSdk = 35
 defaultConfig { applicationId = "com.akingstar.askai"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
 buildFeatures { compose = true; buildConfig = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 val signingReady = listOf("ASKAI_KEYSTORE_FILE", "ASKAI_KEYSTORE_PASSWORD", "ASKAI_KEY_ALIAS", "ASKAI_KEY_PASSWORD").all { !System.getenv(it).isNullOrBlank() }
 if (signingReady) signingConfigs.create("release") {
  storeFile = file(System.getenv("ASKAI_KEYSTORE_FILE")!!)
  storePassword = System.getenv("ASKAI_KEYSTORE_PASSWORD")
  keyAlias = System.getenv("ASKAI_KEY_ALIAS")
  keyPassword = System.getenv("ASKAI_KEY_PASSWORD")
 }
 buildTypes { debug { buildConfigField("boolean", "ALLOW_DEBUG_LOGS", "true") }; release { if (signingReady) signingConfig = signingConfigs.getByName("release"); isMinifyEnabled = true; proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro") } }
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
 implementation(libs.androidx.core.ktx); implementation(libs.androidx.activity.compose); implementation(libs.androidx.lifecycle.runtime.ktx); implementation(libs.androidx.lifecycle.viewmodel.compose)
 implementation(platform(libs.androidx.compose.bom)); implementation(libs.androidx.compose.ui); implementation(libs.androidx.compose.ui.tooling.preview); implementation(libs.androidx.compose.material3); implementation(libs.androidx.compose.material.icons); debugImplementation(libs.androidx.compose.ui.tooling)
 implementation(libs.androidx.room.runtime); implementation(libs.androidx.room.ktx); ksp(libs.androidx.room.compiler); implementation(libs.androidx.work.runtime); implementation(libs.okhttp); implementation(libs.kotlinx.serialization.json); implementation(libs.security.crypto); implementation(libs.datastore.preferences)
}
