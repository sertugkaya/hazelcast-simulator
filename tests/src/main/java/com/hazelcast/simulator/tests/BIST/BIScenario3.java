package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import com.hazelcast.core.IMap;
import com.hazelcast.core.IQueue;
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
import com.hazelcast.simulator.worker.tasks.AbstractMonotonicWorker;

import static com.hazelcast.simulator.tests.helpers.KeyUtils.generateIntKeys;

/**
 * Created by sertugkaya on 10/10/15.
 */
public class BIScenario3 {

    // properties
    public int keyCount = 50000;
    public KeyLocality keyLocality = KeyLocality.RANDOM;

    private TestContext testContext;
    private static final ILogger LOGGER = Logger.getLogger(BIScenario3.class);


    private IMap<Integer, SomeObject> indexMap;
    private IAtomicLong lastTIPSeqNumAtomicLong;
    private int[] keys;

    public Probe indexMapGetLatency;
    public Probe indexMapSetLatency;

    @Setup
    public void setUp(TestContext testContext) throws Exception {
        LOGGER.info("======== SETUP =========");
        this.testContext = testContext;
        HazelcastInstance targetInstance = testContext.getTargetInstance();
        indexMap = targetInstance.getMap("IndexMap");
        lastTIPSeqNumAtomicLong = targetInstance.getAtomicLong("LastTIPSeqNumAtomicLong");

    }

    @Teardown
    public void tearDown() throws Exception {
        LOGGER.info("======== TEAR DOWN =========");
        indexMap.destroy();
        lastTIPSeqNumAtomicLong.destroy();
        LOGGER.info("======== THE END =========");
    }

    @Warmup
    public void warmup() {
        keys = generateIntKeys(keyCount, keyLocality, testContext.getTargetInstance());
        Streamer<Integer, SomeObject> streamer = StreamerFactory.getInstance(indexMap);
        for (int key : keys) {
            SomeObject value = new SomeObject();
            value.populate();
            streamer.pushEntry(key, value);
        }
        streamer.await();
    }

    @RunWithWorker
    public Worker createWorker() {
        return new Worker();
    }

    private class Worker extends AbstractMonotonicWorker {

        @Override
        protected void timeStep() {
            Integer key = randomKey();
            indexMapGetLatency.started();
            SomeObject value = indexMap.get(key);
            indexMapGetLatency.done();
            SomeObject newValue = doSomething(value);
            indexMapSetLatency.started();
            indexMap.set(key, newValue);
            indexMapSetLatency.done();
            lastTIPSeqNumAtomicLong.set(new Long(key));
        }

        private Integer randomKey() {
            return keys[randomInt(keys.length)];
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
