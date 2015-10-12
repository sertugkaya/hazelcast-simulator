package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;

/**
 * Created by sertugkaya on 09/10/15.
 */
public class SomeObject implements IdentifiedDataSerializable {
    @Override
    public int getFactoryId() {
        return SomeObjectSerializableFactory.FACTORY_ID;
    }

    @Override
    public int getId() {
        return SomeObjectSerializableFactory.SOMEOBJECT_ID;
    }

    @Override
    public void writeData(ObjectDataOutput out)
            throws IOException {

    }

    @Override
    public void readData(ObjectDataInput in)
            throws IOException {

    }
}
