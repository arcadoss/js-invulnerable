package com.google.javascript.jscomp;

import java.util.HashSet;
import java.util.Set;

import com.google.javascript.jscomp.AnalyzerState.Label;

/**
 * @author arcadoss
 */
public class ObjectObj implements ConvertableObj<ObjectObj>, BaseObj<ObjectObj> {
  private static final int MaxObjCount = 1000;
  Set<Label> value;

  public ObjectObj() {
    this.value = new HashSet<Label>();
  }

  public ObjectObj(Set<Label> value) {
    this.value = value;
  }

  @Override
  public ObjectObj union(ObjectObj rValue) {
    Set<Label> newValue = new HashSet<Label>(value);
    newValue.addAll(rValue.getValue());
    if (newValue.size() > MaxObjCount)
      throw new IllegalStateException("Label reffers on too many objects");

    return new ObjectObj(newValue);
  }

  public Set<Label> getValue() {
    return value;
  }

  @Override
  public IntObj toInt() {
    // todo : implement correct transformation
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
