package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public class IntObj implements BaseObj<IntObj> {
  private double left;
  private double right;

  public IntObj() {
    this.left = 0;
    this.right = 0;
  }

  public IntObj(double value) {
    this.left = value;
    this.right = value;
  }

  @Override
  public IntObj union(IntObj rValue) {
    this.left = Math.min(this.left, rValue.getLeft());
    this.right = Math.max(this.right, rValue.getRight());
    return this;
  }

  public double getLeft() {
    return left;
  }

  public double getRight() {
    return right;
  }
}
