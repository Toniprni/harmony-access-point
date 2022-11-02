package eu.domibus.core.spring;

import eu.domibus.core.property.listeners.PropertyListenersConfiguration;
import eu.domibus.core.security.configuration.DomibusAuthenticationConfiguration;
import eu.domibus.core.security.configuration.SecurityAdminConsoleConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.*;
import org.springframework.jms.annotation.EnableJms;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import static eu.domibus.ext.services.DomibusPropertyManagerExt.PLUGINS_CONFIG_HOME;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Configuration("domibusConfiguration")
@ImportResource({
        "classpath:META-INF/cxf/cxf-extension-jaxws.xml",
        "classpath:META-INF/cxf/cxf-servlet.xml",
        "classpath*:config/*-plugin.xml",
        "file:///${domibus.config.location}/"+ PLUGINS_CONFIG_HOME + "/*-plugin.xml",
        "classpath*:META-INF/resources/WEB-INF/cxf-endpoint.xml"
})
@ComponentScan(
        basePackages = "eu.domibus",
        excludeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.web\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.ext\\.rest\\..*")/*,
                //eArchive client beans
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.archive\\.client\\.api\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.archive\\.client\\.invoker\\..*"),
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "eu\\.domibus\\.archive\\.client\\.model\\..*")*/
        }
)
@EnableJms
@EnableTransactionManagement(proxyTargetClass = true)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableCaching
@Import({SecurityAdminConsoleConfiguration.class, DomibusAuthenticationConfiguration.class, PropertyListenersConfiguration.class})
public class DomibusRootConfiguration {
}
