plugins {
    id("java")
    id("application")
}

group = "labs.lab7.server"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":Common"))
    implementation("org.postgresql:postgresql:42.6.0")
    implementation("org.hibernate:hibernate-core:6.1.5.Final")
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass.set("labs.lab7.server.Main")
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "labs.lab7.server.Main"
    }
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
