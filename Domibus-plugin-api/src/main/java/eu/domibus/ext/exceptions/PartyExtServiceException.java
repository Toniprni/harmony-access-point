package eu.domibus.ext.exceptions;

/**
 * Exception while managing Ext Parties API
 *
 * @since 4.2
 * @author Catalin Enache
 */
public class PartyExtServiceException extends DomibusServiceExtException {

    /**
     *
     * @param errorCode
     * @param message
     */
    public PartyExtServiceException(DomibusErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    /**
     *
     * @param errorCode
     * @param message
     * @param throwable
     */
    public PartyExtServiceException(DomibusErrorCode errorCode, String message, Throwable throwable) {
        super(errorCode, message, throwable);
    }

    /**
     *
     * @param cause
     */
    public PartyExtServiceException(Throwable cause) {
        super(DomibusErrorCode.DOM_001, cause.getMessage(), cause);
    }
}
