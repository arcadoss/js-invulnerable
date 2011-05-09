package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public class UndefObj extends SimpleObj<UndefObj> implements ConvertableObj<UndefObj> {
  public UndefObj(int value) {
    super(value);
  }

  public UndefObj() {
  }

  @Override
  public IntObj toInt() {
    // todo : implement correct conversion
    return new IntObj();
  }

  @Override
  public BoolObj toBool() {
    return null;
  }

  @Override
  public StrObj toStr() {
    return null;
  }

  @Override
  public ObjectObj toObject() {
    return null;
  }


  @Override
  public UndefObj union(UndefObj rValue) {
    return null;
  }
}
