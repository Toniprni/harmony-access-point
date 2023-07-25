package eu.domibus.api.earchive;

/**
 * @author François Gautier
 * @since 5.0
 */
public enum EArchiveRequestType {
    /**
     * Describe the batches generated by the eArchive jobs in the context of continuous archiving
     */
    CONTINUOUS,

    /**
     * Describe the batches generated by API calls in the context of a ponctual archiving
     */
    MANUAL,
    /**
     * Missed messages - from the client perspective this is also "CONTINUOUS"
     */
    SANITIZER

}