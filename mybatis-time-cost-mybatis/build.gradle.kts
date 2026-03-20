plugins {
    `java-library`
    `maven-publish`
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    api("org.mybatis:mybatis:3.5.15")
    implementation("com.google.code.gson:gson:2.10.1")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "com.mybatis.timecost"
            artifactId = "mybatis-time-cost-mybatis"
            version = "0.1.0"
        }
    }
}

