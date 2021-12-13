package eu.domibus.ext.rest.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import eu.domibus.api.spring.DomibusWebContext;
import eu.domibus.ext.domain.archive.BatchDTO;
import eu.domibus.ext.web.interceptor.AuthenticationInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Java configuration (that replaces xml file) for configuring external rest services
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@DomibusWebContext
@Configuration("domibusExtWebConfiguration")
@ComponentScan(basePackages = "eu.domibus.ext.rest")
public class DomibusExtWebConfiguration implements WebMvcConfigurer {


    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(authenticationInterceptor())
                .addPathPatterns("/ext/**");
    }

    @Bean
    AuthenticationInterceptor authenticationInterceptor() {
        return new AuthenticationInterceptor();
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        // Configure Object Mapper with format date as: "2021-12-01T14:52:00Z"
        // for objects in package: eu.domibus.ext.domain.archive
        DomibusExtMappingConverter converter = new DomibusExtMappingConverter(BatchDTO.class.getPackage().getName());
        converter.setObjectMapper(new ObjectMapper() {{
            registerModule(new JavaTimeModule());
            registerModule(new Jdk8Module());
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }});
        converters.add(0, converter);
    }
}
