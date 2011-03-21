package com.google.javascript.jscomp;

import java.util.HashSet;
import java.util.Set;

/**
 * @author arcadoss
 */
public class StrObj implements BaseObj<StrObj> {
  private static final int MaxStrCount = 1000;
  Set<String> value;

  public StrObj() {
    this.value = new HashSet<String>();
  }

  private StrObj(Set<String> value) {
    this.value = value;
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
}
