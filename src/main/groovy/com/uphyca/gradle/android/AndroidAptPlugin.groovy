// This plugin is based on https://bitbucket.org/hvisser/android-apt and https://github.com/zaki50/android_gradle_template
package com.uphyca.gradle.android

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BasePlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

class AndroidAptPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def hasAppPlugin = project.plugins.hasPlugin AppPlugin
        def hasLibraryPlugin = project.plugins.hasPlugin LibraryPlugin
        def log = project.logger

        // Ensure the Android plugin has been added in app or library form, but not both.
        if (!hasAppPlugin && !hasLibraryPlugin) {
            throw new IllegalStateException("The 'android' or 'android-library' plugin is required.")
        } else if (hasAppPlugin && hasLibraryPlugin) {
            throw new IllegalStateException(
                    "Having both 'android' and 'android-library' plugin is not supported.")
        }

        def variants = hasAppPlugin ?
                project.android.applicationVariants :
                project.android.libraryVariants

        def aptConfiguration = project.configurations.create('apt')
        aptConfiguration.extendsFrom project.configurations.getByName('compile')

        project.afterEvaluate {
            variants.all { variant ->

                def aptOutputDir = project.file(new File(project.buildDir, "source/generated"))
                def aptOutput = new File(aptOutputDir, variant.dirName)
                def sourceSet = new File(variant.dirName).getName()

                log.debug("----------------------------------------")
                log.debug("apt output dir: $aptOutputDir")
                log.debug("apt output: $aptOutput")
                log.debug("source set: $sourceSet")
                log.debug("----------------------------------------")

                project.android.sourceSets[sourceSet].java.srcDirs += aptOutput.getPath()

                JavaCompile javaCompile = variant.javaCompile

                javaCompile.options.compilerArgs += [
                        '-processorpath', project.configurations.apt.getAsPath(),
                        '-s', aptOutput
                ]
                javaCompile.source = variant.javaCompile.source.filter { p ->
                    return !p.getPath().startsWith(aptOutputDir.getPath())
                }

                javaCompile.doFirst {
                    aptOutput.mkdirs()
                }
            }
        }
    }
}