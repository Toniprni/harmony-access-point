package eu.domibus.core.util;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author François Gautier
 * @since 5.0
 */
@Service
public class FileSystemUtil {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FileSystemUtil.class);

    /**
     * It attempts to create the directory whenever is not present.
     * It works also when the location is a symbolic link.
     */
    public Path createLocation(String path) throws FileSystemException {
        FileSystemManager fileSystemManager = getVFSManager();

        try (FileObject fileObject = fileSystemManager.resolveFile(path)) {
            if (!fileObject.exists()) {
                fileObject.createFolder();
                LOG.info("The folder [{}] has been created!", fileObject.getPath().toAbsolutePath());
            }
            if (fileObject.isSymbolicLink()) {
                try (FileObject f1 = fileSystemManager.resolveFile(Files.readSymbolicLink(fileObject.getPath()).toAbsolutePath().toString())) {
                    return returnWritablePath(f1);
                }
            }
            return returnWritablePath(fileObject);
        } catch (IOException ioEx) {
            return getTemporaryPath(path, fileSystemManager, ioEx);
        }
    }

    private Path getTemporaryPath(String path, FileSystemManager fileSystemManager, IOException ioEx) throws FileSystemException {
        LOG.error("Error creating/accessing the folder [{}]", path, ioEx);
        try (FileObject fo = fileSystemManager.resolveFile(System.getProperty("java.io.tmpdir"))) {
            if(LOG.isWarnEnabled()) {
                LOG.warn(WarningUtil.warnOutput("The temporary folder " + fo.getPath().toAbsolutePath() + " has been selected!"));
            }
            return fo.getPath();
        }
    }

    private Path returnWritablePath(FileObject fileObject) throws IOException {
        if (!fileObject.isWriteable()) {
            throw new IOException("Write permission for folder " + fileObject.getPath().toAbsolutePath() + " is not granted.");
        }
        return fileObject.getPath();
    }

    private FileSystemManager getVFSManager() {
        try {
            return VFS.getManager();
        } catch (FileSystemException e) {
            throw new IllegalStateException("VFS Manager could not be created");
        }
    }
}
