package eu.domibus.core.message;

import eu.domibus.api.message.MessageSubtype;
import eu.domibus.api.model.MessageSubtypeEntity;
import eu.domibus.core.dao.BasicDao;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Repository
public class MessageSubtypeDao extends BasicDao<MessageSubtypeEntity> {

    public MessageSubtypeDao() {
        super(MessageSubtypeEntity.class);
    }

    public MessageSubtypeEntity findByType(final MessageSubtype role) {
        final TypedQuery<MessageSubtypeEntity> query = this.em.createNamedQuery("MessageSubtypeEntity.findByValue", MessageSubtypeEntity.class);
        query.setParameter("VALUE", role);
        return DataAccessUtils.singleResult(query.getResultList());
    }
}