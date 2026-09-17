package com.behl.overseer.configuration;

import java.util.Optional;
import java.util.UUID;

import javax.cache.CacheManager;
import javax.cache.Caching;

import org.redisson.config.Config;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;

@Configuration
public class RedisConfiguration {

	// Redis/JCache region that stores Bucket4j data for each user.
	private static final String CACHE_NAME = "rate-limit";

	/** Creates the Redis-backed JCache used to store distributed rate-limit buckets. */
	@Bean(name = "rate-limit-cache-manager")
	public CacheManager cacheManager(final RedisProperties redisProperties) {
		// Obtain the application-wide JCache manager and avoid creating the same region twice.
		final var cacheManager = Caching.getCachingProvider().getCacheManager();
		final var isCacheCreated = Optional.ofNullable(cacheManager.getCache(CACHE_NAME)).isPresent();
        
		if (Boolean.FALSE.equals(isCacheCreated)) {
			// Build the Redis connection details from Spring's `spring.data.redis` settings.
			final var connectionUrl = String.format("redis://%s:%d", redisProperties.getHost(), redisProperties.getPort());
			final var configuration = new Config();
			configuration.useSingleServer().setPassword(redisProperties.getPassword()).setAddress(connectionUrl);

			// Redisson adapts its Redis client configuration to the standard JCache API.
			cacheManager.createCache(CACHE_NAME, RedissonConfiguration.fromConfig(configuration));
		}
		return cacheManager;
	}

	/** Adapts the Redis cache into Bucket4j's API for per-user bucket operations. */
	@Bean
	ProxyManager<UUID> proxyManager(final CacheManager cacheManager) {
		return new JCacheProxyManager<UUID>(cacheManager.getCache(CACHE_NAME));
	}

}
