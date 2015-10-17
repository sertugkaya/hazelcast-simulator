package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

/**
 * Created by sertugkaya on 16/10/15.
 */
public class TradableEntryProcessorFactory
        implements DataSerializableFactory {
    public static final int FACTORY_ID = 3;
    public static final int TRADEABLE_ID = 3;

    @Override
    public IdentifiedDataSerializable create(int i) {
        if (i == TradableEntryProcessorFactory.TRADEABLE_ID){
            return new TradableEntryProcessor();
        } else {
            return null;
        }
    }
}
