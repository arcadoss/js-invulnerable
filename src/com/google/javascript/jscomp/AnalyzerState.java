package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;

import java.util.*;

/**
 * @author arcadoss
 */
class AnalyzerState implements LatticeElement, BaseObj<AnalyzerState> {
  Store store;
  Stack stack;
  Marker marker;

  AnalyzerState(Store store, Stack stack, Marker marker) {
    this.store = store;
    this.stack = stack;
    this.marker = marker;
  }

  /**
   * @author arcadoss
   */
  public static class Store implements BaseObj<Store> {
    Map<Label, AbsObject> store;

    public Store() {
      store = new HashMap<Label, AbsObject>();
    }

    private Store(Map<Label, AbsObject> store) {
      this.store = store;
    }

    @Override
    public Store union(Store rValue) {
      Map<Label, AbsObject> newStore = joinMaps(store, rValue.getStore());
      return new Store(newStore);
    }

    public Map<Label, AbsObject> getStore() {
      return store;
    }
  }

  /**
   * @author arcadoss
   */
  public static class Stack implements BaseObj<Stack> {
    Map<String, Value> tempValues;
    Set<ExecutionContext> context;

    final String varForFunc = "v_call";
    final String varForExept = "v_ex";
    Value funcRes;
    Value exeptRes;

    public Stack() {
      tempValues = new HashMap<String, Value>();
      context = new HashSet<ExecutionContext>();

      funcRes = new Value();
      exeptRes = new Value();
      tempValues.put(varForFunc, funcRes);
      tempValues.put(varForExept, exeptRes);
    }

    private Stack(Map<String, Value> tempValues, Set<ExecutionContext> context) {
      this.tempValues = tempValues;
      this.context = context;
    }

    @Override
    public Stack union(Stack rValue) {
      Map<String, Value> newTempVal = joinMaps(tempValues, rValue.getTempValues());
      Set<ExecutionContext> newContext = joinSets(context, rValue.getContext());

      Stack newStack = new Stack(newTempVal, newContext);

      return newStack;
    }

    public Map<String, Value> getTempValues() {
      return tempValues;
    }

    public Set<ExecutionContext> getContext() {
      return context;
    }

    public Value getFuncRes() {
      return funcRes;
    }

    public void setFuncRes(Value funcRes) {
      this.funcRes = funcRes;
    }

    public Value getExeptRes() {
      return exeptRes;
    }

    public void setExeptRes(Value exeptRes) {
      this.exeptRes = exeptRes;
    }
  }

  /**
   * @author arcadoss
   */
  public static class Marker implements BaseObj<Marker> {
    Set<MyFlowGraph.Branch> markers;

    public Marker() {
      this.markers = new HashSet<MyFlowGraph.Branch>();
    }

    public Marker(Set<MyFlowGraph.Branch> set) {
      this.markers = set;
    }

    @Override
    public Marker union(Marker rValue) {
      Set<MyFlowGraph.Branch> set = joinSets(markers, rValue.getMarkers());
      return new Marker(set);
    }

    public Set<MyFlowGraph.Branch> getMarkers() {
      return markers;
    }
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
  public static class Value implements BaseObj<Value> {
    //    todo: add unknown here ?
    static final int typeCount = 6;
    BaseObj undefVal;
    BaseObj nullVal;
    BaseObj boolVal;
    BaseObj numVal;
    BaseObj strVal;
    BaseObj objVal;

    List<BaseObj> values;

    public Value() {
      undefVal = new SimpleObj();
      nullVal = new SimpleObj();
      boolVal = new BoolObj();
      numVal = new IntObj();
      strVal = new StrObj();
      objVal = new ObjectObj();
      values = makeList(undefVal, nullVal, boolVal, numVal, strVal, objVal);
    }

    private Value(BaseObj undefVal, BaseObj nullVal, BaseObj boolVal, BaseObj numVal, BaseObj strVal, BaseObj objVal) {
      this.undefVal = undefVal;
      this.nullVal = nullVal;
      this.boolVal = boolVal;
      this.numVal = numVal;
      this.strVal = strVal;
      this.objVal = objVal;
      this.values = makeList(undefVal, nullVal, boolVal, numVal, strVal, objVal);
    }

    private Value(List<BaseObj> newValues) {
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
      Iterator<BaseObj> iter1 = values.iterator();
      Iterator<BaseObj> iter2 = rValue.getValues().iterator();
      List<BaseObj> newValues = new ArrayList<BaseObj>();

      while (iter1.hasNext()) {
        newValues.add(iter1.next().union(iter2.next()));
      }

      Value out = new Value(newValues);
      return out;
    }

    public List<BaseObj> getValues() {
      return values;
    }

    private static List<BaseObj> makeList(BaseObj undefVal, BaseObj nullVal, BaseObj boolVal, BaseObj numVal, BaseObj strVal, BaseObj objVal) {
      List<BaseObj> out = new ArrayList<BaseObj>();
      out.add(undefVal);
      out.add(nullVal);
      out.add(boolVal);
      out.add(numVal);
      out.add(strVal);
      out.add(objVal);
      return out;
    }
  }

  /**
   * @author arcadoss
   */
  public static class ExecutionContext {
    LinkedList<Label> scopeChain;
    Label thisObj;
    Label varObj;

    //todo : initialize objects here
  }

  /**
   * @author arcadoss
   */
  public static class AbsObject implements BaseObj<AbsObject> {
    Map<String, Property> properties;
    Set<LinkedList<Label>> scopeChains;
    boolean isFunction;
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> functionEntry;

    public AbsObject() {
      this.properties = new HashMap<String, Property>();
      this.scopeChains = new HashSet<LinkedList<Label>>();
      this.isFunction = false;
      this.functionEntry = null;
    }

    private AbsObject(Map<String, Property> properties, Set<LinkedList<Label>> scopeChains) {
      this.properties = properties;
      this.scopeChains = scopeChains;
    }

    @Override
    public AbsObject union(AbsObject rValue) {
      Map<String, Property> newProp = joinMaps(properties, rValue.getProperties());
      Set<LinkedList<Label>> newChains = joinSets(scopeChains, rValue.getScopeChains());

      return new AbsObject(newProp, newChains);
    }

    public Map<String, Property> getProperties() {
      return properties;
    }

    public Set<LinkedList<Label>> getScopeChains() {
      return scopeChains;
    }
  }

  /**
   * @author arcadoss
   */
  public static class Property implements BaseObj<Property> {
    private static final int PropCount = 5;
    Value value;
    BaseObj absent;
    BaseObj readOnly;
    BaseObj dontDelete;
    BaseObj dontEnum;
    BaseObj modified;
    List<BaseObj> properties;

    public Property() {
      value = new Value();
      absent = new SimpleObj();
      readOnly = new BoolObj();
      dontDelete = new BoolObj();
      dontEnum = new BoolObj();
      modified = new SimpleObj();

      properties = makeList(absent, readOnly, dontDelete, dontEnum, modified);
    }

    private Property(Value value, List<BaseObj> properties) {
      if (properties.size() != PropCount) {
        throw new IllegalArgumentException("Trying to initialize Property object with wrong parameters");
      }
      this.value = value;
      this.absent = properties.get(0);
      this.readOnly = properties.get(1);
      this.dontDelete = properties.get(2);
      this.dontEnum = properties.get(3);
      this.modified = properties.get(4);
      this.properties = properties;
    }

    @Override
    public Property union(Property rValue) {
      Value newValue = value.union(rValue.getValue());

      Iterator<BaseObj> iter1 = properties.iterator();
      Iterator<BaseObj> iter2 = rValue.getProperties().iterator();
      List<BaseObj> newProps = new ArrayList<BaseObj>();

      while (iter1.hasNext()) {
        newProps.add(iter1.next().union(iter2.next()));
      }

      return new Property(newValue, newProps);
    }

    public Value getValue() {
      return value;
    }

    public List<BaseObj> getProperties() {
      return properties;
    }

    private static List<BaseObj> makeList(BaseObj absent, BaseObj readOnly, BaseObj dnd, BaseObj dne, BaseObj modified) {
      List<BaseObj> out = new ArrayList<BaseObj>(PropCount);

      out.add(absent);
      out.add(readOnly);
      out.add(dnd);
      out.add(dne);
      out.add(modified);

      return out;
    }
  }

  @Override
  public AnalyzerState union(AnalyzerState rValue) {
    Stack newStack = stack.union(rValue.getStack());
    Store newStore = store.union(rValue.getStore());
    marker = marker.union(rValue.getMarker());

    return new AnalyzerState(newStore, newStack, marker);
  }

  public Store getStore() {
    return store;
  }

  public Stack getStack() {
    return stack;
  }

  public Marker getMarker() {
    return marker;
  }


  public static AnalyzerState createGlobal() {
    Stack initStack = new Stack();
    Marker initMarker = new Marker();
    Store initStore = new Store();

    return new AnalyzerState(initStore, initStack, initMarker);
  }

  public static AnalyzerState bottom() {
    Stack initStack = new Stack();
    Marker initMarker = new Marker();
    Store initStore = new Store();

    return new AnalyzerState(initStore, initStack, initMarker);
  }


  private static <K, V extends BaseObj<V>> Map<K, V> joinMaps(Map<K, V> map1, Map<K, V> map2) {
    Map<K, V> out = new HashMap(map1);

    Set<Map.Entry<K, V>> rMap = map2.entrySet();

    for (Map.Entry<K, V> elem : rMap) {
      if (out.containsKey(elem.getKey())) {
        out.get(elem.getKey()).union(elem.getValue());
      } else {
        out.put(elem.getKey(), elem.getValue());
      }
    }

    return out;
  }

  private static <T> Set<T> joinSets(Set<T> context1, Set<T> context2) {
    Set<T> out = new HashSet<T>(context1);
    out.addAll(context2);
    return out;
  }

}
