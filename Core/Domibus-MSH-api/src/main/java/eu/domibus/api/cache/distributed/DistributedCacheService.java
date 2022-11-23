package eu.domibus.api.cache.distributed;

import eu.domibus.api.cache.DomibusCacheException;

/**
 * Service responsible for managing the distributed cache
 */
public interface DistributedCacheService {

    /**
     * Creates or gets a distributed cache with the specified name.
     * If the cache does not exist, it will be created with the default values and near cache configuration specified in domibus-default.properties
     * @param cacheName The name of the cache
     */
    void createCache(String cacheName);

    /**
     * Creates or gets a distributed cache with the specified name and configuration.
     * If the cache does not exist, it will be created with the specified configuration and near cache configuration specified in domibus-default.properties
     * @param cacheName The name of the cache
     * @param cacheSize The max cache size
     * @param timeToLiveSeconds The time to live in seconds for the cache entries
     * @param maxIdleSeconds Maximum number of seconds for each entry to stay idle in the cache.
     */
    void createCache(String cacheName, int cacheSize, int timeToLiveSeconds, int maxIdleSeconds);

    /**
     * Creates or gets a distributed cache with the specified name and configuration.
     * If the cache does not exist, it will be created with the specified configuration and specified near cache configuration
     * @param cacheName The name of the cache
     * @param cacheSize The max cache size
     * @param timeToLiveSeconds The time to live in seconds for the cache entries
     * @param maxIdleSeconds Maximum number of seconds for each entry to stay idle in the cache.
     * @param nearCacheSize The near cache default size for the distributed cache
     * @param nearCacheTimeToLiveSeconds The near cache maximum number of seconds for each entry to stay in the near cache
     * @param nearCacheMaxIdleSeconds The near cache maximum number of seconds for each entry to stay idle in the cache.
     * @throws DomibusCacheException in case the cache does not exist
     */
    void createCache(String cacheName, int cacheSize, int timeToLiveSeconds, int maxIdleSeconds, int nearCacheSize, int nearCacheTimeToLiveSeconds, int nearCacheMaxIdleSeconds);

    void addEntryInCache(String cacheName, String key, Object value) throws DomibusCacheException;

    Object getEntryFromCache(String cacheName, String key) throws DomibusCacheException;

    void evictEntryFromCache(String cacheName, String key) throws DomibusCacheException;
}
