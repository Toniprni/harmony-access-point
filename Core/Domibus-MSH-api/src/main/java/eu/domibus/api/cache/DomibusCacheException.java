package eu.domibus.api.cache;

import eu.domibus.api.exceptions.DomibusCoreErrorCode;
import eu.domibus.api.exceptions.DomibusCoreException;

public class DomibusCacheException extends DomibusCoreException {

    public DomibusCacheException(DomibusCoreErrorCode dce, String message) {
        super(dce, message);
    }

    public DomibusCacheException(DomibusCoreErrorCode dce, String message, Throwable cause) {
        super(dce, message, cause);
    }

    public DomibusCacheException(String message) {
        super(message);
    }
}
