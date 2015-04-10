package org.paleozogt.gradle.infozip

import org.gradle.api.Project
import org.gradle.api.Plugin

class InfoZipPlugin implements Plugin<Project> {
    void apply(Project project) {
        applyExtension(project)
        applyTasks(project)
    }    

    void applyExtension(Project project) {
    }
    
    void applyTasks(Project project) {
    }
}
