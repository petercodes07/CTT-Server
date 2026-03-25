plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

dependencies {
    implementation(libs.ktorServerCore)
    implementation(libs.ktorServerNetty)
    implementation(libs.ktorServerContentNegotiation)
    implementation(libs.ktorSerializationKotlinxJson)
    implementation(libs.kotlinxSerializationJson)
}

application {
    mainClass.set("com.example.backend.BackendKt")
}
