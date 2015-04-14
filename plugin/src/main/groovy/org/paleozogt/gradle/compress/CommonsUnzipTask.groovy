package org.paleozogt.gradle.compress

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.WorkResult;
import org.gradle.api.tasks.AbstractCopyTask;
import org.gradle.api.internal.file.copy.CopyAction;
import org.gradle.api.internal.file.copy.CopyActionProcessingStream;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.file.CopyActionProcessingStreamAction;
import org.gradle.api.internal.tasks.SimpleWorkResult;
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal;
import org.gradle.api.tasks.OutputDirectory;

import org.gradle.api.internal.file.copy.CopySpecInternal;
import org.gradle.api.internal.file.copy.DestinationRootCopySpec;
import org.gradle.internal.reflect.Instantiator;

class CommonsUnzipTask extends AbstractCopyTask {
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
                boolean copied = details.copyTo(target);
                if (copied) {
                    didWork = true;
                }
            }
        }
    }
}
