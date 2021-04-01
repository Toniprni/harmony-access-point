package eu.domibus.core.jpa;

import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.JPAConstants;
import eu.domibus.core.property.PrefixedProperties;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.MultiTenancyStrategy;
import org.hibernate.cfg.Environment;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.util.Optional;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_ENTITY_MANAGER_FACTORY_PACKAGES_TO_SCAN;

/**
 * @author Cosmin Baciu
 * @since 4.0
 */
@Configuration
public class DomibusJPAConfiguration {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(DomibusJPAConfiguration.class);

    public static final String JPA_PROPERTIES = "jpaProperties";


    @Bean
    public JpaVendorAdapter jpaVendorAdapter() {
        return new HibernateJpaVendorAdapter();
    }

    @Bean
    @DependsOn({DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE})
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier(DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE) DataSource dataSource,
                                                                       DomibusPropertyProvider domibusPropertyProvider,
                                                                       @Qualifier(JPA_PROPERTIES) PrefixedProperties jpaProperties,
                                                                       Optional<ConnectionProvider> singleTenantConnectionProviderImpl,
                                                                       Optional<MultiTenantConnectionProvider> multiTenantConnectionProviderImpl,
                                                                       Optional<CurrentTenantIdentifierResolver> tenantIdentifierResolver) {
        LocalContainerEntityManagerFactoryBean result = new LocalContainerEntityManagerFactoryBean();
        result.setPersistenceUnitName(JPAConstants.PERSISTENCE_UNIT_NAME);
        final String packagesToScanString = domibusPropertyProvider.getProperty(DOMIBUS_ENTITY_MANAGER_FACTORY_PACKAGES_TO_SCAN);
        if (StringUtils.isNotEmpty(packagesToScanString)) {
            final String[] packagesToScan = StringUtils.split(packagesToScanString, ",");
            result.setPackagesToScan(packagesToScan);
        }
        result.setDataSource(dataSource);
        result.setJpaVendorAdapter(jpaVendorAdapter());

        if (singleTenantConnectionProviderImpl.isPresent()) {
            LOG.info("Configuring jpaProperties for single-tenancy");
            jpaProperties.put(Environment.CONNECTION_PROVIDER, singleTenantConnectionProviderImpl.get());
        } else if (multiTenantConnectionProviderImpl.isPresent()) {
            LOG.info("Configuring jpaProperties for multi-tenancy");
            jpaProperties.put(Environment.MULTI_TENANT, MultiTenancyStrategy.SCHEMA);
            jpaProperties.put(Environment.MULTI_TENANT_CONNECTION_PROVIDER, multiTenantConnectionProviderImpl.get());
            if (tenantIdentifierResolver.isPresent()) {
                jpaProperties.put(Environment.MULTI_TENANT_IDENTIFIER_RESOLVER, tenantIdentifierResolver.get());
            }
        }
        result.setJpaProperties(jpaProperties);

        return result;
    }

    @Bean(JPA_PROPERTIES)
    public PrefixedProperties jpaProperties(DomibusPropertyProvider domibusPropertyProvider) {
        PrefixedProperties result = new PrefixedProperties(domibusPropertyProvider, "domibus.entityManagerFactory.jpaProperty.");
        return result;
    }

    @Bean("transactionManager")
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }
}
