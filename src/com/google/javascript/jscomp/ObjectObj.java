package com.google.javascript.jscomp;

import java.util.HashSet;
import java.util.Set;

import com.google.javascript.jscomp.AnalyzerState.Label;

/**
 * @author arcadoss
 */
public class ObjectObj implements BaseObj<ObjectObj> {
  private static final int MaxObjCount = 1000;
  Set<Label> value;

  public ObjectObj() {
    this.value = new HashSet<Label>();
  }

  @Override
  public ObjectObj union(ObjectObj rValue) {
    this.value.addAll(rValue.getValue());
    if (value.size() > MaxObjCount)
      throw new IllegalStateException("Label reffers on too many objects");
    return this;
  }

  public Set<Label> getValue() {
    return value;
  }
}
