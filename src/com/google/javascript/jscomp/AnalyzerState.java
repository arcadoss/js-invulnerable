package com.google.javascript.jscomp;

import java.util.*;

/**
 * @author arcadoss
 */
class AnalyzerState implements LatticeElement {
  Store store;
  Stack stack;

  /**
   * @author arcadoss
   */
  public static class Store {
    Map<Label, AbsObject> store;
  }

  /**
   * @author arcadoss
   */
  public static class Stack {
    Map<String, Value> tempValues;
    ExecutionContext context;
    Set<Label> reachable;
  }

  /**
   * @author arcadoss
   */
  public static class Label {
    int label;
  }

  /**
   * @author arcadoss
   */
  public static class Value {
    //    todo: add unknown here ?
    BaseObj undefVal;
    BaseObj nullVal;
    BaseObj boolVal;
    BaseObj numVal;
    BaseObj strVal;
    BaseObj objVal;

    Collection<BaseObj> values;

    public Value() {
      undefVal = new SimpleObj();
      nullVal = new SimpleObj();
      boolVal = new BoolObj();
      numVal = new IntObj();
      strVal = new StrObj();
      objVal = new ObjectObj();

      values = new ArrayList<BaseObj>();
      values.add(undefVal);
      values.add(nullVal);
      values.add(boolVal);
      values.add(numVal);
      values.add(strVal);
      values.add(objVal);
    }
  }

  /**
   * @author arcadoss
   */
  public static class ExecutionContext {
    LinkedList<Label> scopeChain;
    Label thisObj;
    Label varObj;
  }

  /**
   * @author arcadoss
   */
  public static class AbsObject {
    Map<String, Property> properties;
    Set<LinkedList<Label>> scopeChains;
  }

  /**
   * @author arcadoss
   */
  public static class Property {
    Value value;
    BaseObj absent;
    BaseObj readOnly;
    BaseObj dontDelete;
    BaseObj dontEnum;
    BaseObj modified;

    public Property() {
      value = new Value();
      absent = new SimpleObj();
      modified = new SimpleObj();
      readOnly = new BoolObj();
      dontDelete = new BoolObj();
      dontEnum = new BoolObj();
    }
  }

//  todo: implement BaseObj classes
//  todo: method for creating new lattice, with default global variables and methods
//  todo: create 'abstract memory' for storing object entities
//  todo: interprocedural analysis
//  todo: context sensetivity
//  todo: recency abstraction
//  todo: widening, narrowing and other cool stuff
}
