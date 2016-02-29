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
package com.hazelcast.simulator.tests.special;

import com.hazelcast.core.IMap;
import com.hazelcast.logging.ILogger;
import com.hazelcast.logging.Logger;
import com.hazelcast.simulator.test.TestContext;
import com.hazelcast.simulator.test.TestException;
import com.hazelcast.simulator.test.TestPhase;
import com.hazelcast.simulator.test.TestRunner;
import com.hazelcast.simulator.test.annotations.Run;
import com.hazelcast.simulator.test.annotations.Setup;
import com.hazelcast.simulator.test.annotations.Teardown;
import com.hazelcast.simulator.test.annotations.Verify;
import com.hazelcast.simulator.test.annotations.Warmup;
import com.hazelcast.simulator.utils.EmptyStatement;
import com.hazelcast.simulator.utils.ExceptionReporter;

import java.util.LinkedList;
import java.util.List;

import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.isClient;
import static com.hazelcast.simulator.tests.helpers.HazelcastTestUtils.isMemberNode;
import static com.hazelcast.simulator.utils.CommonUtils.exitWithError;
import static org.junit.Assert.fail;

/**
 * A test that causes a failure. This is useful for testing the simulator framework and for demonstration purposes.
 */
public class FailingTest {

    public enum Failure {
        EXCEPTION,
        ERROR,
        NPE,
        FAIL,
        OOME,
        EXIT
    }

    public enum Selection {
        ALL,
        ONE_PER_NODE,
        ONE_PER_CLUSTER
    }

    public enum Type {
        ALL,
        MEMBER,
        CLIENT
    }

    private static final ILogger LOGGER = Logger.getLogger(FailingTest.class);

    // properties
    public TestPhase testPhase = TestPhase.RUN;
    public Failure failure = Failure.EXCEPTION;
    public Selection selection = Selection.ALL;
    public Type type = Type.ALL;
    public boolean throwError = false;

    private TestContext testContext;
    private boolean isSelected;

    @Setup
    public void setUp(TestContext testContext) throws Exception {
        this.testContext = testContext;

        if (matchingType(type, testContext)) {
            isSelected = isSelected(selection, testContext);
        }

        createFailure(TestPhase.SETUP);
    }

    @Teardown
    public void localTearDown() throws Exception {
        createFailure(TestPhase.LOCAL_TEARDOWN);
    }

    @Teardown(global = true)
    public void globalTearDown() throws Exception {
        createFailure(TestPhase.GLOBAL_TEARDOWN);
    }

    @Warmup
    public void localWarmup() throws Exception {
        createFailure(TestPhase.LOCAL_WARMUP);
    }

    @Warmup(global = true)
    public void globalWarmup() throws Exception {
        createFailure(TestPhase.GLOBAL_WARMUP);
    }

    @Verify(global = false)
    public void localVerify() throws Exception {
        createFailure(TestPhase.LOCAL_VERIFY);
    }

    @Verify
    public void globalVerify() throws Exception {
        createFailure(TestPhase.GLOBAL_VERIFY);
    }

    @Run
    public void run() throws Exception {
        createFailure(TestPhase.RUN);
    }

    private void createFailure(TestPhase currentTestPhase) throws Exception {
        if (!isSelected || testPhase != currentTestPhase) {
            return;
        }

        switch (failure) {
            case EXCEPTION:
                handleThrowable(new TestException("Expected exception"));
                break;
            case ERROR:
                handleThrowable(new AssertionError("Expected error"));
                break;
            case NPE:
                handleThrowable(new NullPointerException("Expected NPE"));
                break;
            case FAIL:
                fail("Wanted failure");
                break;
            case OOME:
                createOOME();
                break;
            case EXIT:
                exitWithError();
                break;
            default:
                throw new UnsupportedOperationException("Unknown failure: " + failure);
        }
    }

    private void handleThrowable(Exception exception) throws Exception {
        if (throwError) {
            throw exception;
        } else {
            ExceptionReporter.report(testContext.getTestId(), exception);
        }
    }

    private void handleThrowable(Error error) {
        if (throwError) {
            throw error;
        } else {
            ExceptionReporter.report(testContext.getTestId(), error);
        }
    }

    private void createOOME() {
        List<byte[]> list = new LinkedList<byte[]>();
        for (; ; ) {
            try {
                list.add(new byte[100 * 1000 * 1000]);
            } catch (OutOfMemoryError ignored) {
                EmptyStatement.ignore(ignored);
                break;
            }
        }
        LOGGER.severe("We should never reach this code! List size: " + list.size());
    }

    private static boolean matchingType(Type type, TestContext testContext) {
        if (type == Type.ALL) {
            return true;
        }
        if (type == Type.MEMBER && isMemberNode(testContext.getTargetInstance())) {
            return true;
        }
        if (type == Type.CLIENT && isClient(testContext.getTargetInstance())) {
            return true;
        }
        return false;
    }

    private static boolean isSelected(Selection selection, TestContext testContext) {
        switch (selection) {
            case ALL:
                return true;
            case ONE_PER_NODE:
                return (getMap(testContext).putIfAbsent(testContext.getPublicIpAddress(), true) == null);
            case ONE_PER_CLUSTER:
                return (getMap(testContext).putIfAbsent(testContext.getTestId(), true) == null);
            default:
                throw new UnsupportedOperationException("Unknown selection type");
        }
    }

    private static IMap<String, Boolean> getMap(TestContext testContext) {
        return testContext.getTargetInstance().getMap("failureSelection" + testContext.getTestId());
    }

    public static void main(String[] args) throws Exception {
        FailingTest test = new FailingTest();
        new TestRunner<FailingTest>(test).run();
    }
}
