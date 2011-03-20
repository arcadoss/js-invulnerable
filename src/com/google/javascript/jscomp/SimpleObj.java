package com.google.javascript.jscomp;

/**
 * Class represent the following lattice
 * <p/>
 * TOP
 * |
 * BOTTOM
 *
 * @author arcadoss
 */
public class SimpleObj implements BaseObj<SimpleObj> {
  private static final int
      BOTTOM = Integer.parseInt("0", 2),
      TOP = Integer.parseInt("1", 2);

  private int value;

  public SimpleObj() {
    this.value = BOTTOM;
  }

  @Override
  public SimpleObj union(SimpleObj rValue) {
    this.value = this.value | rValue.getValue();
    return this;
  }

  public int getValue() {
    return value;
  }
}
