package eu.domibus.core.message.compression;

import eu.domibus.api.model.*;
import eu.domibus.common.ErrorCode;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.message.PartPropertyDao;
import eu.domibus.core.message.splitandjoin.SplitAndJoinService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.logging.DomibusMessageCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

/**
 * This class is responsible for compression handling of incoming and outgoing ebMS3 messages.
 *
 * @author Christian Koch, Stefan Mueller
 * @since 3.0
 */
@Service
public class CompressionService {
    public static final String COMPRESSION_PROPERTY_KEY = "CompressionType";
    public static final String COMPRESSION_PROPERTY_VALUE = "application/gzip";
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(CompressionService.class);

    @Autowired
    private CompressionMimeTypeBlacklist blacklist;

    @Autowired
    protected SplitAndJoinService splitAndJoinService;

    @Autowired
    protected PartPropertyDao partPropertyDao;

    /**
     * This method is responsible for compression of payloads in a ebMS3 AS4 conformant way in case of {@link eu.domibus.api.model.MSHRole#SENDING}
     *
     * @param messageId           the sending {@link UserMessage} with all payloads
     * @param partInfo            the sending {@link UserMessage} with all payloads
     * @param legConfigForMessage legconfiguration for this message
     * @return {@code true} if compression was applied properly and {@code false} if compression was not enabled in the corresponding pmode
     * @throws EbMS3Exception if an problem occurs during the compression or the mimetype was missing
     */
    public boolean handleCompression(String messageId, PartInfo partInfo, final LegConfiguration legConfigForMessage) throws EbMS3Exception {
        if (partInfo == null) {
            return false;
        }

        //if compression is not necessary return false
        if (!legConfigForMessage.isCompressPayloads()) {
            LOG.debug("Compression is not configured for message [{}]");
            return false;
        }

        if (partInfo.isInBody()) {
            LOG.debug("Compression is not used for body payloads");
            return false;
        }

        final boolean mayUseSplitAndJoin = splitAndJoinService.mayUseSplitAndJoin(legConfigForMessage);
        if (mayUseSplitAndJoin) {
            LOG.debug("SplitAndJoin compression is only applied for the multipart message");
            return false;
        }

        if (partInfo.getPartProperties() == null) {
            LOG.businessError(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_COMPRESSION_FAILURE_MISSING_MIME_TYPE, partInfo.getHref(), messageId);
            EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0303, "No mime type found for payload with cid:" + partInfo.getHref(), messageId, null);
            ex.setMshRole(MSHRole.SENDING);
            throw ex;
        }

        String mimeType = null;
        for (final Property property : partInfo.getPartProperties()) {
            if (Property.MIME_TYPE.equalsIgnoreCase(property.getName())) {
                mimeType = property.getValue();
                break;
            }
        }

        if (mimeType == null || mimeType.isEmpty()) {
            LOG.businessError(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_COMPRESSION_FAILURE_MISSING_MIME_TYPE, partInfo.getHref(), messageId);
            EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0303, "No mime type found for payload with cid:" + partInfo.getHref(), messageId, null);
            ex.setMshRole(MSHRole.SENDING);
            throw ex;
        }

        //if mimetype of payload is not considered to be compressed, skip
        if (this.blacklist.getEntries().contains(mimeType)) {
            return false;
        }

        final PartProperty compressionProperty = partPropertyDao.findOrCreateProperty(CompressionService.COMPRESSION_PROPERTY_KEY, CompressionService.COMPRESSION_PROPERTY_VALUE, null);
        partInfo.getPartProperties().add(compressionProperty);
        final CompressedDataSource compressedDataSource = new CompressedDataSource(partInfo.getPayloadDatahandler().getDataSource());
        DataHandler gZipDataHandler = new DataHandler(compressedDataSource);
        partInfo.setPayloadDatahandler(gZipDataHandler);
        CompressionService.LOG.debug("Payload with cid: " + partInfo.getHref() + " and mime type: " + mimeType + " will be compressed");

        return true;
    }

    /**
     * This method handles decompression of payloads for messages in case of {@link eu.domibus.api.model.MSHRole#RECEIVING}
     *
     * @param userMessage         the receving {@link UserMessage} with all payloads
     * @param legConfigForMessage processing information for the message
     * @return {@code true} if everything was decompressed without problems, {@code false} in case of disabled compression via pmode
     * @throws EbMS3Exception if an problem occurs during the de compression or the mimetype of a compressed payload was missing
     */
    public boolean handleDecompression(final UserMessage userMessage, List<PartInfo> partInfoList, final LegConfiguration legConfigForMessage) throws EbMS3Exception {
        //if compression is not necessary return false
        if (!legConfigForMessage.isCompressPayloads()) {
            LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_DECOMPRESSION_NOT_ENABLED);
            return false;
        }

        for (final PartInfo partInfo : partInfoList) {
            if (partInfo.isInBody()) {
                LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_DECOMPRESSION_PART_INFO_IN_BODY, partInfo.getHref());
                continue;
            }

            String mimeType = null;
            boolean payloadCompressed = false;

            if (partInfo.getPartProperties() != null) {
                for (final Property property : partInfo.getPartProperties()) {
                    if (Property.MIME_TYPE.equalsIgnoreCase(property.getName())) {
                        mimeType = property.getValue();
                    }
                    if (CompressionService.COMPRESSION_PROPERTY_KEY.equalsIgnoreCase(property.getName()) && CompressionService.COMPRESSION_PROPERTY_VALUE.equalsIgnoreCase(property.getValue())) {
                        payloadCompressed = true;
                    }
                }
            }


            if (!payloadCompressed) {
                LOG.debug("Decompression is not needed: payload is not compressed");
                continue;
            }

            final Property compressionProperty = new Property();
            compressionProperty.setName(CompressionService.COMPRESSION_PROPERTY_KEY);
            compressionProperty.setValue(CompressionService.COMPRESSION_PROPERTY_VALUE);
            partInfo.getPartProperties().remove(compressionProperty);

            if (mimeType == null) {
                LOG.businessError(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_DECOMPRESSION_FAILURE_MISSING_MIME_TYPE, partInfo.getHref(), userMessage.getMessageId());
                EbMS3Exception ex = new EbMS3Exception(ErrorCode.EbMS3ErrorCode.EBMS_0303, "No mime type found for payload with cid:" + partInfo.getHref(), userMessage.getMessageId(), null);
                ex.setMshRole(MSHRole.RECEIVING);
                throw ex;
            }
            partInfo.setPayloadDatahandler(new DataHandler(new DecompressionDataSource(partInfo.getPayloadDatahandler().getDataSource(), mimeType)));
            LOG.businessInfo(DomibusMessageCode.BUS_MESSAGE_PAYLOAD_DECOMPRESSION, partInfo.getHref());
        }
        return true;
    }

    private class CompressedDataSource implements DataSource {
        private DataSource ds;

        private CompressedDataSource(DataSource ds) {
            this.ds = ds;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return ds.getInputStream();
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            return ds.getOutputStream();
        }

        @Override
        public String getContentType() {
            return CompressionService.COMPRESSION_PROPERTY_VALUE;
        }

        @Override
        public String getName() {
            return "compressed-" + ds.getName();
        }
    }
}
