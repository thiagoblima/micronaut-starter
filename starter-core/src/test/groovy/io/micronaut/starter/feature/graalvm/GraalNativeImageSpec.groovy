package io.micronaut.starter.feature.graalvm

import io.micronaut.starter.BeanContextSpec
import io.micronaut.starter.application.ApplicationType
import io.micronaut.starter.feature.build.gradle.templates.buildGradle
import io.micronaut.starter.feature.build.maven.templates.pom
import io.micronaut.starter.fixture.CommandOutputFixture
import io.micronaut.starter.options.BuildTool
import io.micronaut.starter.options.Language
import io.micronaut.starter.options.Options
import io.micronaut.starter.options.TestFramework
import spock.lang.Shared
import spock.lang.Subject
import spock.lang.Unroll

class GraalNativeImageSpec extends BeanContextSpec implements CommandOutputFixture {

    @Subject
    @Shared
    GraalNativeImage graalNativeImage = beanContext.getBean(GraalNativeImage)

    @Unroll("feature graalvm works for application type: #description")
    void "feature graalvm works for every type of application type"(ApplicationType applicationType,
                                                                    String description) {
        expect:
        graalNativeImage.supports(applicationType)

        where:
        applicationType << ApplicationType.values()
        description = applicationType.name
    }

    void 'test gradle graalvm feature'() {
        when:
        String template = buildGradle.template(buildProject(), getFeatures(["graalvm"])).render().toString()

        then:
        template.contains('annotationProcessor(platform("io.micronaut:micronaut-bom:\$micronautVersion"))')
        template.contains('annotationProcessor("io.micronaut:micronaut-graal")')
        template.contains('compileOnly(platform("io.micronaut:micronaut-bom:\$micronautVersion"))')
        template.contains('compileOnly("org.graalvm.nativeimage:svm")')

        when:
        template = buildGradle.template(buildProject(), getFeatures(["graalvm"], Language.KOTLIN)).render().toString()

        then:
        template.contains('kapt(platform("io.micronaut:micronaut-bom:\$micronautVersion"))')
        template.contains('kapt("io.micronaut:micronaut-graal")')
        template.contains('compileOnly(platform("io.micronaut:micronaut-bom:\$micronautVersion"))')
        template.contains('compileOnly("org.graalvm.nativeimage:svm")')

        when:
        template = buildGradle.template(buildProject(), getFeatures(["graalvm"], Language.GROOVY)).render().toString()

        then:
        template.count('compileOnly(platform("io.micronaut:micronaut-bom:\$micronautVersion"))') == 1
        template.contains('compileOnly("org.graalvm.nativeimage:svm")')
    }

    void 'test maven graalvm feature'() {
        when:
        String template = pom.template(buildProject(), getFeatures(["graalvm"]), []).render().toString()

        then:
        template.contains("""
    <dependency>
      <groupId>org.graalvm.nativeimage</groupId>
      <artifactId>svm</artifactId>
      <scope>provided</scope>
    </dependency>
""")
        template.contains("""
            <path>
              <groupId>io.micronaut</groupId>
              <artifactId>micronaut-graal</artifactId>
              <version>\${micronaut.version}</version>
            </path>
""")

        when:
        template = pom.template(buildProject(), getFeatures(["graalvm"], Language.KOTLIN), []).render().toString()

        then:
        template.contains("""
    <dependency>
      <groupId>org.graalvm.nativeimage</groupId>
      <artifactId>svm</artifactId>
      <scope>provided</scope>
    </dependency>
""")
        template.contains("""
                <annotationProcessorPath>
                  <groupId>io.micronaut</groupId>
                  <artifactId>micronaut-graal</artifactId>
                  <version>\${micronaut.version}</version>
                </annotationProcessorPath>
""")

        when:
        template = pom.template(buildProject(), getFeatures(["graalvm"], Language.GROOVY), []).render().toString()

        then:
        template.contains("""
    <dependency>
      <groupId>org.graalvm.nativeimage</groupId>
      <artifactId>svm</artifactId>
      <scope>provided</scope>
    </dependency>
""")
        template.contains("""
    <dependency>
      <groupId>io.micronaut</groupId>
      <artifactId>micronaut-graal</artifactId>
      <scope>provided</scope>
    </dependency>
""")
    }

    @Unroll
    void 'verify dockerfile for a default application type with maven and feature graalvm for language=#language'() {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, TestFramework.JUNIT, BuildTool.MAVEN),
                ['graalvm']
        )
        String dockerfile = output['Dockerfile']

        then:
        dockerfile
        dockerfile.contains('RUN native-image --no-server -cp target/foo-*.jar')
        dockerfile.contains('COPY --from=graalvm /home/app/foo/foo /app/foo')
        dockerfile.contains('ENTRYPOINT ["/app/foo"]')

        and: 'defaults to graalvm ce jdk8 image'
        dockerfile.contains('FROM oracle/graalvm-ce:20.0.0-java8 as graalvm')
        dockerfile.contains('#FROM oracle/graalvm-ce:20.0.0-java11 as graalvm')

        where:
        language << Language.values().toList()
        extension << Language.extensions()
        srcDir << Language.srcDirs()
        testSrcDir << Language.testSrcDirs()
    }

    @Unroll
    void 'verify dockerfile for a default application type with gradle and feature graalvm for language=#language'() {
        when:
        def output = generate(
                ApplicationType.DEFAULT,
                new Options(language, TestFramework.JUNIT, BuildTool.GRADLE),
                ['graalvm']
        )
        String dockerfile = output['Dockerfile']

        then:
        dockerfile
        dockerfile.contains('FROM oracle/graalvm-ce:20.0.0-java8 as graalvm')
        dockerfile.contains('RUN native-image --no-server -cp build/libs/foo-*-all.jar')
        dockerfile.contains('COPY --from=graalvm /home/app/foo/foo /app/foo')
        dockerfile.contains('ENTRYPOINT ["/app/foo"]')

        and: 'defaults to graalvm ce jdk8 image'
        dockerfile.contains('FROM oracle/graalvm-ce:20.0.0-java8 as graalvm')
        dockerfile.contains('#FROM oracle/graalvm-ce:20.0.0-java11 as graalvm')

        where:
        language << Language.values().toList()
        extension << Language.extensions()
        srcDir << Language.srcDirs()
        testSrcDir << Language.testSrcDirs()
    }
}
