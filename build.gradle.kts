import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `maven-publish`
    id("org.jetbrains.kotlin.jvm") version "1.8.20"
}

group = "me.modmuss50"
version = "0.0.1"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
}

tasks.withType(KotlinCompile::class.java).all {
    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType(JavaCompile::class.java).all {
    options.release = 17
}

gradlePlugin {
    plugins.create("mod-publish-plugin") {
        id = "me.modmuss50.mod-publish-plugin"
        implementationClass = "me.modmuss50.mpp.MppPlugin"
    }
}