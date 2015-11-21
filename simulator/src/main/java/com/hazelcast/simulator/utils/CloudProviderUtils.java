/*
 * Copyright (c) 2008-2015, Hazelcast, Inc. All Rights Reserved.
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
package com.hazelcast.simulator.utils;

public final class CloudProviderUtils {

    public static final String PROVIDER_STATIC = "static";
    public static final String PROVIDER_EC2 = "aws-ec2";
    public static final String PROVIDER_GCE = "google-compute-engine";

    private CloudProviderUtils() {
    }

    public static boolean isStatic(String cloudProvider) {
        return PROVIDER_STATIC.equals(cloudProvider);
    }

    public static boolean isEC2(String cloudProvider) {
        return PROVIDER_EC2.equals(cloudProvider);
    }

    public static boolean isGCE(String cloudProvider) {
        return PROVIDER_GCE.equals(cloudProvider);
    }
}
