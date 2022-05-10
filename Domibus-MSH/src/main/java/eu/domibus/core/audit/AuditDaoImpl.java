package eu.domibus.core.audit;

import eu.domibus.common.JPAConstants;
import eu.domibus.core.audit.model.*;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@Repository
public class AuditDaoImpl implements AuditDao {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AuditDaoImpl.class);
    public static final String CHANGED = "changed";
    public static final String ID = "id";

    @PersistenceContext(unitName = JPAConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @Override
    public List<Audit> listAudit(final Set<String> auditTargets,
                                 final Set<String> actions,
                                 final Set<String> users,
                                 final Date from,
                                 final Date to,
                                 final int start,
                                 final int max) {

        logCriteria(auditTargets, actions, users, from, to, start, max);
        TypedQuery<Audit> query = entityManager.createQuery(
                buildAuditListCriteria(auditTargets, actions, users, from, to, null));
        query.setFirstResult(start);
        query.setMaxResults(max);
        return customSortAudit(query.getResultList());
    }

    @Override
    public List<Audit> listAuditExceptSuperUsers(Set<String> auditTargets, Set<String> actions, Set<String> users, Date from, Date to, int start, int max, List<Long> superUserIds) {
        logCriteria(auditTargets, actions, users, from, to, start, max);
        TypedQuery<Audit> query = entityManager.createQuery(
                buildAuditListCriteria(auditTargets, actions, users, from, to, superUserIds));
        query.setFirstResult(start);
        query.setMaxResults(max);
        return customSortAudit(query.getResultList());
    }

    protected CriteriaQuery<Audit> buildAuditListCriteria(final Set<String> auditTargets,
                                                          final Set<String> actions,
                                                          final Set<String> users,
                                                          final Date from,
                                                          final Date to,
                                                          final List<Long> superUserIds) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Audit> criteriaQuery = criteriaBuilder.createQuery(Audit.class);
        Root<Audit> root = criteriaQuery.from(Audit.class);
        criteriaQuery.select(root);
        where(auditTargets, actions, users, from, to, superUserIds, criteriaBuilder, criteriaQuery, root);
        criteriaQuery.orderBy(
                criteriaBuilder.desc(root.get(ID).get(CHANGED)),
                criteriaBuilder.desc(root.get(ID).get(ID)));
        return criteriaQuery;
    }

    private void logCriteria(final Set<String> auditTargets,
                             final Set<String> actions,
                             final Set<String> users,
                             final Date from,
                             final Date to,
                             final int start,
                             final int max) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Searching audit for ");
            LOG.debug("target :");
            if (auditTargets != null) {
                auditTargets.forEach(LOG::debug);
            }
            LOG.debug("actions :");
            if (actions != null) {
                actions.forEach(LOG::debug);
            }
            LOG.debug("users :");
            if (users != null) {
                users.forEach(LOG::debug);
            }
            LOG.debug("from :" + from);
            LOG.debug("to :" + to);
            LOG.debug("start :" + start);
            LOG.debug("max :" + max);
        }
    }

    @Override
    public Long countAuditExceptSuperUsers(final Set<String> auditTargets,
                                           final Set<String> actions, final Set<String> users,
                                           final Date from,
                                           final Date to, final List<Long> superUserIds) {
        TypedQuery<Long> query = entityManager.createQuery(
                buildAuditCountCriteria(
                        auditTargets,
                        actions,
                        users,
                        from,
                        to,
                        superUserIds));
        return query.getSingleResult();
    }

    @Override
    public Long countAudit(final Set<String> auditTargets,
                           final Set<String> actions, final Set<String> users,
                           final Date from,
                           final Date to) {
        TypedQuery<Long> query = entityManager.createQuery(
                buildAuditCountCriteria(
                        auditTargets,
                        actions,
                        users,
                        from,
                        to,
                        null));
        return query.getSingleResult();
    }

    protected CriteriaQuery<Long> buildAuditCountCriteria(final Set<String> auditTargets,
                                                          final Set<String> actions,
                                                          final Set<String> users,
                                                          final Date from,
                                                          final Date to,
                                                          final List<Long> superUserIds) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> criteriaQuery = criteriaBuilder.createQuery(Long.class);
        Root<Audit> root = criteriaQuery.from(Audit.class);
        criteriaQuery.select(criteriaBuilder.count(root));
        where(auditTargets, actions, users, from, to, superUserIds, criteriaBuilder, criteriaQuery, root);
        return criteriaQuery;
    }

    protected void where(final Set<String> auditTargets,
                         final Set<String> actions,
                         final Set<String> users,
                         final Date from,
                         final Date to,
                         final List<Long> superUserIds,
                         final CriteriaBuilder criteriaBuilder,
                         final CriteriaQuery<?> criteriaQuery,
                         final Root<Audit> root) {

        List<Predicate> predicates = new ArrayList<>();
        if (CollectionUtils.isNotEmpty(auditTargets)) {
            Path<Object> auditTargetField = root.get(ID).get("auditTargetName");
            predicates.add(auditTargetField.in(auditTargets));
        }
        if (CollectionUtils.isNotEmpty(actions)) {
            Path<Object> actionField = root.get(ID).get("action");
            predicates.add(actionField.in(actions));
        }
        if (CollectionUtils.isNotEmpty(users)) {
            Path<Object> userField = root.get("user");
            predicates.add(userField.in(users));
        }
        if (from != null) {
            Path<Date> changedDate = root.get(ID).get(CHANGED);
            predicates.add(criteriaBuilder.greaterThanOrEqualTo(changedDate, from));
        }
        if (to != null) {
            Path<Date> changedDate = root.get(ID).get(CHANGED);
            predicates.add(criteriaBuilder.lessThanOrEqualTo(changedDate, to));
        }
        if (superUserIds != null) {
            Path<Long> userId = root.get(ID).get(ID); //the id of the audited user from AuditId class
            predicates.add(userId.in(superUserIds).not());
        }
        if (!predicates.isEmpty()) {
            criteriaQuery.where(predicates.toArray(new Predicate[]{}));
        }
    }

    @Override
    public void saveMessageAudit(final MessageAudit messageAudit) {
        entityManager.persist(messageAudit);
    }

    @Override
    public void savePModeAudit(final PModeAudit pmodeAudit) {
        entityManager.persist(pmodeAudit);
    }

    @Override
    public void savePModeArchiveAudit(final PModeArchiveAudit pmodeArchiveAudit) {
        entityManager.persist(pmodeArchiveAudit);
    }

    @Override
    @Transactional
    public void saveJmsMessageAudit(final JmsMessageAudit jmsMessageAudit) {
        entityManager.persist(jmsMessageAudit);
    }

    @Override
    @Transactional
    public void saveTruststoreAudit(TruststoreAudit audit) {
        entityManager.persist(audit);
    }

    // fix the sorting for when the id is actually an integer and not a string
    protected List<Audit> customSortAudit(List<Audit> list) {
        list.sort((element1, element2) -> {
            int result = ObjectUtils.compare(element2.getChanged(), element1.getChanged());
            if (result != 0) {
                return result;
            }
            // fix it only when numeric
            if (NumberUtils.isDigits(element2.getId()) && NumberUtils.isDigits(element1.getId())) {
                return ObjectUtils.compare(NumberUtils.createLong(element2.getId()), NumberUtils.createLong(element1.getId()));
            }
            return ObjectUtils.compare(element2.getId(), element1.getId());
        });
        return list;
    }
}
