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
package com.hazelcast.simulator.tests.map.queryresultsize;

import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.TestRunner;
import com.hazelcast.simulator.test.annotations.RunWithWorker;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.test.annotations.Warmup;
import com.hazelcast.simulator.worker.tasks.IWorker;

/**
 * This test verifies that {@link com.hazelcast.core.IMap#values()}, {@link com.hazelcast.core.IMap#keySet()} and
 * {@link com.hazelcast.core.IMap#entrySet()} throw an exception if the configured result size limit is exceeded.
 *
 * To achieve this we fill a map slightly above the trigger limit of the query result size limit, so we are sure it will always
 * trigger the exception. Then we call the map methods and verify that each call created an exception.
 *
 * The test can be configured to use {@link String} or {@link Integer} keys. You can also override the number of filled items
 * with the {@link #keyCount} property, e.g. to test for missing exceptions with enabled result size limit and an empty map.
 *
 * You have to activate the query result size limit by providing a custom hazelcast.xml with the following setting:
 * <pre>
 *   {@code
 *   <properties>
 *     <property name="hazelcast.query.result.size.limit">100000</property>
 *   </properties>
 *   }
 * </pre>
 *
 * @since Hazelcast 3.5
 */
public class MapResultSizeLimitTest extends AbstractMapTest {

    // properties
    public String basename = MapResultSizeLimitTest.class.getSimpleName();
    public String keyType = "String";
    public String operationType = "values";
    public int keyCount = -1;

    @Setup
    public void setUp(TestContext testContext) {
        baseSetup(testContext, basename);

        failOnVersionMismatch();
    }

    @Override
    long getGlobalKeyCount(Integer minResultSizeLimit, Float resultLimitFactor) {
        if (keyCount > -1) {
            return keyCount;
        }
        return Math.round(minResultSizeLimit * resultLimitFactor * 1.1);
    }

    @Warmup
    public void warmup() {
        baseWarmup(keyType);
    }

    @Verify(global = true)
    public void globalVerify() {
        baseVerify(true);
    }

    @RunWithWorker
    public IWorker run() {
        return baseRunWithWorker(operationType);
    }

    public static void main(String[] args) throws Exception {
        new TestRunner<MapResultSizeLimitTest>(new MapResultSizeLimitTest()).run();
    }
}
