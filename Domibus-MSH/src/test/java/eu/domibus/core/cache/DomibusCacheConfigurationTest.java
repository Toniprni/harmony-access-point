package eu.domibus.core.cache;

import mockit.*;
import mockit.integration.junit4.JMockit;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import javax.cache.configuration.Configuration;
import javax.cache.spi.CachingProvider;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Cosmin Baciu
 * @author Catalin Enache
 * @since 4.0
 */
@RunWith(JMockit.class)
public class DomibusCacheConfigurationTest {

    @Tested
    DomibusCacheConfiguration domibusCacheConfiguration;


    @Test
    public void test_cacheManagerExternalFilePresent() throws  Exception {
        prepareTestEhCacheFiles();
        new Expectations(domibusCacheConfiguration) {{
            domibusCacheConfiguration.externalCacheFileExists();
            result = true;

        }};

        //tested method
        org.springframework.cache.CacheManager cacheManager = domibusCacheConfiguration.cacheManager();

        Assert.assertNotNull(cacheManager.getCache("policyCacheDefault"));
        Assert.assertNotNull(cacheManager.getCache("policyCacheExternal"));

    }

    protected void prepareTestEhCacheFiles() {
        Deencapsulation.setField(domibusCacheConfiguration, "defaultEhCacheFile", "config/ehcache/ehcache-default-test.xml");
        Deencapsulation.setField(domibusCacheConfiguration, "externalEhCacheFile", "target/test-classes/conf/domibus/internal/ehcache-test.xml");
    }

    @Test
    public void test_cacheManagerNoExternalFilePresent() throws  Exception {
        prepareTestEhCacheFiles();
        new Expectations(domibusCacheConfiguration) {{
            domibusCacheConfiguration.externalCacheFileExists();
            result = false;
        }};

        //tested method
        org.springframework.cache.CacheManager cacheManager = domibusCacheConfiguration.cacheManager();

        Assert.assertNotNull(cacheManager.getCache("policyCacheDefault"));
        Assert.assertNull(cacheManager.getCache("policyCacheExternal"));
    }


    @Test
    public void test_mergeExternalCacheConfiguration(@Mocked javax.cache.CacheManager defaultCacheManager,
                                                     @Mocked javax.cache.CacheManager externalCacheManager,
                                                     @Mocked CachingProvider cachingProvider) {

        domibusCacheConfiguration.mergeExternalCacheConfiguration(cachingProvider, defaultCacheManager);

        new FullVerifications(domibusCacheConfiguration) {{
            cachingProvider.getCacheManager((URI) any, (ClassLoader) any);

            domibusCacheConfiguration.overridesDefaultCache(defaultCacheManager, externalCacheManager);
        }};
    }

    @Test
    public void test_overridesDefaultCache(@Mocked javax.cache.CacheManager defaultCacheManager,
                                           @Mocked javax.cache.CacheManager externalCacheManager) {
        new Expectations(domibusCacheConfiguration) {{
            externalCacheManager.getCacheNames();
            result = new String[]{"cache1", "cache2"};

            domibusCacheConfiguration.cacheExists(defaultCacheManager, "cache1");;
            result = true;
        }};

        domibusCacheConfiguration.overridesDefaultCache(defaultCacheManager, externalCacheManager);

        new Verifications() {{
            defaultCacheManager.destroyCache("cache1");

            List<String> cacheNamesActual =  new ArrayList<>();
            defaultCacheManager.createCache(withCapture(cacheNamesActual), (Configuration)any);
            Assert.assertTrue(cacheNamesActual.size() == 2);
        }};
    }

}