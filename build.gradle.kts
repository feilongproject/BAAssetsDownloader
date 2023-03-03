plugins {
    id("com.android.application") version "7.3.1" apply false
    id("com.android.library") version "7.3.1" apply false
    id("com.google.dagger.hilt.android") version "2.45" apply false
    kotlin("android") version "1.8.10" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}