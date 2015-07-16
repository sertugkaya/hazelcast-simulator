package com.hazelcast.simulator.tests.map;

import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MapStoreConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.annotations.RunWithWorker;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.tests.map.helpers.MapStoreWithCounterPerKey;
import com.hazelcast.simulator.worker.tasks.AbstractMonotonicWorker;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.isClient;
import static com.hazelcast.simulator.utils.CommonUtils.sleepSeconds;
import static junit.framework.Assert.assertEquals;

public class MapEvictAndStoreTest {

    private static final ILogger LOGGER = Logger.getLogger(MapEvictAndStoreTest.class);

    // properties
    public String basename = MapEvictAndStoreTest.class.getSimpleName();

    private HazelcastInstance targetInstance;
    private IMap<Long, String> map;
    private IAtomicLong keyCounter;

    @Setup
    public void setup(TestContext testContext) {
        targetInstance = testContext.getTargetInstance();
        map = targetInstance.getMap(basename);
        keyCounter = targetInstance.getAtomicLong(basename);
    }

    @Verify(global = false)
    public void verify() {
        if (isClient(targetInstance)) {
            return;
        }

        MapConfig mapConfig = targetInstance.getConfig().getMapConfig(basename);
        LOGGER.info(basename + ": MapConfig: " + mapConfig);

        MapStoreConfig mapStoreConfig = mapConfig.getMapStoreConfig();
        LOGGER.info(basename + ": MapStoreConfig: " + mapStoreConfig);

        int sleepSeconds = mapConfig.getTimeToLiveSeconds() * 2 + mapStoreConfig.getWriteDelaySeconds() * 2;
        LOGGER.info("Sleeping for " + sleepSeconds + " seconds to wait for delay and TTL values.");
        sleepSeconds(sleepSeconds);

        MapStoreWithCounterPerKey mapStore = (MapStoreWithCounterPerKey) mapStoreConfig.getImplementation();
        LOGGER.info(basename + ": map size = " + map.size());
        LOGGER.info(basename + ": map store = " + mapStore);

        LOGGER.info(basename + ": Checking if some keys where stored more than once");
        for (Object key : mapStore.keySet()) {
            assertEquals("There were multiple calls to MapStore.store", 1, mapStore.valueOf(key));
        }
    }

    @RunWithWorker
    public Worker createWorker() {
        return new Worker();
    }

    public class Worker extends AbstractMonotonicWorker {

        @Override
        public void timeStep() {
            long key = keyCounter.incrementAndGet();
            map.put(key, "test value");
        }
    }
}