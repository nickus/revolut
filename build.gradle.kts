import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "revolut"
version = "1.0-SNAPSHOT"

val kotlin_version = "1.3.61"
val ktor_version = "1.3.0"
val mainClass = "com.revolut.ApplicationKt"
repositories {
    maven("https://dl.bintray.com/kotlin/kotlin")
    jcenter()
    mavenCentral()
    maven("https://plugins.gradle.org/m2/")
}

plugins {
    application
    kotlin("jvm") version "1.3.61"
    groovy
}

application {
    mainClassName = mainClass
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile>().all {
    kotlinOptions.jvmTarget = "1.8"
}


tasks.named<GroovyCompile>("compileTestGroovy") {
    val compileKotlin = tasks.named<KotlinCompile>("compileTestKotlin")
    dependsOn(compileKotlin)
    classpath += files(compileKotlin.get().destinationDir)
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    implementation(kotlin("stdlib", kotlin_version))
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-jackson:$ktor_version")
    implementation("com.google.inject:guice:4.2.2")
    implementation("com.opentable.components:otj-pg-embedded:0.13.3")
    implementation("com.zaxxer:HikariCP:3.4.2")
    implementation("org.flywaydb:flyway-core:6.2.2")
    implementation("ch.qos.logback:logback-classic:1.2.3")
    testImplementation("org.spockframework:spock-core:2.0-M2-groovy-3.0")
    testImplementation("org.spockframework:spock-guice:2.0-M2-groovy-3.0")
    testImplementation("io.ktor:ktor-client:$ktor_version")
    testImplementation("io.ktor:ktor-client-apache:$ktor_version")
    testImplementation("io.ktor:ktor-client-logging-jvm:$ktor_version")
    testImplementation("io.ktor:ktor-client-jackson:$ktor_version")
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to mainClass
        )
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory()) it else zipTree(it) })
}

