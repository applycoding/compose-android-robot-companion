// SPDX-License-Identifier: Apache-2.0

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.license.report)
    application
}

kotlin {
    jvmToolchain(21)
}

application {
    mainClass.set("com.applycoding.robotcompanion.server.MainKt")
}

dependencies {
    implementation(project(":sim"))
    implementation(project(":link"))
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.cio)
    implementation(libs.ktor.server.websockets)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.websockets)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}

licenseReport {
    renderers = arrayOf(com.github.jk1.license.render.InventoryMarkdownReportRenderer())
    filters = arrayOf(com.github.jk1.license.filter.LicenseBundleNormalizer())
}

tasks.test {
    useJUnit()
    jvmArgs(
        "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
    )
}
