package eu.domibus.core.message;

import eu.domibus.api.model.MessageProperty;
import eu.domibus.api.model.Mpc;
import eu.domibus.core.dao.BasicDao;
import org.springframework.dao.support.DataAccessUtils;
import org.springframework.stereotype.Repository;

import javax.persistence.TypedQuery;

/**
 * @author Cosmin Baciu
 * @since 5.0
 */
@Repository
public class MpcDao extends BasicDao<Mpc> {

    public MpcDao() {
        super(Mpc.class);
    }

    public Mpc findOrCreateMpc(String value) {
        Mpc mpc = findMpc(value);
        if (mpc != null) {
            return mpc;
        }
        Mpc newMpc = new Mpc();
        newMpc.setValue(value);
        create(newMpc);
        return newMpc;
    }

    public Mpc findMpc(final String mpc) {
        final TypedQuery<Mpc> query = this.em.createNamedQuery("Mpc.findByValue", Mpc.class);
        query.setParameter("MPC", mpc);
        return DataAccessUtils.singleResult(query.getResultList());
    }
}
