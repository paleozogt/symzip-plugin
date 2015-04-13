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
        private visitedSymLinks= [];

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
                    visitedSymLinks.add(details.getFile());
                } else {
                    getLogger().lifecycle("processFile {} (dir={}, childOfSymlink={})", details, details.isDirectory(), isChildOfVisitedSymlink(details.getFile()));
                }
            }
        }

        private Boolean isChildOfVisitedSymlink(File file) {
            for (File symLink : visitedSymLinks) {
                if (isChildOf(symLink, file)) return true;
            }
            return false;
        }

        private Boolean isChildOf(File dir, File file) {
            File parent= file.getParentFile();
            while (parent != null) {
                if (dir.toString() == parent.toString()) return true;
                parent= parent.getParentFile();
            }
            return false;
        }
    }
}
