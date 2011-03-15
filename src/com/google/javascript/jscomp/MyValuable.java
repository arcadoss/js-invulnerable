package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public abstract class MyValuable {
  Type type;

  public abstract Object getValue();

  public Type getType() {
    return type;
  }

  public void setType(Type type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return getValue().toString();
  }

  public static enum Type {
    STR, NMB, NONE;

  }
}
