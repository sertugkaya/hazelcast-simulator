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
package com.hazelcast.simulator.worker;

/**
 * Defines the different types for Simulator Worker components.
 */
public enum WorkerType {

    INTEGRATION_TEST(IntegrationTestWorker.class.getName(), false),

    MEMBER(MemberWorker.class.getName(), true),
    CLIENT(ClientWorker.class.getName(), false);

    private final String className;
    private final boolean isMember;

    WorkerType(String className, boolean isMember) {
        this.className = className;
        this.isMember = isMember;
    }

    public String getClassName() {
        return className;
    }

    public boolean isMember() {
        return isMember;
    }

    public String toLowerCase() {
        return name().toLowerCase();
    }
}
