package eu.domibus.ext.delegate.services.truststore;

import eu.domibus.api.crypto.TLSCertificateManager;
import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.pki.KeyStoreContentInfo;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.ext.delegate.mapper.DomibusExtMapper;
import eu.domibus.ext.domain.KeyStoreContentInfoDTO;
import eu.domibus.ext.domain.TrustStoreDTO;
import eu.domibus.ext.domain.TrustStoreEntryDTO;
import eu.domibus.ext.exceptions.TruststoreExtException;
import eu.domibus.ext.services.TLSTrustStoreExtService;
import org.springframework.stereotype.Service;

import java.util.List;

import static eu.domibus.api.crypto.TLSCertificateManager.TLS_TRUSTSTORE_NAME;

/**
 * @author Soumya Chandran
 * @since 5.1
 */
@Service
public class TLSTrustStoreServiceDelegate implements TLSTrustStoreExtService {

    private final TLSCertificateManager tlsCertificateManager;

    private final DomibusExtMapper domibusExtMapper;

    public TLSTrustStoreServiceDelegate(TLSCertificateManager tlsCertificateManager, DomibusExtMapper domibusExtMapper) {
        this.tlsCertificateManager = tlsCertificateManager;
        this.domibusExtMapper = domibusExtMapper;
    }

    @Override
    public KeyStoreContentInfoDTO downloadTruststoreContent() {
        try {
            KeyStoreContentInfo contentInfo = tlsCertificateManager.getTruststoreContent();
            return domibusExtMapper.keyStoreContentInfoToKeyStoreContentInfoDTO(contentInfo);
        } catch (Exception e) {
            throw new TruststoreExtException(e);
        }
    }

    @Override
    public List<TrustStoreEntryDTO> getTrustStoreEntries() {
        try {
            List<TrustStoreEntry> trustStoreEntries = tlsCertificateManager.getTrustStoreEntries();
            return domibusExtMapper.trustStoreEntriesToTrustStoresEntriesDTO(trustStoreEntries);
        } catch (Exception ex) {
            throw new TruststoreExtException(ex);
        }
    }

    @Override
    public void uploadTruststoreFile(KeyStoreContentInfoDTO contentInfoDTO) {
        KeyStoreContentInfo storeContentInfo = domibusExtMapper.keyStoreContentInfoDTOToKeyStoreContentInfo(contentInfoDTO);
        tlsCertificateManager.replaceTrustStore(storeContentInfo);
    }

    @Override
    public void addCertificate(byte[] fileContent, String alias) {
        boolean added = tlsCertificateManager.addCertificate(fileContent, alias);
        if (!added) {
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_011,
                    "Certificate [" + alias + "] was not added to the [" + TLS_TRUSTSTORE_NAME + "] most probably because it already contains the same certificate.");
        }
    }

    @Override
    public void removeCertificate(String alias) {
        boolean removed = tlsCertificateManager.removeCertificate(alias);
        if (!removed) {
            throw new DomibusCoreException(DomibusCoreErrorCode.DOM_009,
                    "Certificate [" + alias + "] was not removed from the [" + TLS_TRUSTSTORE_NAME + "] because it does not exist.");
        }
    }

    @Override
    public String getStoreFileExtension() {
        return tlsCertificateManager.getStoreFileExtension();
    }

}

