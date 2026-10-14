/*
 * Copyright (c) 2026 Vivek Umrao.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
    id("java")
    id("jacoco")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

subprojects {
    val libs = rootProject.libs
    apply(plugin = "java")
    apply(plugin = "jacoco")

    extra["commons-lang3.version"] = libs.versions.commons.lang3.get()
    extra["assertj.version"] = libs.versions.assertj.get()

    dependencies {
        constraints {
            add("implementation", libs.commons.lang3)
            add("implementation", libs.commons.compress)
            add("implementation", libs.commons.io)
            add("implementation", libs.commons.beanutils)
            add("implementation", libs.json.smart)
            add("implementation", libs.jose4j)
            add("implementation", libs.zookeeper)
            add("implementation", libs.zookeeper.jute)
            add("implementation", libs.zstd.jni)
            add("implementation", libs.snappy.java)
            add("implementation", libs.assertj.core)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        finalizedBy(tasks.withType<JacocoReport>())
    }

    tasks.withType<JacocoReport>().configureEach {
        dependsOn(tasks.withType<Test>())
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }
}
