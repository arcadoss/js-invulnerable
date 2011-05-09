package com.google.javascript.jscomp;

import java.util.*;

/**
 * @author arcadoss
 */
public class Value implements BaseObj<Value> {
  //    todo: add unknown here ?
  static final int typeCount = 6;
  ConvertableObj undefVal;
  ConvertableObj nullVal;
  ConvertableObj boolVal;
  ConvertableObj numVal;
  ConvertableObj strVal;
  ConvertableObj objVal;

  List<ConvertableObj> values;

  public Value() {
    undefVal = new UndefObj();
    nullVal = new NullObj();
    boolVal = new BoolObj();
    numVal = new IntObj();
    strVal = new StrObj();
    objVal = new ObjectObj();
    values = makeList(undefVal, nullVal, boolVal, numVal, strVal, objVal);
  }

  public Value(ConvertableObj undefVal, ConvertableObj nullVal, ConvertableObj boolVal, ConvertableObj numVal, ConvertableObj strVal, ConvertableObj objVal) {
    this.undefVal = undefVal;
    this.nullVal = this.nullVal;
    this.boolVal = boolVal;
    this.numVal = numVal;
    this.strVal = strVal;
    this.objVal = objVal;
    this.values = makeList(undefVal, this.nullVal, boolVal, numVal, strVal, objVal);
  }

  public Value(List<ConvertableObj> newValues) {
    if (newValues.size() != typeCount) {
      throw new IllegalArgumentException("Trying initialize Value type with wrong arguments");
    }

    undefVal = newValues.get(0);
    nullVal = newValues.get(1);
    boolVal = newValues.get(2);
    numVal = newValues.get(3);
    strVal = newValues.get(4);
    objVal = newValues.get(5);
    values = newValues;
  }

  @Override
  public Value union(Value rValue) {
    Iterator<ConvertableObj> iter1 = values.iterator();
    Iterator<ConvertableObj> iter2 = rValue.getValues().iterator();
    List<ConvertableObj> newValues = new ArrayList<ConvertableObj>();

    while (iter1.hasNext()) {
      ConvertableObj val1 = iter1.next();
      ConvertableObj val2 = iter2.next();
      ConvertableObj val = (ConvertableObj) val1.union(val2);

      newValues.add(val);
    }

    Value out = new Value(newValues);
    return out;
  }

  public List<ConvertableObj> getValues() {
    return values;
  }

  public BaseObj getUndefVal() {
    return undefVal;
  }

  public BaseObj getNullVal() {
    return nullVal;
  }

  public BaseObj getBoolVal() {
    return boolVal;
  }

  public BaseObj getNumVal() {
    return numVal;
  }

  public BaseObj getStrVal() {
    return strVal;
  }

  public BaseObj getObjVal() {
    return objVal;
  }

  private static List<ConvertableObj> makeList(ConvertableObj undefVal, ConvertableObj nullVal, ConvertableObj boolVal, ConvertableObj numVal, ConvertableObj strVal, ConvertableObj objVal) {
    List<ConvertableObj> out = new ArrayList<ConvertableObj>();
    out.add(undefVal);
    out.add(nullVal);
    out.add(boolVal);
    out.add(numVal);
    out.add(strVal);
    out.add(objVal);
    return out;
  }

  public static Value makeObj(AnalyzerState.Label l) {
    Set<AnalyzerState.Label> set = new HashSet<AnalyzerState.Label>();
    set.add(l);
    ConvertableObj undefVal = new UndefObj();
    ConvertableObj nullVal = new NullObj();
    ConvertableObj boolVal = new BoolObj();
    ConvertableObj numVal = new IntObj();
    ConvertableObj strVal = new StrObj();
    ConvertableObj objVal = new ObjectObj(set);

    Value out = new Value(undefVal, nullVal, boolVal, numVal, strVal, objVal);
    return out;
  }

  public static Value makeUndef() {
    ConvertableObj undefVal = new UndefObj(SimpleObj.TOP);
    ConvertableObj nullVal = new NullObj();
    ConvertableObj boolVal = new BoolObj();
    ConvertableObj numVal = new IntObj();
    ConvertableObj strVal = new StrObj();
    ConvertableObj objVal = new ObjectObj();

    Value out = new Value(undefVal, nullVal, boolVal, numVal, strVal, objVal);
    return out;
  }

  public static Value add(Value lVal, Value rVal) {
    IntObj lInt = new IntObj();
    IntObj rInt = new IntObj();

    for (ConvertableObj obj : lVal.getValues()) {
      lInt.union(obj.toInt());
    }

    for (ConvertableObj obj : rVal.getValues()) {
      rInt.union(obj.toInt());
    }

    return Value.makeInt(IntObj.add(lInt, rInt));
  }

  private static Value makeInt(IntObj intVal) {
    ConvertableObj undefVal = new UndefObj();
    ConvertableObj nullVal = new NullObj();
    ConvertableObj boolVal = new BoolObj();
    ConvertableObj strVal = new StrObj();
    ConvertableObj objVal = new ObjectObj();

    Value out = new Value(undefVal, nullVal, boolVal, intVal, strVal, objVal);
    return out;
  }
}
