import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"
    application
}

group = "info.skyblond.gahled"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // kotlin logging
    implementation("io.github.microutils:kotlin-logging-jvm:2.1.23")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    // H2 db
    implementation("com.h2database:h2:2.1.214")
    // hikariCP
    implementation("com.zaxxer:HikariCP:5.0.1")
    // exposed
    implementation("org.jetbrains.exposed", "exposed-core", "0.38.2")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.38.2")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.38.2")
    // telegram bot api
    implementation("org.telegram:telegrambots:6.0.1")



    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("info.skyblond.gahled.MainKt")
}
