package eu.domibus.test.dao;

import eu.domibus.api.datasource.DataSourceConstants;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.Deencapsulation;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.internal.DatasourceConnectionProviderImpl;
import org.hibernate.engine.jdbc.connections.spi.ConnectionProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * @author Francois Gautier
 * @since 5.0
 */
@Configuration
public abstract class AbstractDatabaseConfig {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractDatabaseConfig.class);

    public static final String H_2_DIALECT = "org.hibernate.dialect.H2Dialect";
    public static final String ORACLE_DIALECT = "org.hibernate.dialect.Oracle10gDialect";

    @Bean(DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE)
    protected abstract DataSource dataSource();

    @Bean
    public LocalContainerEntityManagerFactoryBean domibusEM(@Qualifier(DataSourceConstants.DOMIBUS_JDBC_DATA_SOURCE) DataSource dataSource, ConnectionProvider connectionProvider) {
        Properties jpaProperties = new Properties();
        jpaProperties.put("hibernate.hbm2ddl.auto", "update");
        jpaProperties.put("hibernate.show_sql", "true");
        jpaProperties.put("hibernate.format_sql", "true");
        jpaProperties.put("hibernate.id.new_generator_mappings", "false");

        jpaProperties.put("hibernate.current_session_context_class", "thread");
        jpaProperties.put("hibernate.cache.use_query_cache", "true");
        jpaProperties.put("hibernate.cache.use_second_level_cache", "true");
        jpaProperties.put("hibernate.cache.default_cache_concurrency_strategy", "read-write");
        jpaProperties.put("hibernate.cache.region.factory_class", "org.hibernate.cache.ehcache.EhCacheRegionFactory");

        jpaProperties.put("hibernate.generate_statistics", "true");
        jpaProperties.put("hibernate.cache.generate_statistics", "true");

        jpaProperties.put(AvailableSettings.CONNECTION_PROVIDER, connectionProvider);
        jpaProperties.put("hibernate.dialect", getSpecificDatabaseDialect());

        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = new LocalContainerEntityManagerFactoryBean();
        localContainerEntityManagerFactoryBean.setPackagesToScan("eu.domibus");
        localContainerEntityManagerFactoryBean.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        localContainerEntityManagerFactoryBean.setJpaProperties(jpaProperties);
        localContainerEntityManagerFactoryBean.setDataSource(dataSource);
        return localContainerEntityManagerFactoryBean;
    }

    @Bean
    public JpaTransactionManager jpaTransactionManager(LocalContainerEntityManagerFactoryBean domibusEM) {
        JpaTransactionManager jpaTransactionManager = new JpaTransactionManager();
        jpaTransactionManager.setEntityManagerFactory(domibusEM.getObject());
        return jpaTransactionManager;
    }

    @Bean
    public ConnectionProvider connectionProvider(DataSource dataSource) {
        LOG.putMDC(DomibusLogger.MDC_USER, "ItTestUser");
        DatasourceConnectionProviderImpl domibusConnectionProvider = new DatasourceConnectionProviderImpl();
        Deencapsulation.setField(domibusConnectionProvider, "dataSource", dataSource);
        return domibusConnectionProvider;
    }

    public abstract String getSpecificDatabaseDialect();

    public EmbeddedDatabase getH2DataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .build();
    }
    public DriverManagerDataSource getOracleDataSource() {
        DriverManagerDataSource driverManagerDataSource = new DriverManagerDataSource();
        driverManagerDataSource.setDriverClassName("oracle.jdbc.OracleDriver");
        driverManagerDataSource.setUrl("");
        driverManagerDataSource.setUsername("sa");
        driverManagerDataSource.setPassword("");
        return driverManagerDataSource;
    }
}
