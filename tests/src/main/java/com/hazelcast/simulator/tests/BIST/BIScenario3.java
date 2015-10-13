package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.TestRunner;
import com.hazelcast.simulator.test.annotations.RunWithWorker;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.Warmup;
import com.hazelcast.simulator.tests.helpers.KeyLocality;
import com.hazelcast.simulator.worker.loadsupport.Streamer;
import com.hazelcast.simulator.worker.loadsupport.StreamerFactory;
import com.hazelcast.simulator.worker.tasks.AbstractMonotonicWorker;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.waitClusterSize;
import static com.hazelcast.simulator.tests.helpers.KeyUtils.generateIntKeys;

/**
 * Created by sertugkaya on 10/10/15.
 */
public class BIScenario3 {

    // properties
    public int keyCount = 5000;
    public KeyLocality keyLocality = KeyLocality.RANDOM;
    public int numberOfMembers = 2;

    private TestContext testContext;
    private static final ILogger LOGGER = Logger.getLogger(BIScenario3.class);


    private IMap<Integer, SomeObject> indexMap;
    private IQueue<MixedObject> viopQueue;
    private IAtomicLong lastTIPSeqNumAtomicLong;
    private int[] keys;

    @Setup
    public void setUp(TestContext testContext) throws Exception {
        LOGGER.info("======== SETUP =========");
        this.testContext = testContext;
        HazelcastInstance targetInstance = testContext.getTargetInstance();
        indexMap = targetInstance.getMap("TradableMap");
        viopQueue = targetInstance.getQueue("ViopQueue");
        lastTIPSeqNumAtomicLong = targetInstance.getAtomicLong("LastTIPSeqNumAtomicLong");

    }

    @Teardown
    public void tearDown() throws Exception {
        LOGGER.info("======== TEAR DOWN =========");
        indexMap.destroy();
        viopQueue.destroy();
        lastTIPSeqNumAtomicLong.destroy();
        LOGGER.info("======== THE END =========");
    }

    @Warmup(global = true)
    public void warmup() {
        try {
            waitClusterSize(LOGGER, testContext.getTargetInstance(), numberOfMembers);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        keys = generateIntKeys(keyCount, Integer.MAX_VALUE, keyLocality, testContext.getTargetInstance());
        Streamer<Integer, SomeObject> streamer = StreamerFactory.getInstance(indexMap);
        for (int key : keys) {
            SomeObject value = new SomeObject();
            streamer.pushEntry(key, value);
        }
        streamer.await();
    }

    @RunWithWorker
    public Worker createWorker() {
        return new Worker();
    }

    private class Worker extends AbstractMonotonicWorker {

        protected void beforeRun() {
            if (indexMap.size() != keyCount) {
                throw new RuntimeException("Warmup has not run since the map is not filled correctly, found size: " + indexMap.size());
            }
        }

        @Override
        protected void timeStep() {
            Integer key = randomInt(keyCount);
            SomeObject value = indexMap.get(key);
            SomeObject newValue = doSomething(value);
            indexMap.set(key, newValue);
            viopQueue.add(new MixedObject(value, newValue, key));
            System.out.println("lastTIPSeqNumAtomicLong counter: " + lastTIPSeqNumAtomicLong.incrementAndGet());
        }

        protected void afterRun() {
        }

    }

    private SomeObject doSomething(SomeObject value) {
        return value;
    }

    public static void main(String[] args) throws Exception {
        BIScenario3 test = new BIScenario3();
        new TestRunner<BIScenario3>(test).run();
    }


}
