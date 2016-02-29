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

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IList;
import com.hazelcast.core.IMap;
import com.hazelcast.core.TransactionalMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.annotations.Run;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.test.annotations.Warmup;
import com.hazelcast.simulator.tests.helpers.KeyIncrementPair;
import com.hazelcast.simulator.tests.helpers.TxnCounter;
import com.hazelcast.simulator.utils.ThreadSpawner;
import com.hazelcast.transaction.TransactionContext;
import com.hazelcast.transaction.TransactionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.rethrow;
import static org.junit.Assert.assertEquals;

/**
 * Testing transaction context with multi keys.
 *
 * A number of map keys (maxKeysPerTxn) are chosen at random to take part in the transaction. As maxKeysPerTxn increases as a
 * proportion of keyCount, more conflict will occur between the transactions, less transactions will be committed successfully and
 * more transactions are rolled back.
 */
public class MapTransactionContextConflictTest {

    private static final int MAX_INCREMENT = 999;

    private static final ILogger LOGGER = Logger.getLogger(MapTransactionContextConflictTest.class);

    public String basename = MapTransactionContextConflictTest.class.getSimpleName();
    public int threadCount = 3;
    public int keyCount = 50;
    public int maxKeysPerTxn = 5;
    public boolean throwCommitException = false;
    public boolean throwRollBackException = false;

    private HazelcastInstance targetInstance;
    private TestContext testContext;

    @Setup
    public void setup(TestContext testContext) {
        this.testContext = testContext;
        targetInstance = testContext.getTargetInstance();
    }

    @Warmup(global = true)
    public void warmup() {
        IMap<Integer, Long> map = targetInstance.getMap(basename);
        for (int i = 0; i < keyCount; i++) {
            map.put(i, 0L);
        }
    }

    @Run
    public void run() {
        ThreadSpawner spawner = new ThreadSpawner(basename);
        for (int i = 0; i < threadCount; i++) {
            spawner.spawn(new Worker());
        }
        spawner.awaitCompletion();
    }

    private class Worker implements Runnable {

        private final Random random = new Random();
        private final long[] localIncrements = new long[keyCount];
        private final TxnCounter count = new TxnCounter();

        @Override
        @SuppressWarnings("PMD.PreserveStackTrace")
        public void run() {
            while (!testContext.isStopped()) {

                List<KeyIncrementPair> potentialIncrements = new ArrayList<KeyIncrementPair>();
                for (int i = 0; i < maxKeysPerTxn; i++) {
                    KeyIncrementPair p = new KeyIncrementPair(random, keyCount, MAX_INCREMENT);
                    potentialIncrements.add(p);
                }

                List<KeyIncrementPair> putIncrements = new ArrayList<KeyIncrementPair>();

                TransactionContext context = targetInstance.newTransactionContext();
                try {
                    context.beginTransaction();
                    TransactionalMap<Integer, Long> map = context.getMap(basename);

                    for (KeyIncrementPair p : potentialIncrements) {
                        long current = map.getForUpdate(p.key);
                        map.put(p.key, current + p.increment);

                        putIncrements.add(p);
                    }
                    context.commitTransaction();

                    // Do local key increments if commit is successful
                    count.committed++;
                    for (KeyIncrementPair p : putIncrements) {
                        localIncrements[p.key] += p.increment;
                    }
                } catch (TransactionException commitException) {
                    LOGGER.warning(basename + ": commit failed. tried key increments=" + putIncrements, commitException);
                    if (throwCommitException) {
                        throw rethrow(commitException);
                    }

                    try {
                        context.rollbackTransaction();
                        count.rolled++;
                    } catch (TransactionException rollBackException) {
                        LOGGER.warning(basename + ": rollback failed " + rollBackException.getMessage(), rollBackException);
                        count.failedRollbacks++;

                        if (throwRollBackException) {
                            throw rethrow(rollBackException);
                        }
                    }
                }
            }
            targetInstance.getList(basename + "inc").add(localIncrements);
            targetInstance.getList(basename + "count").add(count);
        }
    }

    @Verify(global = false)
    public void verify() {
        IList<TxnCounter> counts = targetInstance.getList(basename + "count");
        TxnCounter total = new TxnCounter();
        for (TxnCounter c : counts) {
            total.add(c);
        }
        LOGGER.info(basename + ": " + total + " from " + counts.size() + " worker threads");

        IList<long[]> allIncrements = targetInstance.getList(basename + "inc");
        long[] expected = new long[keyCount];
        for (long[] incs : allIncrements) {
            for (int i = 0; i < incs.length; i++) {
                expected[i] += incs[i];
            }
        }

        IMap<Integer, Long> map = targetInstance.getMap(basename);
        int failures = 0;
        for (int i = 0; i < keyCount; i++) {
            if (expected[i] != map.get(i)) {
                failures++;
                LOGGER.info(basename + ": key=" + i + " expected " + expected[i] + " != " + "actual " + map.get(i));
            }
        }
        assertEquals(basename + ": " + failures + " key=>values have been incremented unExpected", 0, failures);
    }

}
