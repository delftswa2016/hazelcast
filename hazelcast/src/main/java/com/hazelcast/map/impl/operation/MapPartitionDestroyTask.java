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

package com.hazelcast.map.impl.operation;


import com.hazelcast.map.impl.PartitionContainer;
import com.hazelcast.spi.impl.PartitionSpecificRunnable;
import java.util.concurrent.Semaphore;

public class MapPartitionDestroyTask implements PartitionSpecificRunnable {
    private final PartitionContainer partitionContainer;
    private final String mapName;
    private Semaphore semaphore;

    public MapPartitionDestroyTask(PartitionContainer partitionContainer, String mapName, Semaphore semaphore) {
        this.partitionContainer = partitionContainer;
        this.mapName = mapName;
        this.semaphore = semaphore;
    }

    @Override
    public void run() {
        partitionContainer.destroyMap(mapName);
        semaphore.release();
    }

    @Override
    public int getPartitionId() {
        return partitionContainer.getPartitionId();
    }
}
