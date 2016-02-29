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
package com.hazelcast.simulator.protocol.core;

import com.hazelcast.simulator.protocol.operation.SimulatorOperation;
import com.hazelcast.simulator.protocol.processors.TestOperationProcessor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.hazelcast.simulator.protocol.core.ResponseType.FAILURE_TEST_NOT_FOUND;

/**
 * Manages {@link TestOperationProcessor} instances for tests.
 */
public class TestProcessorManager {

    private final Map<Integer, TestOperationProcessor> testProcessors = new ConcurrentHashMap<Integer, TestOperationProcessor>();

    private final SimulatorAddress localAddress;

    public TestProcessorManager(SimulatorAddress localAddress) {
        this.localAddress = localAddress;
    }

    public TestOperationProcessor getTest(int testIndex) {
        return testProcessors.get(testIndex);
    }

    public void addTest(int testIndex, TestOperationProcessor processor) {
        testProcessors.put(testIndex, processor);
    }

    public void removeTest(int testIndex) {
        testProcessors.remove(testIndex);
    }

    public void processOnAllTests(Response response, SimulatorOperation operation, SimulatorAddress source) {
        for (Map.Entry<Integer, TestOperationProcessor> entry : testProcessors.entrySet()) {
            TestOperationProcessor processor = entry.getValue();
            processOperation(processor, response, operation, source);
        }
    }

    public void processOnTest(Response response, SimulatorOperation operation, SimulatorAddress source, int testAddressIndex) {
        TestOperationProcessor processor = testProcessors.get(testAddressIndex);
        if (processor == null) {
            response.addResponse(localAddress, FAILURE_TEST_NOT_FOUND);
        } else {
            processOperation(processor, response, operation, source);
        }
    }

    private void processOperation(TestOperationProcessor processor, Response response, SimulatorOperation operation,
                                  SimulatorAddress source) {
        SimulatorAddress testAddress = processor.getTestAddress();
        ResponseType responseType = processor.process(operation, source);
        response.addResponse(testAddress, responseType);
    }
}
