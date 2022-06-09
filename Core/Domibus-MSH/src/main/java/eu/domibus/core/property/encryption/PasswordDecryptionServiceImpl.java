package eu.domibus.core.property.encryption;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.encryption.PasswordDecryptionContext;
import eu.domibus.api.property.encryption.PasswordDecryptionService;
import eu.domibus.api.property.encryption.PasswordEncryptionSecret;
import eu.domibus.api.util.EncryptionUtil;
import eu.domibus.logging.IDomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.io.File;

import static eu.domibus.core.property.encryption.PasswordEncryptionServiceImpl.ENC_END;
import static eu.domibus.core.property.encryption.PasswordEncryptionServiceImpl.ENC_START;

/**
 * Responsible for decrypting encrypted passwords(split from encryption service to avoid cyclic dependencies)
 *
 * @author Ion Perpegel
 * @since 5.0
 */
@Service
public class PasswordDecryptionServiceImpl implements PasswordDecryptionService {

    private static final IDomibusLogger LOG = DomibusLoggerFactory.getLogger(PasswordDecryptionServiceImpl.class);

    private final PasswordEncryptionDao passwordEncryptionDao;

    private final EncryptionUtil encryptionUtil;

    private final PasswordDecryptionContextFactory passwordDecryptionContextFactory;

    private final PasswordDecryptionHelper passwordDecryptionHelper;

    public PasswordDecryptionServiceImpl(PasswordEncryptionDao passwordEncryptionDao, EncryptionUtil encryptionUtil,
                                         PasswordDecryptionContextFactory passwordDecryptionContextFactory,
                                         PasswordDecryptionHelper passwordDecryptionHelper) {
        this.passwordEncryptionDao = passwordEncryptionDao;
        this.encryptionUtil = encryptionUtil;
        this.passwordDecryptionContextFactory = passwordDecryptionContextFactory;
        this.passwordDecryptionHelper = passwordDecryptionHelper;
    }

    @Override
    public boolean isValueEncrypted(final String propertyValue) {
        return passwordDecryptionHelper.isValueEncrypted(propertyValue);
    }

    @Override
    public String decryptProperty(Domain domain, String propertyName, String encryptedFormatValue) {
        final PasswordDecryptionContext passwordEncryptionContext = passwordDecryptionContextFactory.getContext(domain);
        final File encryptedKeyFile = passwordEncryptionContext.getEncryptedKeyFile();
        return decryptProperty(encryptedKeyFile, propertyName, encryptedFormatValue);
    }

    @Override
    public String decryptPropertyIfEncrypted(Domain domain, String propertyName, String propertyValue) {
        if (!isValueEncrypted(propertyValue)) {
            return propertyValue;
        }

        LOG.debug("Decrypting property [{}]", propertyName);
        return decryptProperty(domain, propertyName, propertyValue);
    }

    protected String decryptProperty(final File encryptedKeyFile, String propertyName, String encryptedFormatValue) {
        final boolean valueEncrypted = isValueEncrypted(encryptedFormatValue);
        if (!valueEncrypted) {
            LOG.trace("Property [{}] is not encrypted: skipping decrypting value", propertyName);
            return encryptedFormatValue;
        }

        PasswordEncryptionSecret secret = passwordEncryptionDao.getSecret(encryptedKeyFile);
        LOG.debug("Using encrypted key file for decryption [{}]", encryptedKeyFile);

        final SecretKey secretKey = encryptionUtil.getSecretKey(secret.getSecretKey());
        final GCMParameterSpec secretKeySpec = encryptionUtil.getSecretKeySpec(secret.getInitVector());

        String base64EncryptedValue = extractValueFromEncryptedFormat(encryptedFormatValue);
        final byte[] encryptedValue = Base64.decodeBase64(base64EncryptedValue);

        return encryptionUtil.decrypt(encryptedValue, secretKey, secretKeySpec);
    }

    protected String extractValueFromEncryptedFormat(String encryptedFormat) {
        return StringUtils.substringBetween(encryptedFormat, ENC_START, ENC_END);
    }

}
