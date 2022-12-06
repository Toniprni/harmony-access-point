package eu.domibus.web.rest;

import eu.domibus.api.crypto.TrustStoreContentDTO;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.api.util.MultiPartFileUtil;
import eu.domibus.api.validators.SkipWhiteListed;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.converter.PartyCoreMapper;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.TrustStoreRO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static eu.domibus.core.crypto.MultiDomainCryptoServiceImpl.DOMIBUS_TRUSTSTORE_NAME;

/**
 * @author Mircea Musat
 * @author Ion Perpegel
 * @since 3.3
 */
@RestController
@RequestMapping(value = "/rest/truststore")
public class TruststoreResource extends TruststoreResourceBase {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TruststoreResource.class);

    private final MultiDomainCryptoService multiDomainCertificateProvider;

    private final DomainContextProvider domainProvider;

    private final CertificateService certificateService;

    public TruststoreResource(MultiDomainCryptoService multiDomainCertificateProvider,
                              DomainContextProvider domainProvider, CertificateService certificateService,
                              PartyCoreMapper partyConverter, ErrorHandlerService errorHandlerService,
                              MultiPartFileUtil multiPartFileUtil, AuditService auditService, DomainContextProvider domainContextProvider, DomibusConfigurationService domibusConfigurationService) {
        super(partyConverter, errorHandlerService, multiPartFileUtil, auditService, domainContextProvider, domibusConfigurationService);

        this.multiDomainCertificateProvider = multiDomainCertificateProvider;
        this.domainProvider = domainProvider;
        this.certificateService = certificateService;
    }

    @PostMapping(value = "/save")
    public String uploadTruststoreFile(@RequestPart("file") MultipartFile truststoreFile,
                                       @SkipWhiteListed @RequestParam("password") String password) throws RequestValidationException {
        LOG.debug("Uploading file [{}] as the truststore for the current domain ", truststoreFile.getName());

        replaceTruststore(truststoreFile, password);

        return "Truststore file has been successfully replaced.";
    }

    @GetMapping(value = "/download", produces = "application/octet-stream")
    public ResponseEntity<ByteArrayResource> downloadTrustStore(){
        LOG.debug("Downloading the truststore as byte array for the current domain");

        return downloadTruststoreContent();
    }

    @PostMapping(value = "/reset")
    public void reset() {
        LOG.debug("Resetting the keystore for the current domain");

        Domain currentDomain = domainProvider.getCurrentDomain();
        multiDomainCertificateProvider.resetTrustStore(currentDomain);
    }

    @GetMapping(value = {"/list"})
    public List<TrustStoreRO> trustStoreEntries() {
        LOG.debug("Listing entries of the truststore for the current domain");

        return getTrustStoreEntries();
    }

    @GetMapping(value = "/changedOnDisk")
    public boolean isChangedOnDisk() {
        LOG.debug("Checking if the truststore has changed on disk for the current domain");

        return certificateService.isChangedOnDisk(DOMIBUS_TRUSTSTORE_NAME);
    }

    @GetMapping(path = "/csv")
    public ResponseEntity<String> getEntriesAsCsv() {
        LOG.debug("Downloading the keystore as CSV for the current domain");
        return getEntriesAsCSV(getStoreName());
    }

    @Override
    protected void doReplaceTrustStore(byte[] truststoreFileContent, String fileName, String password) {
        Domain currentDomain = domainProvider.getCurrentDomain();
        multiDomainCertificateProvider.replaceTrustStore(currentDomain, fileName, truststoreFileContent, password);
    }

    @Override
    protected void auditDownload(Long entityId) {
        auditService.addTruststoreDownloadedAudit(entityId != null ? entityId.toString() : getStoreName());
    }

    @Override
    protected TrustStoreContentDTO getTrustStoreContent() {
        return multiDomainCertificateProvider.getTruststoreContent(domainProvider.getCurrentDomain());
    }

    @Override
    protected List<TrustStoreEntry> doGetStoreEntries() {
        return certificateService.getTrustStoreEntries(DOMIBUS_TRUSTSTORE_NAME);
    }

    @Override
    protected String getStoreName() {
        return "truststore";
    }
}
