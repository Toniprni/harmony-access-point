package eu.domibus.core.message;

import eu.domibus.api.message.UserMessageSecurityService;
import eu.domibus.api.messaging.MessageNotFoundException;
import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.UserMessage;
import eu.domibus.api.security.AuthUtils;
import eu.domibus.api.security.AuthenticationException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @since 3.3
 */
@Service
public class UserMessageSecurityDefaultService implements UserMessageSecurityService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageSecurityDefaultService.class);

    protected final AuthUtils authUtils;
    protected final UserMessageServiceHelper userMessageServiceHelper;
    protected final UserMessageDao userMessageDao;

    public UserMessageSecurityDefaultService(AuthUtils authUtils, UserMessageServiceHelper userMessageServiceHelper, UserMessageDao userMessageDao) {
        this.authUtils = authUtils;
        this.userMessageServiceHelper = userMessageServiceHelper;
        this.userMessageDao = userMessageDao;
    }

    @Override
    public void checkMessageAuthorizationWithUnsecureLoginAllowed(UserMessage userMessage) throws AuthenticationException {
        try {
            validateUserAccess(userMessage);
        } catch (AccessDeniedException e) {
            throw new AuthenticationException("You are not allowed to access message [" + userMessage.getMessageId() + "]. Reason: [" + e.getMessage() + "]", e);
        }
    }

    /**
     * @param userMessage with set of {@link eu.domibus.api.model.MessageProperty}
     * @throws AccessDeniedException if the authOriginalUser is not ORIGINAL_SENDER or FINAL_RECIPIENT of the {@link UserMessage}
     */
    public void validateUserAccessWithUnsecureLoginAllowed(UserMessage userMessage) throws AccessDeniedException {
        /* unsecured login allowed */
        if (authUtils.isUnsecureLoginAllowed()) {
            LOG.debug("Unsecured login is allowed");
            return;
        }
        try {
            validateUserAccess(userMessage);
        } catch (AccessDeniedException e) {
            throw new AuthenticationException("You are not allowed to access message [" + userMessage.getMessageId() + "]. Reason: [" + e.getMessage() + "]", e);
        }
    }

    public void validateUserAccess(UserMessage userMessage) {
        String authOriginalUser = authUtils.getOriginalUserOrNullIfAdmin();
        List<String> propertyNames = new ArrayList<>();
        propertyNames.add(MessageConstants.ORIGINAL_SENDER);
        propertyNames.add(MessageConstants.FINAL_RECIPIENT);

        if (StringUtils.isBlank(authOriginalUser)) {
            LOG.trace("OriginalUser is [{}] is admin", authOriginalUser);
            return;
        }

        LOG.trace("OriginalUser is [{}] not admin", authOriginalUser);

        /* check the message belongs to the authenticated user */
        boolean found = false;
        for (String propertyName : propertyNames) {
            String originalUser = userMessageServiceHelper.getProperty(userMessage, propertyName);
            if (StringUtils.equalsIgnoreCase(originalUser, authOriginalUser)) {
                found = true;
                break;
            }
        }
        if (!found) {
            LOG.debug("Could not validate originalUser for [{}]", authOriginalUser);
            throw new AccessDeniedException("You are not allowed to handle this message [" + userMessage.getMessageId() + "]. You are authorized as [" + authOriginalUser + "]");
        }
        LOG.trace("Could validate originalUser for [{}]", authOriginalUser);
    }

    public void validateUserAccessWithUnsecureLoginAllowed(UserMessage userMessage, String authOriginalUser, String propertyName) {
        if (StringUtils.isBlank(authOriginalUser)) {
            LOG.trace("OriginalUser is [{}] admin", authOriginalUser);
            return;
        }
        LOG.trace("OriginalUser is [{}] not admin", authOriginalUser);
        /* check the message belongs to the authenticated user */
        String originalUser = userMessageServiceHelper.getProperty(userMessage, propertyName);
        if (!StringUtils.equalsIgnoreCase(originalUser, authOriginalUser)) {
            LOG.debug("User [{}] is trying to submit/access a message having as final recipient: [{}]", authOriginalUser, originalUser);
            throw new AccessDeniedException("You are not allowed to handle this message. You are authorized as [" + authOriginalUser + "]");
        }
    }

    public void checkMessageAuthorizationWithUnsecureLoginAllowed(final Long messageEntityId) {
        UserMessage userMessage = userMessageDao.findByEntityId(messageEntityId);
        if (userMessage == null) {
            throw new MessageNotFoundException(messageEntityId);
        }
        validateUserAccessWithUnsecureLoginAllowed(userMessage);
    }

    @Override
    public void checkMessageAuthorization(String messageId, MSHRole mshRole) {
        UserMessage userMessage = userMessageDao.findByMessageId(messageId, mshRole);
        if (userMessage == null) {
            throw new MessageNotFoundException(messageId);
        }
        validateUserAccessWithUnsecureLoginAllowed(userMessage);
    }

    // we keep this for back-ward compatibility
    public void checkMessageAuthorizationWithUnsecureLoginAllowed(String messageId) {
        UserMessage userMessage = userMessageDao.findByMessageId(messageId);
        if (userMessage == null) {
            throw new MessageNotFoundException(messageId);
        }
        validateUserAccessWithUnsecureLoginAllowed(userMessage);
    }

    public void checkMessageAuthorizationWithUnsecureLoginAllowed(String messageId, MSHRole mshRole) {
        UserMessage userMessage = userMessageDao.findByMessageId(messageId, mshRole);
        if (userMessage == null) {
            throw new MessageNotFoundException(messageId);
        }
        validateUserAccessWithUnsecureLoginAllowed(userMessage);
    }

    // we keep this for now
    public void checkMessageAuthorization(String messageId) {
        UserMessage userMessage = userMessageDao.findByMessageId(messageId);
        if (userMessage == null) {
            throw new MessageNotFoundException(messageId);
        }
        try {
            validateUserAccess(userMessage);
        } catch (AccessDeniedException e) {
            throw new AuthenticationException("You are not allowed to access message [" + userMessage.getMessageId() + "]. Reason: [" + e.getMessage() + "]", e);
        }
    }

}
