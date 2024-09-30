plugins {
    id("java")
    id("application")
}

group = "labs.lab7.client"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Common"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("labs.lab7.client.utility.Runner")
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Main-Class" to "labs.lab7.client.utility.Runner"
        )
    }
}
