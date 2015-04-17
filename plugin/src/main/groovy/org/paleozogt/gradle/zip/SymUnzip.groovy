package org.paleozogt.gradle.zip

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.file.FileCopyDetails;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.tasks.OutputDirectory;

import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.file.copy.DestinationRootCopySpec;
import org.gradle.internal.reflect.Instantiator;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream.UnicodeExtraFieldPolicy;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.archivers.zip.UnixStat;
import org.apache.commons.compress.archivers.ArchiveEntry;

import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FilenameUtils;

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
            return new SimpleWorkResult(action.didWork);
        }

        private class FileCopyDetailsInternalAction implements CopyActionProcessingStreamAction {
            private boolean didWork;

            public void processFile(FileCopyDetailsInternal details) {
                File target = fileResolver.resolve(details.getRelativePath().getPathString());

                String sourceExt = FilenameUtils.getExtension(details.getFile().toString()).toLowerCase();
                if (sourceExt.equals("zip")) {
                    explodeZip(details, target.getParentFile());
                    didWork= true;
                } else {
                    boolean copied = details.copyTo(target);
                    if (copied) {
                        didWork = true;
                    }
                }
            }

            protected void explodeZip(FileCopyDetails fileDetails, File target) {
                ZipFile zipFile= new ZipFile(fileDetails.getFile());

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
                    } else if (!entry.isDirectory()) {
                        IOUtils.copy(zipFile.getInputStream(entry), new FileOutputStream(entryFile));
                        getFileSystem().chmod(entryFile, getEntryMode(entry));
                    }
                }
            }

            protected static int getEntryMode(ZipArchiveEntry entry) {
                int unixMode = entry.getUnixMode() & 0777;
                if(unixMode == 0){
                    //no mode infos available - fall back to defaults
                    if(isDirectory()){
                        unixMode = FileSystem.DEFAULT_DIR_MODE;
                    }else{
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
        }
    }
}
