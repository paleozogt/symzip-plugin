package org.paleozogt.gradle.infozip

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.BuildLauncher;

import org.junit.Test
import static org.junit.Assert.*

class PreprocessorPluginTest {

    @Test
    public void applyTest() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'org.paleozogt.infozip'
    }

    @Test
    public void sampleBuildTest() {
        GradleConnector connector = GradleConnector.newConnector()
        connector.forProjectDirectory(new File("src/test/resources/test-build"))
        ProjectConnection connection = connector.connect()
        try {
            BuildLauncher launcher = connection.newBuild()
            launcher.forTasks("build")
            launcher.run()
        } finally {
            connection.close()
        }
    }
}
