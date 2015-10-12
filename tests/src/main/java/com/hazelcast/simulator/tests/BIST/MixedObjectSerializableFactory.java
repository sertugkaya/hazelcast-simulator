package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

/**
 * Created by sertugkaya on 10/10/15.
 */
public class MixedObjectSerializableFactory implements DataSerializableFactory {
    public static final int FACTORY_ID = 1;
    public static final int MIXEDOBJECT_ID = 1;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        if ( typeId == MIXEDOBJECT_ID ) {
            return new MixedObject();
        } else {
            return null;
        }
    }
}
