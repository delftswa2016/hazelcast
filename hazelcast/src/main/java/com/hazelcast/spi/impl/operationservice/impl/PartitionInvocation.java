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

package com.hazelcast.spi.impl.operationservice.impl;

import com.hazelcast.core.ExecutionCallback;
import com.hazelcast.nio.Address;
import com.hazelcast.spi.ExceptionAction;
import com.hazelcast.spi.Operation;
import com.hazelcast.spi.partition.IPartition;

/**
 * A {@link Invocation} evaluates a Operation Invocation for a particular partition running on top of the
 * {@link OperationServiceImpl}.
 */
public final class PartitionInvocation extends Invocation {

    public PartitionInvocation(OperationServiceImpl operationService, Operation op, int tryCount, long tryPauseMillis,
                               long callTimeout, ExecutionCallback callback, boolean resultDeserialized) {
        super(operationService, op, tryCount, tryPauseMillis,
                callTimeout, callback, resultDeserialized);
    }

    @Override
    public Address getTarget() {
        IPartition partition = nodeEngine.getPartitionService().getPartition(op.getPartitionId());
        return partition.getReplicaAddress(op.getReplicaIndex());
    }

    @Override
    ExceptionAction onException(Throwable t) {
        ExceptionAction action = op.onInvocationException(t);
        return action != null ? action : ExceptionAction.THROW_EXCEPTION;
    }
}
