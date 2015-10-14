package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.probes.probes.IntervalProbe;
import com.hazelcast.simulator.probes.probes.impl.HdrProbe;
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
 * Created by sertugkaya on 09/10/15.
 */
public class BIScenario1 {

    // properties
    public int keyCount = 150000;
    public KeyLocality keyLocality = KeyLocality.RANDOM;
    public int numberOfMembers = 2;

    private TestContext testContext;
    private static final ILogger LOGGER = Logger.getLogger(BIScenario1.class);


    private IMap<Integer, SomeObject> tradableMap;
    private IQueue<MixedObject> bapQueue;
    private IQueue<MixedObject> viopQueue;
    private IAtomicLong lastTIPSeqNumAtomicLong;
    private int[] keys;

    //TODO change from latencyProbe?
    public HdrProbe tradeMapGetLatency;
    public HdrProbe tradeMapSetLatency;
    public HdrProbe bapQueueAddLAtency;
    public HdrProbe ViopQueueAddLAtency;
    public HdrProbe totalLatency;

    @Setup
    public void setUp(TestContext testContext) throws Exception {
        LOGGER.info("======== SETUP =========");
        this.testContext = testContext;
        HazelcastInstance targetInstance = testContext.getTargetInstance();
        tradableMap = targetInstance.getMap("TradableMap");
        bapQueue = targetInstance.getQueue("BapQueue");
        viopQueue = targetInstance.getQueue("ViopQueue");
        lastTIPSeqNumAtomicLong = targetInstance.getAtomicLong("LastTIPSeqNumAtomicLong");

    }

    @Teardown
    public void tearDown() throws Exception {
        LOGGER.info("======== TEAR DOWN =========");
        tradableMap.destroy();
        bapQueue.destroy();
        viopQueue.destroy();
        lastTIPSeqNumAtomicLong.destroy();
        LOGGER.info("======== THE END =========");
    }

    @Warmup
    public void warmup() {
        try {
            waitClusterSize(LOGGER, testContext.getTargetInstance(), numberOfMembers);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        keys = generateIntKeys(keyCount, Integer.MAX_VALUE, keyLocality, testContext.getTargetInstance());
        Streamer<Integer, SomeObject> streamer = StreamerFactory.getInstance(tradableMap);
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
            if (tradableMap.size() == 0) {
                throw new RuntimeException("Warmup has not run since the map is not filled correctly, found size: " + tradableMap.size());
            }
        }

        @Override
        protected void timeStep() {
            totalLatency.started();
            Integer key = randomKey();
            tradeMapGetLatency.started();
            SomeObject value = tradableMap.get(key);
            tradeMapGetLatency.done();
            SomeObject newValue = doSomething(value);
            tradeMapSetLatency.started();
            tradableMap.set(key, newValue);
            tradeMapSetLatency.done();
            bapQueueAddLAtency.started();
            bapQueue.add(new MixedObject(value, newValue, key));
            bapQueueAddLAtency.done();
            ViopQueueAddLAtency.started();
            viopQueue.add(new MixedObject(value, newValue, key));
            ViopQueueAddLAtency.done();
            LOGGER.info("lastTIPSeqNumAtomicLong counter: " + lastTIPSeqNumAtomicLong.incrementAndGet());
            totalLatency.done();
        }

        protected void afterRun() {
        }

        private Integer randomKey() {
            return keys[randomInt(keys.length)];
        }

    }

    private SomeObject doSomething(SomeObject value) {
        return value;
    }

    public static void main(String[] args) throws Exception {
        BIScenario1 test = new BIScenario1();
        new TestRunner<BIScenario1>(test).run();
    }
}
