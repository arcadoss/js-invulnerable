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

  private IntObj(double left, double right) {
    this.left = left;
    this.right = right;
  }

  @Override
  public IntObj union(IntObj rValue) {
    double newLeft = Math.min(this.left, rValue.getLeft());
    double newRight = Math.max(this.right, rValue.getRight());
    return new IntObj(newLeft, newRight);
  }

  public double getLeft() {
    return left;
  }

  public double getRight() {
    return right;
  }
}
