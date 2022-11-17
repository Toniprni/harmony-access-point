package eu.domibus.core.multitenancy;

import eu.domibus.api.cache.CacheConstants;
import eu.domibus.api.multitenancy.DomainTaskExecutor;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.DomibusUserDetails;
import eu.domibus.core.cache.DomibusCacheService;
import eu.domibus.core.multitenancy.dao.UserDomainDao;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.security.DomibusUserDetailsImpl;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;

import java.util.ArrayList;

/**
 * @author Ion Perpegel
 * @since 4.0
 */
public class UserDomainServiceMultiDomainImpl implements UserDomainService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserDomainServiceMultiDomainImpl.class);

    @Autowired
    protected DomainTaskExecutor domainTaskExecutor;

    @Autowired
    protected UserDomainDao userDomainDao;

    @Autowired
    protected DomibusCacheService domibusCacheService;

    @Autowired
    protected AuthUtils authUtils;

    /**
     * Get the domain associated to the provided user from the general schema. <br>
     * This is done in a separate thread as the DB connection is cached per thread and cannot be changed anymore to the schema of the associated domain
     *
     * @return the domain code of the user
     */
    @Cacheable(cacheManager = CacheConstants.CACHE_MANAGER, value = DomibusCacheService.USER_DOMAIN_CACHE, key = "#user")
    @Override
    public String getDomainForUser(String user) {
        LOG.debug("Searching domain for user [{}]", user);
        String domain = domainTaskExecutor.submit(() -> userDomainDao.findDomainByUser(user));
        LOG.debug("Found domain [{}] for user [{}]", domain, user);
        return domain;
    }

    /**
     * Get the preferred domain associated to the super user from the general schema. <br>
     * This is done in a separate thread as the DB connection is cached per thread and cannot be changed anymore to the schema of the associated domain
     *
     * @return the code of the preferred domain of a super user
     */
    @Cacheable(cacheManager = CacheConstants.CACHE_MANAGER, value = DomibusCacheService.PREFERRED_USER_DOMAIN_CACHE, key = "#user", unless="#result == null")
    @Override
    public String getPreferredDomainForUser(String user) {
        LOG.debug("Searching preferred domain for user [{}]", user);
        String domain = domainTaskExecutor.submit(() -> userDomainDao.findPreferredDomainByUser(user));
        LOG.debug("Found preferred domain [{}] for user [{}]", domain, user);
        return domain;
    }

    @Override
    public void setDomainForUser(String user, String domainCode) {
        LOG.debug("Setting domain [{}] for user [{}]", domainCode, user);

        executeInContext(() -> setDomainByUser(user, domainCode));
    }

    @Override
    public void setPreferredDomainForUser(String user, String domainCode) {
        LOG.debug("Setting preferred domain [{}] for user [{}]", domainCode, user);

        executeInContext(() -> setPreferredDomainByUser(user, domainCode));
    }

    @Override
    public void deleteDomainForUser(String user) {
        LOG.debug("Deleting domain for user [{}]", user);

        executeInContext(() -> userDomainDao.deleteDomainByUser(user));
    }

    private void setDomainByUser(String user, String domainCode) {
        userDomainDao.setDomainByUser(user, domainCode);
        domibusCacheService.clearCache(DomibusCacheService.USER_DOMAIN_CACHE);
    }

    private void setPreferredDomainByUser(String user, String domainCode) {
        userDomainDao.setPreferredDomainByUser(user, domainCode);
        domibusCacheService.clearCache(DomibusCacheService.PREFERRED_USER_DOMAIN_CACHE);
    }

    protected void executeInContext(Runnable method) {
        DomibusUserDetails ud = authUtils.getUserDetails() != null
                ? authUtils.getUserDetails()
                : new DomibusUserDetailsImpl("domibus", StringUtils.EMPTY, new ArrayList<>());

        domainTaskExecutor.submit(() -> authUtils.runWithSecurityContext(() -> {
            LOG.putMDC(DomibusLogger.MDC_USER, ud.getUsername());
            method.run();
        }, ud.getUsername(), ud.getPassword()));
    }
}
