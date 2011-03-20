package com.google.javascript.jscomp;

import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

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
  }

  /**
   * @author arcadoss
   */
  public static class Value {
    BaseObj undefVal;
    BaseObj nullVal;
    BaseObj boolVal;
    BaseObj numVal;
    BaseObj strVal;
    Set<Label> objVal;
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
    Set<Label> scopeChain;
  }

  /**
   * @author arcadoss
   */
  public static class Property {
    BaseObj value;
    BaseObj absent;
    BaseObj readOnly;
    BaseObj dontDelete;
    BaseObj dontEnum;
    BaseObj modified;
  }

//  todo: method for creating new lattice, with default global variables and methods
//  todo: create 'abstract memory' for storing object entities
//  todo: interprocedural analysis
//  todo: context sensetivity
//  todo: recency abstraction
//  todo: widening, narrowing and other cool stuff
}
