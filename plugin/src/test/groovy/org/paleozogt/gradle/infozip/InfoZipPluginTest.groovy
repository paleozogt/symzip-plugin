package org.paleozogt.gradle.infozip

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.BuildLauncher;

import org.junit.Test
import static org.junit.Assert.*

class InfoZipPluginTest {

    @Test
    public void applyTest() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'org.paleozogt.infozip'
    }

    @Test
    public void infoZipTask() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('testTask', type: InfoZipTask)
        assertTrue(task instanceof InfoZipTask)
    }

    @Test
    public void infoUnzipTask() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('testTask', type: InfoUnzipTask)
        assertTrue(task instanceof InfoUnzipTask)
    }

    @Test
    public void sampleBuildTest() {
        runBuild(new File("src/test/resources/test-build"))
    }

    protected void runBuild(File path, String target = "build") {
        GradleConnector connector = GradleConnector.newConnector()
        connector.forProjectDirectory(path)
        ProjectConnection connection = connector.connect()
        try {
            BuildLauncher launcher = connection.newBuild()
            launcher.forTasks(target)
            launcher.run()
        } finally {
            connection.close()
        }
    }
}
