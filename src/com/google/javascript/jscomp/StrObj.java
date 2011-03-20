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

  @Override
  public StrObj union(StrObj rValue) {
    this.value.addAll(rValue.getValue());
    if (value.size() > MaxStrCount)
      throw new IllegalStateException("Too many string values collected");
    return this;
  }

  public Set<String> getValue() {
    return value;
  }
}
