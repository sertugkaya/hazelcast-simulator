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
package com.hazelcast.simulator.tests.map;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;
import com.hazelcast.core.Member;
import com.hazelcast.core.Partition;
import com.hazelcast.core.PartitionService;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.annotations.Run;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.utils.ThreadSpawner;

import java.util.Random;
import java.util.Set;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.isMemberNode;
import static com.hazelcast.simulator.utils.CommonUtils.sleepSeconds;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MapDataIntegrityTest {

    private static final ILogger LOGGER = Logger.getLogger(MapDataIntegrityTest.class);

    // properties
    public String basename = MapDataIntegrityTest.class.getSimpleName();
    public int mapIntegrityThreadCount = 8;
    public int stressThreadCount = 8;
    public int totalIntegrityKeys = 10000;
    public int totalStressKeys = 1000;
    public int valueSize = 1000;
    public boolean mapLoad = true;
    public boolean doRunAsserts = true;

    private TestContext testContext;
    private HazelcastInstance targetInstance;

    private IMap<Integer, byte[]> integrityMap;
    private IMap<Integer, byte[]> stressMap;

    private MapIntegrityThread[] integrityThreads;
    private byte[] value;

    @Setup
    public void setup(TestContext testContext) {
        this.testContext = testContext;
        targetInstance = testContext.getTargetInstance();

        integrityMap = targetInstance.getMap(basename + "Integrity");
        stressMap = targetInstance.getMap(basename + "Stress");

        integrityThreads = new MapIntegrityThread[mapIntegrityThreadCount];
        value = new byte[valueSize];

        Random random = new Random();
        random.nextBytes(value);

        if (mapLoad && isMemberNode(targetInstance)) {
            PartitionService partitionService = targetInstance.getPartitionService();
            final Set<Partition> partitionSet = partitionService.getPartitions();
            for (Partition partition : partitionSet) {
                while (partition.getOwner() == null) {
                    sleepSeconds(1);
                }
            }
            LOGGER.info(format("%s: %d partitions", basename, partitionSet.size()));

            Member localMember = targetInstance.getCluster().getLocalMember();
            for (int i = 0; i < totalIntegrityKeys; i++) {
                Partition partition = partitionService.getPartition(i);
                if (localMember.equals(partition.getOwner())) {
                    integrityMap.put(i, value);
                }
            }
            LOGGER.info(format("%s: integrityMap=%s size=%d", basename, integrityMap.getName(), integrityMap.size()));

            Config config = targetInstance.getConfig();
            MapConfig mapConfig = config.getMapConfig(integrityMap.getName());
            LOGGER.info(format("%s: %s", basename, mapConfig));
        }
    }

    @Verify(global = false)
    public void verify() {
        if (isMemberNode(targetInstance)) {
            LOGGER.info(format("%s: cluster size=%d", basename, targetInstance.getCluster().getMembers().size()));
        }

        LOGGER.info(format("%s: integrityMap=%s size=%d", basename, integrityMap.getName(), integrityMap.size()));
        int totalErrorCount = 0;
        int totalNullValueCount = 0;
        for (MapIntegrityThread integrityThread : integrityThreads) {
            totalErrorCount += integrityThread.sizeErrorCount;
            totalNullValueCount += integrityThread.nullValueCount;
        }
        LOGGER.info(format("%s: total integrityMapSizeErrorCount=%d", basename, totalErrorCount));
        LOGGER.info(format("%s: total integrityMapNullValueCount=%d", basename, totalNullValueCount));

        assertEquals(format("%s: (verify) integrityMap=%s map size", basename, integrityMap.getName()),
                totalIntegrityKeys, integrityMap.size());
        assertEquals(format("%s: (verify) integrityMapSizeErrorCount=", basename), 0, totalErrorCount);
        assertEquals(format("%s: (verify) integrityMapNullValueCount=", basename), 0, totalNullValueCount);
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(basename);
        for (int i = 0; i < mapIntegrityThreadCount; i++) {
            integrityThreads[i] = new MapIntegrityThread();
            spawner.spawn(integrityThreads[i]);
        }
        for (int i = 0; i < stressThreadCount; i++) {
            spawner.spawn(new StressThread());
        }
        spawner.awaitCompletion();
    }

    private class MapIntegrityThread implements Runnable {
        private final Random random = new Random();

        private int nullValueCount = 0;
        private int sizeErrorCount = 0;

        public void run() {
            while (!testContext.isStopped()) {
                int key = random.nextInt(totalIntegrityKeys);
                byte[] val = integrityMap.get(key);
                int actualSize = integrityMap.size();
                if (doRunAsserts) {
                    assertNotNull(format("%s: integrityMap=%s key %s == null", basename, integrityMap.getName(), key), val);
                    assertEquals(format("%s: integrityMap=%s map size", basename, integrityMap.getName()),
                            totalIntegrityKeys, actualSize);
                } else {
                    if (val == null) {
                        nullValueCount++;
                    }
                    if (actualSize != totalIntegrityKeys) {
                        sizeErrorCount++;
                    }
                }
            }
        }
    }

    private class StressThread implements Runnable {
        private final Random random = new Random();

        public void run() {
            while (!testContext.isStopped()) {
                int key = random.nextInt(totalStressKeys);
                stressMap.put(key, value);
            }
        }
    }
}
