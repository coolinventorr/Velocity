plugins {
    `java-library`
}

dependencies {
    compileOnly(project(":velocity-api"))
    annotationProcessor(project(":velocity-api"))
    implementation(libs.nightconfig)
}
