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
package com.hazelcast.simulator.protocol.exception;

import com.hazelcast.simulator.protocol.core.SimulatorAddress;
import com.hazelcast.simulator.protocol.operation.ExceptionOperation;

import java.io.File;
import java.io.IOException;

import static com.hazelcast.simulator.utils.FileUtils.rename;
import static com.hazelcast.simulator.utils.FileUtils.writeText;
import static com.hazelcast.simulator.utils.FormatUtils.NEW_LINE;

/**
 * Stores exception to local files.
 */
public class FileExceptionLogger extends AbstractExceptionLogger {

    public FileExceptionLogger(SimulatorAddress localAddress, ExceptionType exceptionType) {
        super(localAddress, exceptionType);
    }

    @Override
    protected void handleOperation(long exceptionId, ExceptionOperation operation) {
        String targetFileName = exceptionId + ".exception";

        File tmpFile = new File(targetFileName + ".tmp");
        try {
            if (!tmpFile.createNewFile()) {
                throw new IOException("Could not create tmp file: " + tmpFile.getAbsolutePath());
            }
        } catch (IOException e) {
            LOGGER.fatal("Could not report exception; this means that this exception is not visible to the coordinator", e);
            return;
        }

        writeText(operation.getTestId() + NEW_LINE + operation.getStacktrace(), tmpFile);

        File file = new File(targetFileName);
        rename(tmpFile, file);

        LOGGER.warn(operation.getConsoleLog(exceptionId));
    }
}
