package com.google.javascript.jscomp;

/**
* User: arcadoss
* Date: 17.02.11
* Time: 15:13
*/
public class MyAbsValue {
  private static class StrValue extends MyAbsValue {
    String value;

    public StrValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static class NmbValue extends MyAbsValue {
    double value;

    public NmbValue(double value) {
      this.value = value;
    }

    public double getValue() {
      return value;
    }
  }

  public static MyAbsValue newNumber(double value) {
    return new NmbValue(value);
  }

  public static MyAbsValue newString(String value) {
    return new StrValue(value);
  }
}
