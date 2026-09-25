// Build logic: the live template generator (src/templates -> HTML + JSX live template XML).
// Its unit tests run with: ./gradlew -p buildSrc test
plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
