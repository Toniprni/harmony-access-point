package eu.domibus.plugin.fs.worker;

import org.springframework.stereotype.Service;

import eu.domibus.logging.IDomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.plugin.fs.FSFilesManager;

/**
 * @author FERNANDES Henrique, GONCALVES Bruno
 */
@Service
public class FSPurgeFailedService extends FSAbstractPurgeService {
    
    private static final IDomibusLogger LOG = DomibusLoggerFactory.getLogger(FSPurgeFailedService.class);
    
    @Override
    public void purgeMessages() {
        LOG.debug("Purging failed file system messages...");

        super.purgeMessages();
    }

    @Override
    protected String getTargetFolderName() {
        return FSFilesManager.FAILED_FOLDER;
    }

    @Override
    protected Integer getExpirationLimit(String domain) {
        return fsPluginProperties.getFailedPurgeExpired(domain);
    }

}
