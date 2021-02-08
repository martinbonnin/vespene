plugins {
    id("maven-publish")
    kotlin("jvm").version("1.4.21")
    kotlin("kapt").version("1.4.21")
    id("net.mbonnin.one.eight").version("0.1")
}

group = "net.mbonnin.vespene"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("org.bouncycastle:bcpg-jdk15on:1.64")
    implementation("com.squareup.moshi:moshi:1.11.0")

    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.11.0")

    testImplementation("junit:junit:4.12")
}

val sourcesJar by tasks.creating(Jar::class) {
    archiveClassifier.set("sources")
    from(sourceSets.getByName("main").allSource)
}

val javadocJar by tasks.creating(Jar::class) {
    // Empty javadoc :)
    archiveClassifier.set("javadoc")
}

configure<PublishingExtension> {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])

            artifact(javadocJar)
            artifact(sourcesJar)

            pom {

            }
        }
    }
}