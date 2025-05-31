import java.nio.file.Files

plugins {
    base
}

val frontendBinary = "frontend"

tasks.register("createCliSymlink") {
    val distDir = layout.buildDirectory.dir("distribution").get().asFile.toPath()
    val cliScript = layout.buildDirectory.dir("distribution/lang-compiler/bin").get().file("lang-compiler").asFile.toPath()
    val symlink = distDir.resolve("waki")

    doLast {
        if (Files.exists(symlink)) {
            Files.delete(symlink)
        }
        Files.createDirectories(distDir)
        Files.createSymbolicLink(symlink, cliScript)
    }
}

tasks.register<Exec>("buildFrontend") {
    group = "build"
    description = "Build frontend using Makefile"
    workingDir = file("$projectDir/lang-frontend")
    commandLine("make")
    outputs.file("$projectDir/lang-frontend/$frontendBinary")
}

tasks.named("build") {
    dependsOn("buildFrontend")
}

tasks.register<Copy>("dist") {
    group = "distribution"
    description = "Collect all build artifacts into one folder"

    val distDir = layout.buildDirectory.dir("distribution")

    into(distDir)

    from("lang-frontend/compiler") {
        into("lang-frontend")
    }

    from("lang-frontend/input/stdlib.sasm") {
        into("asm")
    }

    from(project(":lang-compiler").layout.buildDirectory.dir("install/lang-compiler")) {
        into("lang-compiler")
    }

    from(project(":lang-backend").layout.buildDirectory.dir("install/lang-backend")) {
        into("lang-backend")
    }

    from(project(":asm").layout.buildDirectory.dir("install/asm")) {
        into("asm")
    }

    from(project(":comp").layout.buildDirectory.dir("install/comp")) {
        into("comp")
    }
}

tasks.named("dist") {
    dependsOn("build")
    dependsOn("asm:installDist")
    dependsOn("comp:installDist")
    dependsOn("lang-backend:installDist")
    dependsOn("lang-compiler:installDist")
    dependsOn("createCliSymlink")
}
