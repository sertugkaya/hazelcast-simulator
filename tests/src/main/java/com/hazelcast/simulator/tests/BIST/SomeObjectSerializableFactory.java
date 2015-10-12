package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.nio.serialization.DataSerializableFactory;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

/**
 * Created by sertugkaya on 10/10/15.
 */
public class SomeObjectSerializableFactory implements DataSerializableFactory{
    public static final int FACTORY_ID = 2;
    public static final int SOMEOBJECT_ID = 2;

    @Override
    public IdentifiedDataSerializable create(int typeId) {
        if ( typeId == SOMEOBJECT_ID ) {
            return new SomeObject();
        } else {
            return null;
        }
    }
}
