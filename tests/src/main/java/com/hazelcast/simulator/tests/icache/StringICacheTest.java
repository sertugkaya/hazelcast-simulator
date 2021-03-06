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
package com.hazelcast.simulator.tests.icache;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.probes.Probe;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.TestRunner;
import com.hazelcast.simulator.test.annotations.RunWithWorker;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.Warmup;
import com.hazelcast.simulator.tests.helpers.KeyLocality;
import com.hazelcast.simulator.worker.loadsupport.Streamer;
import com.hazelcast.simulator.worker.loadsupport.StreamerFactory;
import com.hazelcast.simulator.worker.selector.OperationSelectorBuilder;
import com.hazelcast.simulator.worker.tasks.AbstractWorkerWithMultipleProbes;

import javax.cache.Cache;
import javax.cache.CacheManager;
import java.util.Random;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.waitClusterSize;
import static com.hazelcast.simulator.tests.helpers.KeyUtils.generateStringKeys;
import static com.hazelcast.simulator.tests.icache.helpers.CacheUtils.createCacheManager;
import static com.hazelcast.simulator.utils.GeneratorUtils.generateStrings;

public class StringICacheTest {

    private static final ILogger LOGGER = Logger.getLogger(StringICacheTest.class);

    private enum Operation {
        PUT,
        GET
    }

    // properties
    public String basename = StringICacheTest.class.getSimpleName();
    public int keyLength = 10;
    public int valueLength = 10;
    public int keyCount = 10000;
    public int valueCount = 10000;
    // if we use the putAndGet (so returning a value) or the put (which returns void)
    public boolean useGetAndPut = true;
    public KeyLocality keyLocality = KeyLocality.SHARED;
    public int minNumberOfMembers = 0;
    public double putProb = 0.1;

    private final OperationSelectorBuilder<Operation> operationSelectorBuilder = new OperationSelectorBuilder<Operation>();

    private HazelcastInstance hazelcastInstance;
    private Cache<String, String> cache;
    private String[] keys;
    private String[] values;

    @Setup
    public void setup(TestContext testContext) {
        hazelcastInstance = testContext.getTargetInstance();

        CacheManager cacheManager = createCacheManager(hazelcastInstance);
        cache = cacheManager.getCache(basename);

        operationSelectorBuilder.addOperation(Operation.PUT, putProb)
                                .addDefaultOperation(Operation.GET);
    }

    @Teardown
    public void teardown() {
        cache.close();
    }

    @Warmup(global = false)
    public void warmup() {
        waitClusterSize(LOGGER, hazelcastInstance, minNumberOfMembers);

        keys = generateStringKeys(keyCount, keyLength, keyLocality, hazelcastInstance);
        values = generateStrings(valueCount, valueLength);

        Random random = new Random();
        Streamer<String, String> streamer = StreamerFactory.getInstance(cache);
        for (String key : keys) {
            String value = values[random.nextInt(valueCount)];
            streamer.pushEntry(key, value);
        }
        streamer.await();
    }

    @RunWithWorker
    public Worker createWorker() {
        return new Worker();
    }

    private class Worker extends AbstractWorkerWithMultipleProbes<Operation> {

        public Worker() {
            super(operationSelectorBuilder);
        }

        @Override
        public void timeStep(Operation operation, Probe probe) {
            String key = randomKey();
            long started;

            switch (operation) {
                case PUT:
                    String value = randomValue();
                    started = System.nanoTime();
                    if (useGetAndPut) {
                        cache.getAndPut(key, value);
                    } else {
                        cache.put(key, value);
                    }
                    probe.done(started);
                    break;
                case GET:
                    started = System.nanoTime();
                    cache.get(key);
                    probe.done(started);
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private String randomValue() {
            return values[randomInt(values.length)];
        }

        private String randomKey() {
            int length = keys.length;
            return keys[randomInt(length)];
        }
    }

    public static void main(String[] args) throws Exception {
        StringICacheTest test = new StringICacheTest();
        new TestRunner<StringICacheTest>(test).run();
    }
}
