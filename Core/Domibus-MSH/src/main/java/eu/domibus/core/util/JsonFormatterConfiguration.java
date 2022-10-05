package eu.domibus.core.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static eu.domibus.core.property.DomibusGeneralConstants.JSON_MAPPER_BEAN;

/**
 * @author François Gautier
 * @since 5.0
 */
@Configuration
public class JsonFormatterConfiguration {

    /**
     * Generate common JSON ObjectMapper fpr formatting dates as: "2021-12-01T14:52:00Z"
     *
     * @return
     */
    @Bean(JSON_MAPPER_BEAN)
    public ObjectMapper objectMapper() {
        ObjectMapper bean = new ObjectMapper();
        bean.registerModule(new JavaTimeModule());
        bean.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return bean;
    }
}
