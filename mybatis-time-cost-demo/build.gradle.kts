plugins {
    java
    id("org.springframework.boot") version "3.3.3"
    id("io.spring.dependency-management") version "1.1.6"
}

import org.springframework.boot.gradle.tasks.run.BootRun

group = "com.mybatis.timecost"
version = "0.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.3")
    implementation("com.mybatis.timecost:mybatis-time-cost-mybatis:0.1.0")
    runtimeOnly("com.h2database:h2")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}

tasks.named<BootRun>("bootRun") {
    jvmArgs = listOf("-Xms256m", "-Xmx1024m", "-Dfile.encoding=UTF-8")
}
