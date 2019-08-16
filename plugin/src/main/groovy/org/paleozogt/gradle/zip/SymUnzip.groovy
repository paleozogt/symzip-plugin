package org.paleozogt.gradle.zip

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.io.IOUtils
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.*
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.WorkResult
import org.gradle.internal.nativeintegration.filesystem.FileSystem
import org.gradle.internal.reflect.Instantiator
import org.paleozogt.gradle.zip.SymUnzip.FileCopyAction

import java.nio.file.Files

class SymUnzip extends AbstractCopyTask {
    @Override
    protected CopyAction createCopyAction() {
        File destinationDir = getDestinationDir();
        if (destinationDir == null) {
            throw new InvalidUserDataException("No copy destination directory has been specified, use 'into' to specify a target directory.");
        }
        return new FileCopyAction(getFileLookup().getFileResolver(destinationDir));
    }

    @Override
    protected CopySpecInternal createRootSpec() {
        Instantiator instantiator = getInstantiator();
        FileResolver fileResolver = getFileResolver();

        return instantiator.newInstance(DestinationRootCopySpec.class, fileResolver, super.createRootSpec());
    }

    @Override
    public DestinationRootCopySpec getRootSpec() {
        return (DestinationRootCopySpec) super.getRootSpec();
    }

    /**
     * Returns the directory to copy files into.
     *
     * @return The destination dir.
     */
    @OutputDirectory
    public File getDestinationDir() {
        return getRootSpec().getDestinationDir();
    }

    /**
     * Sets the directory to copy files into. This is the same as calling {@link #into(Object)} on this task.
     *
     * @param destinationDir The destination directory. Must not be null.
     */
    public void setDestinationDir(File destinationDir) {
        into(destinationDir);
    }

    class FileCopyAction implements CopyAction {
        private final FileResolver fileResolver;

        public FileCopyAction(FileResolver fileResolver) {
            this.fileResolver = fileResolver;
        }

        public WorkResult execute(CopyActionProcessingStream stream) {
            FileCopyDetailsInternalAction action = new FileCopyDetailsInternalAction();
            stream.process(action);
            return WorkResults.didWork(action.didWork);
        }

        private class FileCopyDetailsInternalAction implements CopyActionProcessingStreamAction {
            private boolean didWork;

            public void processFile(FileCopyDetailsInternal details) {
                File target = fileResolver.resolve(details.getRelativePath().getPathString());

                try {
                    explodeZip(details, target.getParentFile());
                    didWork= true;
                } catch (IOException e) {
                    boolean copied = details.copyTo(target);
                    if (copied) {
                        didWork = true;
                    }
                }
            }

            protected void explodeZip(FileCopyDetails fileDetails, File target) {
                ZipFile zipFile= null;
                try {
                    zipFile= new ZipFile(fileDetails.getFile());

                    // TODO: for-each?
                    Enumeration entries= zipFile.getEntries();
                    while (entries.hasMoreElements()) {
                        ZipArchiveEntry entry=(ZipArchiveEntry)entries.nextElement();
                        File entryFile= new File(target, entry.getName());
                        entryFile.getParentFile().mkdirs();
                        getLogger().debug("zip entry {} mode={} symlink={}", entry, entry.getUnixMode(), entry.isUnixSymlink());
                        if (entry.isUnixSymlink()) {
                            String linkEntry= getEntryContents(zipFile, entry);
                            File linkEntryFile= new File(linkEntry);
                            Files.createSymbolicLink(entryFile.toPath(), linkEntryFile.toPath());
                        } else if (entry.isDirectory()) {
                            entryFile.mkdir();
                            getFileSystem().chmod(entryFile, getEntryMode(entry));
                        } else {
                            copyStreamToFile(zipFile.getInputStream(entry), entryFile);
                            getFileSystem().chmod(entryFile, getEntryMode(entry));
                        }
                    }
                } finally {
                    ZipFile.closeQuietly(zipFile);
                }
            }

            protected static int getEntryMode(ZipArchiveEntry entry) {
                int unixMode = entry.getUnixMode() & 0777;
                if (unixMode == 0) {
                    //no mode infos available - fall back to defaults
                    if (entry.isDirectory()){
                        unixMode = FileSystem.DEFAULT_DIR_MODE;
                    } else{
                        unixMode = FileSystem.DEFAULT_FILE_MODE;
                    }
                }
                return unixMode;
            }

            protected static String getEntryContents(ZipFile zipFile, ZipArchiveEntry entry) throws IOException {
                InputStream entryStream= zipFile.getInputStream(entry);
                ByteArrayOutputStream contents= new ByteArrayOutputStream();
                IOUtils.copy(entryStream, contents);
                return contents.toString();
            }

            protected static void copyStreamToFile(InputStream inputStream, File outputFile) {
                OutputStream outputStream= null;
                try {
                    outputStream= new FileOutputStream(outputFile);
                    IOUtils.copy(inputStream, outputStream);
                } finally {
                    IOUtils.closeQuietly(outputStream);
                }
            }
        }
    }
}
