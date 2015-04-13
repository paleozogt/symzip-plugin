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

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.ArchiveEntry;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.charset.Charset;

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
            this.zipFile= zipFile;
        }

        public WorkResult execute(final CopyActionProcessingStream stream) {
            getLogger().lifecycle("execute");

            ZipArchiveOutputStream zipOutStr= new ZipArchiveOutputStream(zipFile);
            zipOutStr.setEncoding("UTF8");
            zipOutStr.setUseLanguageEncodingFlag(true);
            zipOutStr.setCreateUnicodeExtraFields(UnicodeExtraFieldPolicy.ALWAYS);
            zipOutStr.setFallbackToUTF8(true);

            stream.process(new StreamAction(zipOutStr));
            zipOutStr.close();
            return new SimpleWorkResult(true);
        }

        private class StreamAction implements CopyActionProcessingStreamAction {
            private final ZipArchiveOutputStream zipOutStr;
            private visitedSymLinks= [];

            public StreamAction(ZipArchiveOutputStream zipOutStr) {
                this.zipOutStr = zipOutStr;
            }

            public void processFile(FileCopyDetailsInternal details) {
                if (isSymLink(details)) {
                    visitSymLink(details);
                } else if (!details.isDirectory() && !isChildOfVisitedSymlink(details)) {
                    visitFile(details);
                }
            }

            private Boolean isSymLink(FileCopyDetails fileDetails) {
                return Files.isSymbolicLink(fileDetails.getFile().toPath());
            }

            private Boolean isChildOfVisitedSymlink(FileCopyDetails fileDetails) {
                File file= fileDetails.getFile();
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

            private void visitFile(FileCopyDetails fileDetails) {
                try {
                    getLogger().lifecycle("visitFile {}", fileDetails);
                    ZipArchiveEntry archiveEntry= (ZipArchiveEntry)zipOutStr.createArchiveEntry(fileDetails.getFile(), fileDetails.getRelativePath().getPathString());
                    archiveEntry.setTime(fileDetails.getLastModified());
                    archiveEntry.setUnixMode(UnixStat.DEFAULT_FILE_PERM | fileDetails.getMode());
                    zipOutStr.putArchiveEntry(archiveEntry);
                    fileDetails.copyTo(zipOutStr);
                    zipOutStr.closeArchiveEntry();
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile), e);
                }
            }

            protected void visitSymLink(FileCopyDetails fileDetails) {
                try {
                    visitedSymLinks.add(fileDetails.getFile());
                    Path link= Files.readSymbolicLink(fileDetails.getFile().toPath());
                    getLogger().lifecycle("visitSymLink {} (symlink->{})", fileDetails, link);

                    ZipArchiveEntry archiveEntry= (ZipArchiveEntry)zipOutStr.createArchiveEntry(fileDetails.getFile(), fileDetails.getRelativePath().getPathString());
                    archiveEntry.setTime(fileDetails.getLastModified());
                    archiveEntry.setUnixMode(UnixStat.DEFAULT_LINK_PERM | UnixStat.LINK_FLAG);
                    zipOutStr.putArchiveEntry(archiveEntry);
                    zipOutStr.write(link.toString().getBytes(Charset.forName("UTF-8")));
                    zipOutStr.closeArchiveEntry();
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile), e);
                }
            }
        }
    }
}
