package com.google.javascript.jscomp;

/**
 * Class represent the following lattice
 * <p/>
 * TOP
 * /   \
 * TRUE FALSE
 * \   /
 * BOTTOM
 *
 * @author arcadoss
 */
public class BoolObj implements BaseObj<BoolObj> {
  public static final int
      BOTTOM = Integer.parseInt("00", 2),
      TRUE = Integer.parseInt("10", 2),
      FALSE = Integer.parseInt("01", 2),
      TOP = Integer.parseInt("11", 2);

  private int value;

  public BoolObj() {
    this.value = BOTTOM;
  }

  public BoolObj(int i) {
    this.value = i;
  }

  public boolean comparableWith(BoolObj rValue) {
    return (this.value == 0 || rValue.getValue() == 0 || ((this.value & rValue.getValue()) != 0));
  }

  @Override
  public BoolObj union(BoolObj rValue) {
    int newValue = this.value | rValue.getValue();
    return new BoolObj(newValue);
  }

  public int getValue() {
    return value;
  }
}
