package eu.domibus.core.audit.model;

import eu.domibus.api.audit.envers.RevisionLogicalName;
import eu.domibus.core.audit.envers.ModificationType;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.util.Date;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@Entity
@DiscriminatorValue("Truststore")
@RevisionLogicalName("Truststore")
public class TruststoreAudit extends AbstractGenericAudit {

    public TruststoreAudit() {
    }

    public TruststoreAudit(final String id, final String userName, final Date revisionDate, final ModificationType modificationType) {
        super(id, userName, revisionDate, modificationType);
    }
}
