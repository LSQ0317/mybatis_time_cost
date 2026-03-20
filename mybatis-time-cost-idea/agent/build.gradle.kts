plugins {
    `java-library`
}

group = "com.mybatis.timecost"
version = rootProject.version

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withSourcesJar()
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.bytebuddy:byte-buddy:1.14.18")
    implementation("com.google.code.gson:gson:2.10.1")
    compileOnly("org.mybatis:mybatis:3.5.15")
}

tasks.jar {
    archiveBaseName.set("mybatis-time-cost-javaagent")
    manifest {
        attributes(
            "Premain-Class" to "com.mybatis.timecost.agent.MybatisTimeCostAgent",
            "Can-Redefine-Classes" to "true",
            "Can-Retransform-Classes" to "true",
            "Class-Path" to configurations.runtimeClasspath.get()
                .filter { it.name != archiveFileName.get() }
                .joinToString(" ") { it.name }
        )
    }
}
