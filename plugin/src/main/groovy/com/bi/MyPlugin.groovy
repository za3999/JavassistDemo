package com.bi

import org.gradle.api.Plugin
import org.gradle.api.Project


class MyPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        println "this is my custom plugin MyPlugin"

        project.android.registerTransform(new MyPluginTransform(project))
    }

}