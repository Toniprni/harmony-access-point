package eu.domibus.core.earchive;

/**
 * @author François Gautier
 * @since 5.0
 */
public enum EArchiveBatchStatus {

    /**
     * Batch had been created
     */
    STARTING,
    /**
     * Batch had been picked up by for processing
     */
    STARTED,
    /**
     * Batch in the process of being stopped
     */
    STOPPING,
    /**
     * Batch had been stopped
     */
    STOPPED,
    /**
     * Batch has failed
     */
    FAILED,
    /**
     * Batch was completed
     */
    COMPLETED,
    /**
     * Batch was abandoned
     */
    ABANDONED

}
