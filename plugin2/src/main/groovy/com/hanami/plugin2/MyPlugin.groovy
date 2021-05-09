package com.hanami.plugin2

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.tasks.ManifestProcessorTask
import com.hanami.plugin2.tool.Logger
import org.gradle.api.Plugin
import org.gradle.api.Project

class MyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "------LifeCycle plugin entrance-------"

        project.extensions.create("config", TransformConfig)


        def android = project.extensions.android


        project.plugins.all {
            if(it instanceof AppPlugin) {
                android.applicationVariants.all { variant ->
                    variant.outputs.all { output ->
                        ManifestProcessorTask manifestProcessorTask = output.processManifestProvider.getOrNull()
                        manifestProcessorTask.doLast { ManifestProcessorTask task ->
                            def directory = task.getBundleManifestOutputDirectory()
                            def manifestPath = "$directory/AndroidManifest.xml"
                            def transformConfigPath = project.extensions.config.configPath
                            Logger.i("transform config path is " + transformConfigPath)
                            TransformConfig config = TransformConfig.parse(transformConfigPath)
                            ClassHandler.getInstance().initConfig(config)
                        }

                    }
                }

                android.registerTransform(new TestTransform())
            }
        }

    }
}
