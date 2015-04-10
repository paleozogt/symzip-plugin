package org.paleozogt.gradle.infozip

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

import org.slf4j.Logger

class InfoZipTask extends AbstractArchiveTask {
    public InfoZipTask() {
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
                if (details.isDirectory()) {
                    visitDir(details);
                } else {
                    visitFile(details);
                }
            }

            private void visitFile(FileCopyDetails fileDetails) {
                try {
                    getLogger().lifecycle("visitFile {}", fileDetails);
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile), e);
                }
            }

            private void visitDir(FileCopyDetails dirDetails) {
                try {
                    getLogger().lifecycle("visitDir {}", dirDetails);
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not add %s to ZIP '%s'.", dirDetails, zipFile), e);
                }
            }
        }
    }
}
