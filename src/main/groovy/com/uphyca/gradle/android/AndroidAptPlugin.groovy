// This plugin is based on https://bitbucket.org/hvisser/android-apt and https://github.com/zaki50/android_gradle_template
package com.uphyca.gradle.android

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class AndroidAptPlugin implements Plugin<Project> {

    private static final String GENERATED_SOURCE_DIR = "source/generated"
    private static final String INSTRUMENT_TEST_TASK_NAME = 'instrumentTest'
    private static final String INSTRUMENT_TEST_SOURCE_SET_NAME = 'instrumentTest'

    @Override
    void apply(Project project) {
        def hasAppPlugin = project.plugins.hasPlugin AppPlugin
        def hasLibraryPlugin = project.plugins.hasPlugin LibraryPlugin

        // Ensure the Android plugin has been added in app or library form, but not both.
        if (!hasAppPlugin && !hasLibraryPlugin) {
            throw new IllegalStateException("The 'android' or 'android-library' plugin is required.")
        } else if (hasAppPlugin && hasLibraryPlugin) {
            throw new IllegalStateException(
                    "Having both 'android' and 'android-library' plugin is not supported.")
        }

        def variants = hasAppPlugin ?
                project.android.applicationVariants : project.android.libraryVariants

        def aptConfiguration = project.configurations.create('apt')
        aptConfiguration.extendsFrom project.configurations.getByName('compile')

        def testAptConfiguration = project.configurations.create(INSTRUMENT_TEST_TASK_NAME + 'Apt')
        testAptConfiguration.extendsFrom project.configurations.getByName(INSTRUMENT_TEST_TASK_NAME + 'Compile')

        project.afterEvaluate {
            variants.all { variant ->
                def configuraton = 'apt'
                applyApt(project, variant, new File(variant.dirName).getName(), configuraton, project.configurations[configuraton].asPath)
            }
            variants.testVariant.findAll { it }.each { variant ->
                def configuration = "${INSTRUMENT_TEST_TASK_NAME}Apt"
                applyApt(project, variant, INSTRUMENT_TEST_SOURCE_SET_NAME, configuration, project.configurations["apt"].asPath + File.pathSeparator + project.configurations[configuration].asPath)
            }
        }
    }

    def applyApt(project, variant, sourceSet, configuration, processorPath) {
        def log = project.logger
        def aptOutputDir = project.file(new File(project.buildDir, GENERATED_SOURCE_DIR))
        def aptOutput = new File(aptOutputDir, variant.dirName)

        log.debug("----------------------------------------")
        log.debug("Variant: $variant.name")
        log.debug("Output directory: $aptOutput")
        log.debug("Source set: $sourceSet")
        log.debug("----------------------------------------")

        project.android.sourceSets[sourceSet].java.srcDirs += aptOutput.getPath()

        JavaCompile javaCompile = variant.javaCompile

        javaCompile.options.compilerArgs += [
                '-processorpath', processorPath,
                '-s', aptOutput
        ]

        javaCompile.source = javaCompile.source.filter { p ->
            return !p.getPath().startsWith(aptOutputDir.path)
        }

        javaCompile.doFirst {
            aptOutput.mkdirs()
        }
    }
}