/*
 * Copyright (c) 2008-2016, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hazelcast.simulator.tests.icache.helpers;

import com.hazelcast.cache.HazelcastCacheManager;
import com.hazelcast.cache.ICache;
import com.hazelcast.cache.impl.HazelcastServerCacheManager;
import com.hazelcast.cache.impl.HazelcastServerCachingProvider;
import com.hazelcast.client.cache.impl.HazelcastClientCacheManager;
import com.hazelcast.client.cache.impl.HazelcastClientCachingProvider;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.logging.ILogger;
import com.hazelcast.util.EmptyStatement;

import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import java.util.concurrent.TimeUnit;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.isMemberNode;
import static java.lang.String.format;

public final class CacheUtils {

    private CacheUtils() {
    }

    public static void sleepDurationTwice(ILogger logger, Duration duration) {
        if (duration.isEternal() || duration.isZero()) {
            return;
        }

        TimeUnit timeUnit = duration.getTimeUnit();
        long timeout = duration.getDurationAmount() * 2;
        logger.info(format("Sleeping for %d %s...", timeout, timeUnit));
        try {
            timeUnit.sleep(timeout);
        } catch (InterruptedException e) {
            EmptyStatement.ignore(e);
        }
    }

    public static <K, V> ICache<K, V> getCache(HazelcastInstance hazelcastInstance, String cacheName) {
        return getCache(createCacheManager(hazelcastInstance), cacheName);
    }

    @SuppressWarnings("unchecked")
    public static <K, V> ICache<K, V> getCache(HazelcastCacheManager cacheManager, String cacheName) {
        return cacheManager.getCache(cacheName).unwrap(ICache.class);
    }

    public static HazelcastCacheManager createCacheManager(HazelcastInstance hazelcastInstance) {
        if (isMemberNode(hazelcastInstance)) {
            return createCacheManager(hazelcastInstance, new HazelcastServerCachingProvider());
        } else {
            return createCacheManager(hazelcastInstance, new HazelcastClientCachingProvider());
        }
    }

    public static HazelcastCacheManager createCacheManager(HazelcastInstance hazelcastInstance, CachingProvider cachingProvider) {
        if (isMemberNode(hazelcastInstance)) {
            return createCacheManager(hazelcastInstance, (HazelcastServerCachingProvider) cachingProvider);
        } else {
            return createCacheManager(hazelcastInstance, (HazelcastClientCachingProvider) cachingProvider);
        }
    }

    static HazelcastServerCacheManager createCacheManager(HazelcastInstance instance, HazelcastServerCachingProvider hcp) {
        if (hcp == null) {
            hcp = new HazelcastServerCachingProvider();
        }
        return new HazelcastServerCacheManager(hcp, instance, hcp.getDefaultURI(), hcp.getDefaultClassLoader(), null);
    }

    static HazelcastClientCacheManager createCacheManager(HazelcastInstance instance, HazelcastClientCachingProvider hcp) {
        if (hcp == null) {
            hcp = new HazelcastClientCachingProvider();
        }
        return new HazelcastClientCacheManager(hcp, instance, hcp.getDefaultURI(), hcp.getDefaultClassLoader(), null);
    }
}
