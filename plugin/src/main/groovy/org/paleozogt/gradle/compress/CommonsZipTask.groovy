package org.paleozogt.gradle.compress

import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.tasks.TaskAction
import org.gradle.api.GradleException;
import org.gradle.api.tasks.WorkResult;

import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.file.FileCopyDetails;

import java.nio.file.Path;
import java.nio.file.Files;

import org.slf4j.Logger

class CommonsZipTask extends AbstractArchiveTask {
    public CommonsZipTask() {
        getLogger().lifecycle("InfoZipTask ctor");
    }

    @Override
    protected CopyAction createCopyAction() {
        getLogger().lifecycle("createCopyAction");
        return new ZipCopyAction(getArchivePath());
    }

    public class ZipCopyAction implements CopyAction {
        private final File zipFile;

        public ZipCopyAction(File zipFile) {
            this.zipFile = zipFile;
        }

        public WorkResult execute(final CopyActionProcessingStream stream) {
            getLogger().lifecycle("execute");
            stream.process(new StreamAction());
            return new SimpleWorkResult(true);
        }

        private class StreamAction implements CopyActionProcessingStreamAction {
            public StreamAction() {
            }

            public void processFile(FileCopyDetailsInternal details) {
                if (Files.isSymbolicLink(details.getFile().toPath())) {
                    getLogger().lifecycle("processFile {} (symlink)", details);
                } else if (details.isDirectory()) {
                    getLogger().lifecycle("processFile {} (dir)", details);
                } else {
                    getLogger().lifecycle("processFile {} (file)", details);
                }
            }
        }
    }
}
