package com.google.javascript.jscomp;

import java.util.HashSet;
import java.util.Set;

/**
 * @author arcadoss
 */
public class StrObj implements ConvertableObj<StrObj> {
  private static final int MaxStrCount = 1000;
  Set<String> value;

  public StrObj() {
    this.value = new HashSet<String>();
  }


  public StrObj(Set<String> value) {
    this.value = value;
  }

  public StrObj(String value) {
    this.value = new HashSet<String>();
    this.value.add(value);
  }

  @Override
  public StrObj union(StrObj rValue) {
    Set<String> newValue = new HashSet<String>(value);
    newValue.addAll(rValue.getValue());
    if (value.size() > MaxStrCount)
      throw new IllegalStateException("Too many string values collected");

    return new StrObj(newValue);
  }

  public Set<String> getValue() {
    return value;
  }

  @Override
  public IntObj toInt() {
    // todo: implement correct transformation
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
}
