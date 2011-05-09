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
public class SimpleObj<A extends SimpleObj> implements BaseObj<A> {
  public static final int
      BOTTOM = Integer.parseInt("0", 2),
      TOP = Integer.parseInt("1", 2);

  private int value;

  public SimpleObj() {
    this.value = BOTTOM;
  }

  public SimpleObj(int value) {
    this.value = value;
  }

  public int getValue() {
    return value;
  }

  @Override
  public A union(A rValue) {
    SimpleObj out = new SimpleObj((this.value == TOP || rValue.getValue() == TOP) ? TOP : BOTTOM);
    return (A) out;
  }
}

