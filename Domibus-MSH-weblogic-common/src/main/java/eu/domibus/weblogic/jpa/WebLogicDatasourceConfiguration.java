package eu.domibus.weblogic.jpa;

import eu.domibus.api.property.DomibusPropertyMetadataManager;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jndi.JndiObjectFactoryBean;

import javax.sql.DataSource;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@Configuration
public class WebLogicDatasourceConfiguration {

    private static final DomibusLogger LOGGER = DomibusLoggerFactory.getLogger(WebLogicDatasourceConfiguration.class);

    @Bean("domibusJDBC-XADataSource")
    public DataSource xaDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
        String jndiName = domibusPropertyProvider.getProperty(DomibusPropertyMetadataManager.DOMIBUS_JDBC_DATASOURCE_JNDI_NAME);

        LOGGER.debug("Configured property [{}] with [{}]", DomibusPropertyMetadataManager.DOMIBUS_JDBC_DATASOURCE_JNDI_NAME, jndiName);
        jndiObjectFactoryBean.setJndiName(jndiName);
        return (DataSource) jndiObjectFactoryBean.getObject();
    }

    @Bean("domibusJDBC-nonXADataSource")
    public DataSource quartzDatasource(DomibusPropertyProvider domibusPropertyProvider) {
        JndiObjectFactoryBean jndiObjectFactoryBean = new JndiObjectFactoryBean();
        String jndiName = domibusPropertyProvider.getProperty(DomibusPropertyMetadataManager.DOMIBUS_JDBC_DATASOURCE_QUARTZ_JNDI_NAME);

        LOGGER.debug("Configured property [{}] with [{}]", DomibusPropertyMetadataManager.DOMIBUS_JDBC_DATASOURCE_QUARTZ_JNDI_NAME, jndiName);
        jndiObjectFactoryBean.setJndiName(jndiName);
        return (DataSource) jndiObjectFactoryBean.getObject();
    }
}
