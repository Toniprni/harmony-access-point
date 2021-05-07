package eu.domibus.core.message;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author François Gautier
 * @since 5.0
 */
@Configuration
public class MessageConfig {
    @Bean
    public UserMessageLogDao userMessageLogDao() {
        return new UserMessageLogDao();
    }

    @Bean
    public MessageInfoDao messageInfoDao() {
        return new MessageInfoDao();
    }

    @Bean
    public MessageStatusDao messageStatusDao() {
        return new MessageStatusDao();
    }

    @Bean
    public NotificationStatusDao notificationStatusDao() {
        return new NotificationStatusDao();
    }

    @Bean
    public UserMessageDao userMessageDao() {
        return new UserMessageDao();
    }

    @Bean
    public MessagePropertyDao propertyDao() {
        return new MessagePropertyDao();
    }

    @Bean
    public UserMessageLogInfoFilter userMessageLogInfoFilter() {
        return new UserMessageLogInfoFilter();
    }
}




