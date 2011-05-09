package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public class NullObj extends SimpleObj<NullObj> implements ConvertableObj<NullObj> {
  public NullObj(int value) {
    super(value);
  }

  public NullObj() {}

  @Override
  public IntObj toInt() {
    return new IntObj(0);
  }

  @Override
  public BoolObj toBool() {
    return new BoolObj(BoolObj.FALSE);
  }

  @Override
  public StrObj toStr() {
    return new StrObj("null");
  }

  @Override
  public ObjectObj toObject() {
    return null;
  }

  @Override
  public NullObj union(NullObj rValue) {
    return null;
  }
}
