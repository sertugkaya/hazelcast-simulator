package com.hazelcast.simulator.tests.map;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.annotations.RunWithWorker;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.worker.tasks.AbstractMonotonicWorker;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionOptions;
import com.hazelcast.transaction.TransactionOptions.TransactionType;

import java.util.concurrent.ThreadLocalRandom;

public class MapTransactionContextTest {

    private static final ILogger LOGGER = Logger.getLogger(MapTransactionContextTest.class);

    // properties
    public TransactionType transactionType = TransactionType.TWO_PHASE;
    public int durability = 1;
    public int range = 1000;
    public boolean failOnException = false;
    private HazelcastInstance hz;

    @Setup
    public void setup(TestContext testContext) throws Exception {
        hz = testContext.getTargetInstance();
    }

    @RunWithWorker
    public Worker createWorker() {
        return new Worker();
    }

    private class Worker extends AbstractMonotonicWorker {

        @Override
        protected void timeStep() {
            if(true){
                throw new RuntimeException();
            }

            int key = nextRandom(0, range / 2);

            TransactionOptions transactionOptions = new TransactionOptions().setTransactionType(transactionType).setDurability(durability);

            TransactionContext transactionContext = hz.newTransactionContext(transactionOptions);

            transactionContext.beginTransaction();

            TransactionalMap<Object, Object> txMap = transactionContext.getMap("map");

            try {
                Object val = txMap.getForUpdate(key);

                if (val != null) {
                    key = nextRandom(range / 2, range);
                }

                txMap.put(key, new Long(key));

                transactionContext.commitTransaction();
            } catch (Exception e) {

                e.printStackTrace();

                transactionContext.rollbackTransaction();

                if (failOnException) {
                    throw new RuntimeException(e);
                }
            }
        }

        protected int nextRandom(int min, int max) {
            return ThreadLocalRandom.current().nextInt(max - min) + min;
        }
    }
}