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

  private static class IntValue extends MyAbsValue {
    int value;

    public IntValue(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
  }

  public static MyAbsValue newNumber(int value) {
    return new IntValue(value);
  }

  public static MyAbsValue newString(String value) {
    return new StrValue(value);
  }
}
