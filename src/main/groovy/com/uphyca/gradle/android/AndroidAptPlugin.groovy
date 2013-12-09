// This plugin is based on https://bitbucket.org/hvisser/android-apt and https://github.com/zaki50/android_gradle_template
package com.uphyca.gradle.android

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import com.android.builder.BuilderConstants
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.compile.JavaCompile
import com.squareup.gradle.android.AndroidTestPlugin

class AndroidAptPlugin implements Plugin<Project> {

    private static final String GENERATED_SOURCE_DIR = "source/generated"
    private static final String INSTRUMENT_TEST_TASK_NAME = 'instrumentTest'
    private static final String INSTRUMENT_TEST_SOURCE_SET_NAME = 'instrumentTest'
    private static final String TEST_TASK_NAME = 'test'
    private static final String TEST_SOURCE_SET_NAME = 'test'

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

        def instrumentTestAptConfiguration = project.configurations.create(INSTRUMENT_TEST_TASK_NAME + 'Apt')
        instrumentTestAptConfiguration.extendsFrom project.configurations.getByName(INSTRUMENT_TEST_TASK_NAME + 'Compile')

        project.afterEvaluate {
            variants.all { variant ->
                def configuration = 'apt'
                def processorPath = project.configurations[configuration].asPath
                applyApt(project, variant, new File(variant.dirName).getName(), processorPath)
            }
            variants.testVariant.findAll { it }.each { variant ->
                def configuration = "${INSTRUMENT_TEST_TASK_NAME}Apt"
                def processorPath = project.configurations["apt"].asPath + File.pathSeparator + project.configurations[configuration].asPath
                applyApt(project, variant, INSTRUMENT_TEST_SOURCE_SET_NAME, processorPath)
            }
            
            def hasTestPlugin = project.plugins.hasPlugin AndroidTestPlugin
            if (!hasTestPlugin) {
                return;
            }

            def testAptConfiguration = project.configurations.create(TEST_TASK_NAME + 'Apt')
            testAptConfiguration.extendsFrom project.configurations.getByName(TEST_TASK_NAME + 'Compile')

            variants.testVariant.findAll { it }.each { variant ->
                if (variant.buildType.name.equals(BuilderConstants.RELEASE)) {
                    return;
                }
                def configuration = "${TEST_TASK_NAME}Apt"
                def processorPath = project.configurations["apt"].asPath + File.pathSeparator + project.configurations[configuration].asPath
                applyTestApt(project, variant, processorPath)
            }
        }
    }

    def applyApt(project, variant, sourceSet, processorPath) {
        def aptOutputDir = project.file(new File(project.buildDir, GENERATED_SOURCE_DIR))
        def aptOutput = new File(aptOutputDir, variant.dirName)

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

    def applyTestApt(project, variant, processorPath) {
        def aptOutputDir = project.file(new File(project.buildDir, GENERATED_SOURCE_DIR))
        def aptOutput = new File(aptOutputDir, variant.dirName)

        //taken from https://github.com/JakeWharton/gradle-android-test-plugin

        JavaPluginConvention javaConvention = project.convention.getPlugin JavaPluginConvention
        def buildTypeName = variant.buildType.name.capitalize()
        def projectFlavorNames = variant.productFlavors.collect { it.name.capitalize() }
        // TODO support flavor groups... ugh
        if (projectFlavorNames.isEmpty()) {
            projectFlavorNames = [""]
        }

        def projectFlavorName = projectFlavorNames.join()
        def variationName = "$projectFlavorName$buildTypeName"

        SourceSet variationSources = javaConvention.sourceSets["$TEST_TASK_NAME$variationName"]
        variationSources.java.srcDirs += aptOutput.getPath()

        def testCompileTask = project.tasks.getByName variationSources.compileJavaTaskName
        testCompileTask.options.compilerArgs += [
                '-processorpath', processorPath,
                '-s', aptOutput
        ]
        testCompileTask.source = testCompileTask.source.filter { p ->
            return !p.getPath().startsWith(aptOutputDir.path)
        }
        testCompileTask.doFirst {
            aptOutput.mkdirs()
        }
    }
}