package com.hanami.plugin2

import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "------LifeCycle plugin entrance-------"

        def android = project.extensions.android

        project.plugins.all {
            if(it instanceof AppPlugin) {
                android.registerTransform(new TestTransform())
            }
        }




        /**
         * test
         */
        project.extensions.create("testPlugin", TestPluginExtension)

        project.task('hello-q') {
            doLast {
                println("Hello ${project.testPlugin.filePath}")
            }
        }

    }
}
