package eu.domibus.web.rest;

import eu.domibus.api.crypto.TrustStoreContentDTO;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.api.util.MultiPartFileUtil;
import eu.domibus.api.validators.SkipWhiteListed;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.converter.PartyCoreMapper;
import eu.domibus.core.crypto.api.TLSCertificateManager;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.TrustStoreRO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@RestController
@RequestMapping(value = "/rest/tlstruststore")
public class TLSTruststoreResource extends TruststoreResourceBase {

    private final TLSCertificateManager tlsCertificateManager;


    public TLSTruststoreResource(TLSCertificateManager tlsCertificateManager,
                                 PartyCoreMapper coreMapper, ErrorHandlerService errorHandlerService,
                                 MultiPartFileUtil multiPartFileUtil, AuditService auditService) {
        super(coreMapper, errorHandlerService, multiPartFileUtil, auditService);
        this.tlsCertificateManager = tlsCertificateManager;
    }

    @PostMapping()
    public String uploadTLSTruststoreFile(@RequestPart("file") MultipartFile file,
                                          @SkipWhiteListed @RequestParam("password") String password) throws RequestValidationException {
        replaceTruststore(file, password);
        return "TLS truststore file has been successfully replaced.";
    }

    @GetMapping(produces = "application/octet-stream")
    public ResponseEntity<ByteArrayResource> downloadTLSTrustStore() {
        return downloadTruststoreContent();
    }

    @GetMapping(value = {"/entries"})
    public List<TrustStoreRO> getTLSTruststoreEntries() {
        return getTrustStoreEntries();
    }

    @GetMapping(path = "/entries/csv")
    public ResponseEntity<String> getTLSEntriesAsCsv() {
        return getEntriesAsCSV("tlsTruststore");
    }

    @PostMapping(value = "/entries")
    public String addTLSCertificate(@RequestPart("file") MultipartFile certificateFile,
                                    @RequestParam("alias") @Valid @NotNull String alias) throws RequestValidationException {
        if (StringUtils.isBlank(alias)) {
            throw new RequestValidationException("Please provide an alias for the certificate.");
        }

        byte[] fileContent = multiPartFileUtil.validateAndGetFileContent(certificateFile);

        tlsCertificateManager.addCertificate(fileContent, alias);

        return "Certificate [" + alias + "] has been successfully added to the TLS truststore.";
    }

    @DeleteMapping(value = "/entries/{alias:.+}")
    public String removeTLSCertificate(@PathVariable String alias) throws RequestValidationException {
        tlsCertificateManager.removeCertificate(alias);
        return "Certificate [" + alias + "] has been successfully removed from the TLS truststore.";
    }

    @Override
    protected void doReplaceTrustStore(byte[] truststoreFileContent, String fileName, String password) {
        tlsCertificateManager.replaceTrustStore(fileName, truststoreFileContent, password);
    }

    @Override
    protected void auditDownload(Long entityId) {
        auditService.addTLSTruststoreDownloadedAudit(entityId != null ? entityId.toString() : "tlstruststore");
    }

    @Override
    protected TrustStoreContentDTO getTrustStoreContent() {
        return tlsCertificateManager.getTruststoreContent();
    }

    @Override
    protected List<TrustStoreEntry> doGetTrustStoreEntries() {
        return tlsCertificateManager.getTrustStoreEntries();
    }

}
