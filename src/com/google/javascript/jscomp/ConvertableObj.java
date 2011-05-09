package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public interface ConvertableObj<A extends ConvertableObj> extends BaseObj<A> {
  IntObj toInt();
  BoolObj toBool();
  StrObj toStr();
  ObjectObj toObject();
}

