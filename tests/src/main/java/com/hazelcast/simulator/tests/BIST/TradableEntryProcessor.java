package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.map.AbstractEntryProcessor;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

import java.io.IOException;
import java.util.Map;

/**
 * Created by sertugkaya on 15/10/15.
 */
public class TradableEntryProcessor
         implements IdentifiedDataSerializable, EntryProcessor<Integer,SomeObject>, EntryBackupProcessor<Integer, SomeObject> {

    private SomeObject oneObject;

    public TradableEntryProcessor(SomeObject oneObject) {
        this.oneObject = oneObject;
    }

    public TradableEntryProcessor() {
    }

    @Override
    public Object process(Map.Entry<Integer, SomeObject> entry) {
        SomeObject entryValue = entry.getValue();
        entryValue.i1 = entryValue.i1 + 5;
        entry.setValue(entryValue);
        return entryValue;
    }

    @Override
    public EntryBackupProcessor<Integer, SomeObject> getBackupProcessor() {
        return this;
    }

    @Override
    public int getFactoryId() {
        return 3;
    }

    @Override
    public int getId() {
        return 3;
    }

    @Override
    public void writeData(ObjectDataOutput objectDataOutput)
            throws IOException {
        oneObject.writeData(objectDataOutput);

    }

    @Override
    public void readData(ObjectDataInput objectDataInput)
            throws IOException {
        SomeObject someObject = new SomeObject();
        someObject.readData(objectDataInput);
        this.oneObject = someObject;

    }

    @Override
    public void processBackup(Map.Entry<Integer, SomeObject> entry) {
        process(entry);
    }
}
