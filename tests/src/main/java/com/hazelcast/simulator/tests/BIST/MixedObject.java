package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;

/**
 * Created by sertugkaya on 09/10/15.
 */
public class MixedObject implements IdentifiedDataSerializable {

    private Integer id;
    private SomeObject oldValue;
    private SomeObject newValue;

    public MixedObject(SomeObject value, SomeObject newValue) {
        this.oldValue = value;
        this.newValue = value;
    }

    public MixedObject() {

    }

    @Override
    public int getFactoryId() {
        return MixedObjectSerializableFactory.FACTORY_ID;
    }

    @Override
    public int getId() {
        return MixedObjectSerializableFactory.MIXEDOBJECT_ID;
    }

    @Override
    public void writeData(ObjectDataOutput out)
            throws IOException {
        out.writeInt(id);
        oldValue.writeData(out);
        newValue.writeData(out);

    }

    @Override
    public void readData(ObjectDataInput in)
            throws IOException {
        this.id = in.readInt();
        SomeObject oldOne = new SomeObject();
        oldOne.readData(in);
        this.oldValue = oldOne;
        SomeObject newOne = new SomeObject();
        newOne.readData(in);
        this.newValue = newOne;

    }
}
