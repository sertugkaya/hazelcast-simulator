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
package com.hazelcast.simulator.tests.queue;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IQueue;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.TestRunner;
import com.hazelcast.simulator.test.annotations.Run;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.utils.ThreadSpawner;

import java.io.Serializable;
import java.util.Random;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.rethrow;
import static org.junit.Assert.assertEquals;

public class ProducerConsumerTest {

    private static final ILogger LOGGER = Logger.getLogger(ProducerConsumerTest.class);

    // properties
    public String basename = ProducerConsumerTest.class.getSimpleName();
    public int producerCount = 4;
    public int consumerCount = 4;
    public int maxIntervalMillis = 1000;

    private IAtomicLong produced;
    private IQueue<Work> workQueue;
    private IAtomicLong consumed;
    private TestContext testContext;

    @Setup
    public void setup(TestContext testContext) {
        this.testContext = testContext;
        HazelcastInstance targetInstance = testContext.getTargetInstance();

        produced = targetInstance.getAtomicLong(basename + ":Produced");
        consumed = targetInstance.getAtomicLong(basename + ":Consumed");
        workQueue = targetInstance.getQueue(basename + ":WorkQueue");
    }

    @Teardown
    public void teardown() {
        produced.destroy();
        workQueue.destroy();
        consumed.destroy();
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(basename);
        for (int i = 0; i < producerCount; i++) {
            spawner.spawn("ProducerThread", new Producer(i));
        }
        for (int i = 0; i < consumerCount; i++) {
            spawner.spawn("ConsumerThread", new Consumer(i));
        }
        spawner.awaitCompletion();
    }

    @Verify
    public void verify() {
        long expected = workQueue.size() + consumed.get();
        long actual = produced.get();
        assertEquals(expected, actual);
    }

    private class Producer implements Runnable {
        final Random rand = new Random(System.currentTimeMillis());
        final int id;

        public Producer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            long iteration = 0;
            while (!testContext.isStopped()) {
                try {
                    Thread.sleep(rand.nextInt(maxIntervalMillis) * consumerCount);
                    produced.incrementAndGet();
                    workQueue.offer(new Work());

                    iteration++;
                    if (iteration % 10 == 0) {
                        LOGGER.info(String.format(
                                "%s prod-id: %d, iteration: %d, produced: %d, workQueue: %d, consumed: %d",
                                Thread.currentThread().getName(), id, iteration,
                                produced.get(), workQueue.size(), consumed.get()
                        ));
                    }
                } catch (Exception e) {
                    throw rethrow(e);
                }
            }
        }
    }

    private class Consumer implements Runnable {
        Random rand = new Random(System.currentTimeMillis());
        int id;

        public Consumer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            long iteration = 0;
            while (!testContext.isStopped()) {
                try {
                    workQueue.take();
                    consumed.incrementAndGet();
                    Thread.sleep(rand.nextInt(maxIntervalMillis) * producerCount);

                    iteration++;
                    if (iteration % 20 == 0) {
                        LOGGER.info(String.format(
                                "%s prod-id: %d, iteration: %d, produced: %d, workQueue: %d, consumed: %d",
                                Thread.currentThread().getName(), id, iteration,
                                produced.get(), workQueue.size(), consumed.get()
                        ));
                    }
                } catch (Exception e) {
                    throw rethrow(e);
                }
            }
        }
    }

    static class Work implements Serializable {
    }

    public static void main(String[] args) throws Exception {
        ProducerConsumerTest test = new ProducerConsumerTest();
        new TestRunner<ProducerConsumerTest>(test).run();
    }
}
