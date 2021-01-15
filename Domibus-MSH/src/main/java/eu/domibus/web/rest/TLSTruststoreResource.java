package eu.domibus.web.rest;

import com.google.common.collect.ImmutableMap;
import eu.domibus.api.crypto.CryptoException;
import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.api.util.MultiPartFileUtil;
import eu.domibus.api.validators.SkipWhiteListed;
import eu.domibus.core.audit.AuditService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.crypto.api.TLSCertificateManager;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.ErrorRO;
import eu.domibus.web.rest.ro.TrustStoreRO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.Arrays;
import java.util.List;

/**
 * @author Mircea Musat
 * @author Ion Perpegel
 * @since 3.3
 */
@RestController
@RequestMapping(value = "/rest")
public class TLSTruststoreResource extends BaseResource {

    public static final String ERROR_MESSAGE_EMPTY_TRUSTSTORE_PASSWORD = "Failed to upload the truststoreFile file since its password was empty."; //NOSONAR

    private TLSCertificateManager tlsCertificateManager;

    private DomainContextProvider domainProvider;

    private CertificateService certificateService;

    private DomainCoreConverter domainConverter;

    private ErrorHandlerService errorHandlerService;

    private MultiPartFileUtil multiPartFileUtil;

    private AuditService auditService;

    public TLSTruststoreResource(TLSCertificateManager tlsCertificateManager,
                                 DomainContextProvider domainProvider, CertificateService certificateService,
                                 DomainCoreConverter domainConverter, ErrorHandlerService errorHandlerService,
                                 MultiPartFileUtil multiPartFileUtil, AuditService auditService) {
        this.tlsCertificateManager = tlsCertificateManager;
        this.domainProvider = domainProvider;
        this.certificateService = certificateService;
        this.domainConverter = domainConverter;
        this.errorHandlerService = errorHandlerService;
        this.multiPartFileUtil = multiPartFileUtil;
        this.auditService = auditService;
    }

    @ExceptionHandler({CryptoException.class})
    public ResponseEntity<ErrorRO> handleCryptoException(CryptoException ex) {
        return errorHandlerService.createResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @PostMapping(value = "/tlstruststore")
    public String uploadTLSTruststoreFile(@RequestPart("file") MultipartFile truststoreFile,
                                          @SkipWhiteListed @RequestParam("password") String password) throws RequestValidationException {
        replaceTruststore(tlsCertificateManager, truststoreFile, password);
        return "TLS truststore file has been successfully replaced.";
    }

    @GetMapping(value = "/tlstruststore", produces = "application/octet-stream")
    public ResponseEntity<ByteArrayResource> downloadTLSTrustStore() {
        return null;
//        return downloadTruststoreContent(tlsMultiDomainCertificateProvider, () -> auditService.addTLSTruststoreDownloadedAudit());
    }

    @GetMapping(value = {"/tlstruststore/entries"})
    public List<TrustStoreRO> getTLSTruststoreEntries() {
        return getTrustStoreEntries(tlsCertificateManager);
    }

    @GetMapping(path = "/tlstruststore/entries/csv")
    public ResponseEntity<String> getTLSEntriesAsCsv() {
        return null;
//        return getEntriesAsCSV(tlsMultiDomainCertificateProvider, "tlsTruststore");
    }

    @PostMapping(value = "/tlstruststore/entries")
    public String addTLSCertificate(@RequestPart("file") MultipartFile certificateFile,
                                    @RequestParam("alias") @Valid @NotNull String alias) throws RequestValidationException {
        byte[] fileContent = multiPartFileUtil.validateAndGetFileContent(certificateFile);

        if (StringUtils.isBlank(alias)) {
            throw new IllegalArgumentException("Please provide an alias for the certificate.");
        }

        tlsCertificateManager.addCertificate(fileContent, alias);

        return "Certificate [" + alias + "] has been successfully added to the TLS truststore.";
    }

    @DeleteMapping(value = "/tlstruststore/entries/{alias:.+}")
    public String removeTLSCertificate(@PathVariable String alias) throws RequestValidationException {
        tlsCertificateManager.removeCertificate(alias);
        return "Certificate [" + alias + "] has been successfully removed from the TLS truststore.";
    }

    protected void replaceTruststore(TLSCertificateManager tlsCertificateManager, MultipartFile truststoreFile, String password) {
        byte[] truststoreFileContent = multiPartFileUtil.validateAndGetFileContent(truststoreFile);

        if (StringUtils.isBlank(password)) {
            throw new IllegalArgumentException(ERROR_MESSAGE_EMPTY_TRUSTSTORE_PASSWORD);
        }

        tlsCertificateManager.replaceTrustStore(truststoreFile.getOriginalFilename(), truststoreFileContent, password);
    }

    protected ResponseEntity<ByteArrayResource> downloadTruststoreContent(Runnable auditMethod) {
        byte[] content = tlsCertificateManager.getTruststoreContent();
        ByteArrayResource resource = new ByteArrayResource(content);

        HttpStatus status = HttpStatus.OK;
        if (resource.getByteArray().length == 0) {
            status = HttpStatus.NO_CONTENT;
        }

        auditMethod.run();

        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header("content-disposition", "attachment; filename=TrustStore.jks")
                .body(resource);
    }

    protected List<TrustStoreRO> getTrustStoreEntries(TLSCertificateManager tlsCertificateManager) {
        List<TrustStoreEntry> trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
        return domainConverter.convert(trustStoreEntries, TrustStoreRO.class);
    }

    protected ResponseEntity<String> getEntriesAsCSV(MultiDomainCryptoService cryptoService, String moduleName) {
//        final List<TrustStoreRO> entries = getTrustStoreEntries(cryptoService);
        final List<TrustStoreRO> entries = null;
        getCsvService().validateMaxRows(entries.size());

        return exportToCSV(entries,
                TrustStoreRO.class,
                ImmutableMap.of(
                        "ValidFrom".toUpperCase(), "Valid from",
                        "ValidUntil".toUpperCase(), "Valid until"
                ),
                Arrays.asList("fingerprints"), moduleName);
    }
}
