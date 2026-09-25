import com.jamesmosquera.bootstraptoolkit.generator.GenerateLiveTemplates
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.intellij.platform")
    id("org.jetbrains.changelog")
}

dependencies {
    testImplementation("junit:junit:4.13.2")

    // IntelliJ Platform Gradle Plugin Dependencies Extension - read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
    intellijPlatform {
        intellijIdea("2025.2.6.2")
        testFramework(TestFrameworkType.Platform)
    }
}

// Live templates are generated from the single source in src/templates (see buildSrc):
// one HTML/Vue group (class) and one JSX/TSX group (className). Never edit the generated XML.
val generateLiveTemplates by tasks.registering(GenerateLiveTemplates::class) {
    sourceDir = layout.projectDirectory.dir("src/templates")
    outputDir = layout.buildDirectory.dir("generated/liveTemplates")
}

sourceSets.main {
    resources.srcDir(generateLiveTemplates)
}

// Plugin signing: https://plugins.jetbrains.com/docs/intellij/plugin-signing.html
// Keys never live in the repo. CI supplies them as environment variables (see release.yml);
// locally they are read from ~/.bootstrap-toolkit-signing/ (chain.crt, private.pem, password.txt).
val signingDir = File(System.getProperty("user.home"), ".bootstrap-toolkit-signing")

intellijPlatform {
    signing {
        if (providers.environmentVariable("CERTIFICATE_CHAIN").isPresent) {
            certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
            privateKey = providers.environmentVariable("PRIVATE_KEY")
            password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
        } else if (File(signingDir, "private.pem").exists()) {
            certificateChainFile = File(signingDir, "chain.crt")
            privateKeyFile = File(signingDir, "private.pem")
            password = providers.fileContents(layout.file(providers.provider { File(signingDir, "password.txt") }))
                .asText.map { it.trim() }
        }
    }
}
