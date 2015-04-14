package org.paleozogt.gradle.zip

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.BuildLauncher;

import org.junit.Test
import static org.junit.Assert.*

import org.slf4j.Logger
import org.gradle.api.logging.Logging

class SymZipPluginTest {
    private Logger logger= Logging.getLogger(getClass());

    @Test
    public void applyTest() {
        Project project = ProjectBuilder.builder().build()
        project.apply plugin: 'org.paleozogt.symzip'
    }

    @Test
    public void zipTask() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('testTask', type: SymZip)
        assertTrue(task instanceof SymZip)
        task.execute();
    }

    @Test
    public void unzipTask() {
        Project project = ProjectBuilder.builder().build()
        def task = project.task('testTask', type: SymUnzip)
        assertTrue(task instanceof SymUnzip)
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
