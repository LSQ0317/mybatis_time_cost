plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.mybatis.timecost"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(8)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
    runtimeOnly(project(":agent"))
}

intellij {
    version.set("2020.1")
    type.set("IC")
    plugins.set(listOf("java"))
}

tasks {
    buildSearchableOptions {
        enabled = false
    }

    runIde {
        minHeapSize = "512m"
        maxHeapSize = "2048m"
        jvmArgs("-Dfile.encoding=UTF-8")
    }

    patchPluginXml {
        sinceBuild.set("201")
        untilBuild.set("")
        changeNotes.set("""
            第一阶段（无 UI）：接收 SQL、打印日志、自动复制、显示耗时
        """.trimIndent())
    }
}
