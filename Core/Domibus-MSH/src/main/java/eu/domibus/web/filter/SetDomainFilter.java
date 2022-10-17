package eu.domibus.web.filter;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.DomibusUserDetails;
import eu.domibus.core.multitenancy.DomibusDomainException;
import eu.domibus.logging.DomibusLoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
public class SetDomainFilter extends GenericFilterBean {

    private static final Logger LOG = DomibusLoggerFactory.getLogger(SetDomainFilter.class);

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected DomibusConfigurationService domibusConfigurationService;

    @Autowired
    AuthUtils authUtils;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        DomibusUserDetails loggedUser = authUtils.getUserDetails();
        if (loggedUser != null) {
            String domain = getDomain(loggedUser);
            LOG.debug("Found authenticated user [{}]; setting its domain [{}] on the context.", loggedUser.getUsername(), domain);
            try {
                domainContextProvider.setCurrentDomainWithValidation(domain);
            } catch (DomibusDomainException ex) {
                LOG.error("Invalid domain: [{}]", domain, ex);
            }
        } else {
            LOG.debug("No authenticated user found so no domain to set.");
        }
        chain.doFilter(request, response);
    }

    protected String getDomain(DomibusUserDetails user) {
        if (domibusConfigurationService.isMultiTenantAware()) {
            return user.getDomain();
        }
        return DomainService.DEFAULT_DOMAIN.getCode();
    }
}
