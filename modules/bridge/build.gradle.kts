/*
 * Copyright 2019-2022 CloudNetService team & contributors
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

import net.fabricmc.loom.task.RemapJarTask
import net.kyori.blossom.BlossomExtension

plugins {
  alias(libs.plugins.fabricLoom)
}

configurations {
  // custom configuration for later dependency resolution
  create("runtimeImpl") {
    configurations.getByName("api").extendsFrom(this)
  }
}

dependencies {
  "compileOnly"(projects.cloudnetWrapperJvm)
  "compileOnly"(libs.bundles.proxyPlatform)
  "compileOnly"(libs.bundles.serverPlatform)

  "annotationProcessor"(libs.velocity)
  "runtimeImpl"(libs.bundles.adventure)
  "runtimeImpl"(projects.cloudnetExt.adventureHelper)

  "minecraft"(libs.minecraft)
  "modImplementation"(libs.fabricLoader)
  "mappings"(loom.officialMojangMappings())
}

tasks.withType<RemapJarTask> {
  // depend on adventure helper jar task
  dependsOn(":cloudnet-ext:adventure-helper:jar")
  // base setup
  archiveFileName.set(Files.bridge)
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  // includes all dependencies of runtimeImpl but excludes gson because we don't need it
  from(configurations.getByName("runtimeImpl").map { if (it.isDirectory) it else zipTree(it) })
  exclude {
    it.file.absolutePath.contains(setOf("com", "google", "gson").joinToString(separator = File.separator))
  }
}

loom {
  accessWidenerPath.set(project.file("src/main/resources/cloudnet_bridge.accesswidener"))
}

moduleJson {
  name = "CloudNet-Bridge"
  author = "CloudNetService"
  main = "eu.cloudnetservice.modules.bridge.node.CloudNetBridgeModule"
  description = "Bridges service software support between all supported versions for easy CloudNet plugin development"
}

configure<BlossomExtension> {
  replaceToken("{project.build.version}", project.version)
}
