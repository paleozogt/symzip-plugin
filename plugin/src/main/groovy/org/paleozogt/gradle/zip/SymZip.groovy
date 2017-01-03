package org.paleozogt.gradle.zip

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

class SymZip extends AbstractArchiveTask {
    public CommonsZipTask() {
    }

    @Override
    protected CopyAction createCopyAction() {
        return new ZipCopyAction(getArchivePath());
    }

    public class ZipCopyAction implements CopyAction {
        private final File zipFile;

        public ZipCopyAction(File zipFile) {
            this.zipFile= zipFile;
        }

        public WorkResult execute(final CopyActionProcessingStream stream) {
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
                getLogger().debug("processFile {}", details);
                if (!isChildOfVisitedSymlink(details)) {
                    if (isSymLink(details)) {
                        visitSymLink(details);
                    } else if (details.isDirectory()) {
                        visitDir(details);
                    } else {
                        visitFile(details);
                    }
                }
            }

            private Boolean isSymLink(FileCopyDetails fileDetails) {
                try {
                    return Files.isSymbolicLink(fileDetails.getFile().toPath());
                } catch (Exception e) {
                    return false;
                }
            }

            private Boolean isChildOfVisitedSymlink(FileCopyDetails fileDetails) {
                try {
                    File file = fileDetails.getFile();
                    for (File symLink : visitedSymLinks) {
                        if (isChildOf(symLink, file)) return true;
                    }
                } catch (Exception e) {
                }
                return false;
            }

            private Boolean isChildOf(File dir, File file) {
                File parent= file.getParentFile();
                while (parent != null) {
                    if (dir.toString().equals(parent.toString())) return true;
                    parent= parent.getParentFile();
                }
                return false;
            }

            private void visitFile(FileCopyDetails fileDetails) {
                try {
                    ZipArchiveEntry archiveEntry= new ZipArchiveEntry(fileDetails.getRelativePath().getPathString());
                    archiveEntry.setTime(fileDetails.getLastModified());
                    archiveEntry.setUnixMode(UnixStat.FILE_FLAG | fileDetails.getMode());
                    zipOutStr.putArchiveEntry(archiveEntry);
                    if (!fileDetails.isDirectory()) fileDetails.copyTo(zipOutStr);
                    zipOutStr.closeArchiveEntry();
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile), e);
                }
            }

            private void visitDir(FileCopyDetails fileDetails) {
                try {
                    // Trailing slash in name indicates that entry is a directory
                    ZipArchiveEntry archiveEntry= new ZipArchiveEntry(fileDetails.getRelativePath().getPathString() + "/");
                    archiveEntry.setTime(fileDetails.getLastModified());
                    archiveEntry.setUnixMode(UnixStat.DIR_FLAG | fileDetails.getMode());
                    zipOutStr.putArchiveEntry(archiveEntry);
                    zipOutStr.closeArchiveEntry();
                } catch (Exception e) {
                    throw new GradleException(String.format("Could not add %s to ZIP '%s'.", fileDetails, zipFile), e);
                }
            }

            protected void visitSymLink(FileCopyDetails fileDetails) {
                try {
                    visitedSymLinks.add(fileDetails.getFile());
                    Path link= Files.readSymbolicLink(fileDetails.getFile().toPath());

                    ZipArchiveEntry archiveEntry= new ZipArchiveEntry(fileDetails.getRelativePath().getPathString());
                    archiveEntry.setTime(fileDetails.getLastModified());
                    archiveEntry.setUnixMode(UnixStat.LINK_FLAG | fileDetails.getMode());
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
