package com.hazelcast.simulator.tests.BIST;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;
import com.hazelcast.simulator.utils.GeneratorUtils;

import java.io.IOException;
import java.util.Random;

/**
 * Created by sertugkaya on 09/10/15.
 */
public class SomeObject
        implements IdentifiedDataSerializable {
    private static Random rand = new Random();
    private final static int n = 150000;

    public String s1;
    public String s2;
    public String s3;
    public String s4;
    public String s5;
    public String s6;

    public int i1;
    public int i2;
    public int i3;
    public int i4;

    public double d1;
    public double d2;
    public double d3;
    public double d4;
    public double d5;
    public double d6;
    public double d7;
    public double d8;
    public double d9;
    public double d10;
    public double d11;
    public double d12;
    public double d13;
    public double d14;

    public SomeObject() {
        this.s1 = GeneratorUtils.generateString(1);
        this.s2 = GeneratorUtils.generateString(2);
        this.s3 = GeneratorUtils.generateString(3);
        this.s4 = GeneratorUtils.generateString(4);
        this.s5 = GeneratorUtils.generateString(5);
        this.s6 = GeneratorUtils.generateString(6);
        this.i1 = rand.nextInt(n);
        this.i2 = rand.nextInt(n);
        this.i3 = rand.nextInt(n);
        this.i4 = rand.nextInt(n);
        this.d1 = rand.nextDouble();
        this.d2 = rand.nextDouble();
        this.d3 = rand.nextDouble();
        this.d4 = rand.nextDouble();
        this.d5 = rand.nextDouble();
        this.d6 = rand.nextDouble();
        this.d7 = rand.nextDouble();
        this.d8 = rand.nextDouble();
        this.d9 = rand.nextDouble();
        this.d10 = rand.nextDouble();
        this.d11 = rand.nextDouble();
        this.d12 = rand.nextDouble();
        this.d13 = rand.nextDouble();
        this.d14 = rand.nextDouble();
    }

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
        out.writeUTF(s1);
        out.writeUTF(s2);
        out.writeUTF(s3);
        out.writeUTF(s4);
        out.writeUTF(s5);
        out.writeUTF(s6);
        out.writeInt(i1);
        out.writeInt(i2);
        out.writeInt(i3);
        out.writeInt(i4);
        out.writeDouble(d1);
        out.writeDouble(d2);
        out.writeDouble(d3);
        out.writeDouble(d4);
        out.writeDouble(d5);
        out.writeDouble(d6);
        out.writeDouble(d7);
        out.writeDouble(d8);
        out.writeDouble(d9);
        out.writeDouble(d10);
        out.writeDouble(d11);
        out.writeDouble(d12);
        out.writeDouble(d13);
        out.writeDouble(d14);
    }

    @Override
    public void readData(ObjectDataInput in)
            throws IOException {
        this.s1 = in.readUTF();
        this.s2 = in.readUTF();
        this.s3 = in.readUTF();
        this.s4 = in.readUTF();
        this.s5 = in.readUTF();
        this.s6 = in.readUTF();

        this.i1 = in.readInt();
        this.i2 = in.readInt();
        this.i3 = in.readInt();
        this.i4 = in.readInt();

        this.d1 = in.readDouble();
        this.d2 = in.readDouble();
        this.d3 = in.readDouble();
        this.d4 = in.readDouble();
        this.d5 = in.readDouble();
        this.d6 = in.readDouble();
        this.d7 = in.readDouble();
        this.d8 = in.readDouble();
        this.d9 = in.readDouble();
        this.d10 = in.readDouble();
        this.d11 = in.readDouble();
        this.d12 = in.readDouble();
        this.d13 = in.readDouble();
        this.d14 = in.readDouble();


    }
}
