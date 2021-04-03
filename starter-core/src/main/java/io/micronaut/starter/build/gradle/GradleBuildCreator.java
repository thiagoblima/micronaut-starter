/*
 * Copyright 2020 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.starter.build.gradle;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.starter.application.generator.GeneratorContext;
import javax.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class GradleBuildCreator {

    @NonNull
    public GradleBuild create(@NonNull GeneratorContext generatorContext) {
        GradleDsl gradleDsl = generatorContext
                .getBuildTool()
                .getGradleDsl()
                .orElseThrow(() -> new IllegalArgumentException("GradleBuildCreator can only create Gradle builds"));
        List<? extends GradlePlugin> gradlePlugins = generatorContext.getBuildPlugins()
                .stream()
                .filter(buildPlugin -> buildPlugin.getBuildTool().isGradle() && buildPlugin.getId() != null)
                .sorted(OrderUtil.COMPARATOR)
                .map(buildPlugin -> {
                    if (buildPlugin.getVersion() == null) {
                        return new CoreGradlePlugin(buildPlugin.getId());
                    }
                    return new CommunityGradlePlugin(buildPlugin.getId(), buildPlugin.getVersion(), buildPlugin.getExtension());
                }).collect(Collectors.toList());
        return new GradleBuild(gradleDsl, resolveDependencies(generatorContext), gradlePlugins);
    }

    @NonNull
    private List<GradleDependency> resolveDependencies(@NonNull GeneratorContext generatorContext) {
        return generatorContext.getDependencies()
                .stream()
                .map(dep -> new GradleDependency(dep, generatorContext))
                .sorted(GradleDependency.COMPARATOR)
                .collect(Collectors.toList());
    }
}
