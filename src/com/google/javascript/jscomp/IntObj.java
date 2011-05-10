package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public class IntObj implements ConvertableObj<IntObj>, BaseObj<IntObj> {
  private int left;
  private int right;
  private boolean bottom;
  public static final int MinInt = -32768;
  public static final int MaxInt = 32767;


  public IntObj() {
    this.bottom = true;
  }

  public IntObj(double value) {
    this.left = (int) Math.floor(value);
    this.right = (int) Math.ceil(value);
    this.bottom = false;
  }

  private IntObj(double left, double right) {
    this.left = (int) Math.floor(left);
    this.right = (int) Math.ceil(right);
    this.bottom = false;
    bound();
  }

  public IntObj(IntObj value) {
    this.bottom = value.isBottom();
    this.left = value.getLeft();
    this.right = value.getRight();
  }

  @Override
  public IntObj union(IntObj rValue) {
    if (bottom) {
      return new IntObj(rValue);
    }

    if (rValue.isBottom()) {
      return new IntObj(this);
    }

    int newLeft = Math.min(this.left, rValue.getLeft());
    int newRight = Math.max(this.right, rValue.getRight());
    return new IntObj(newLeft, newRight);
  }

  public int getLeft() {
    return left;
  }

  public int getRight() {
    return right;
  }

  public boolean isBottom() {
    return bottom;
  }

  @Override
  public IntObj toInt() {
    return this;
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

  @Override
  public String toString() {
    if (bottom) {
      return "_";
    }

    StringBuilder builder = new StringBuilder();
    builder.append("[").append(left).append(",").append(right).append("]");
    return builder.toString();
  }


  public static IntObj add(IntObj lVal, IntObj rVal) {
    if (lVal.isBottom()) {
      return new IntObj(rVal);
    }

    if (rVal.isBottom()) {
      return new IntObj(lVal);
    }

    return new IntObj(lVal.getLeft() + rVal.getLeft(), lVal.getRight() + rVal.getRight());
  }

  public static IntObj mul(IntObj lVal, IntObj rVal) {
    if (lVal.isBottom()) {
      return new IntObj(rVal);
    }

    if (rVal.isBottom()) {
      return new IntObj(lVal);
    }

    int a = lVal.getLeft() * rVal.getLeft();
    int b = lVal.getRight() * rVal.getRight();

    return new IntObj(Math.min(a, b), Math.max(a, b));
  }

  public static IntObj div(IntObj lVal, IntObj rVal) throws UnimplEx {
    if (lVal.isBottom()) {
      return new IntObj(rVal);
    }

    if (rVal.isBottom()) {
      return new IntObj(lVal);
    }

//    if (rVal.contains(0))
    // todo : implement it right !!
    int a = lVal.getLeft() / rVal.getLeft();
    int b = lVal.getRight() / rVal.getRight();
    throw new UnimplEx("You haven't time. Fix me.");

//    return new IntObj(Math.min(a, b), Math.max(a, b));
  }

  public static IntObj sub(IntObj lVal, IntObj rVal) {
    if (lVal.isBottom()) {
      return new IntObj(rVal);
    }

    if (rVal.isBottom()) {
      return new IntObj(lVal);
    }

    return new IntObj(lVal.getLeft() - rVal.getLeft(), lVal.getRight() - rVal.getRight());
  }

  public static IntObj mod(IntObj lVal, IntObj rVal) throws UnimplEx {
    throw new UnimplEx("You haven't time. Fix me.");
  }

  public boolean contains(int i) {
    return (left <= i) && (i <= right);
  }

  private void bound() {
    if (this.left < MinInt) {
      this.left = MinInt;
    }
    if (this.right > MaxInt) {
      this.right = MaxInt;
    }
  }

  public static IntObj neg(IntObj intObj) {
    return new IntObj(-intObj.getRight(), -intObj.getLeft());
  }
}
