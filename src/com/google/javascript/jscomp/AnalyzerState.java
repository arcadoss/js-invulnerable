package com.google.javascript.jscomp;

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

  public AnalyzerState(AnalyzerState input) {
    this.store = new Store(input.getStore());
    this.stack = new Stack(input.getStack());
    this.marker = new Marker(input.getMarker());
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

    public Store(Store store) {
      this.store = new HashMap<Label, AbsObject>(store.getStore());
    }

    @Override
    public Store union(Store rValue) {
      Map<Label, AbsObject> newStore = joinMaps(store, rValue.getStore());
      return new Store(newStore);
    }

    public boolean contains(Label label) {
      return this.store.containsKey(label);
    }

    public AbsObject get(Label label) {
      return this.store.get(label);
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

    public Stack(Stack stack) {
      this.tempValues = new HashMap<String, Value>(stack.getTempValues());
      this.context = new HashSet<ExecutionContext>(stack.getContext());
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

    public Value getTemp(String name) {
      if (tempValues.containsKey(name)) {
        return tempValues.get(name);
      }

      // todo: it shold be removed after all operations would be implemented
      return  new Value();
    }

    public void putTemp(String name, Value val) {
      tempValues.put(name, val);
    }
  }

  /**
   * @author arcadoss
   */
  public static class Marker implements BaseObj<Marker> {
    Set<MyFlowGraph.Branch> markers;

    public Marker() {
      this.markers = new HashSet<MyFlowGraph.Branch>();
      this.markers.add(MyFlowGraph.Branch.UNCOND);
    }

    public Marker(Set<MyFlowGraph.Branch> set) {
      this.markers = set;
    }

    public Marker(Marker marker) {
      this.markers = new HashSet<MyFlowGraph.Branch>(marker.getMarkers());
    }

    @Override
    public Marker union(Marker rValue) {
      Set<MyFlowGraph.Branch> set = joinSets(markers, rValue.getMarkers());
      return new Marker(set);
    }

    public Set<MyFlowGraph.Branch> getMarkers() {
      return markers;
    }

    public void toUncond() {
      if (markers.size() != 1 && markers.contains(MyFlowGraph.Branch.UNCOND)) {
        markers.clear();
        markers.add(MyFlowGraph.Branch.UNCOND);
      }
    }
  }

  /**
   * @author arcadoss
   */
  public static class Label {
    int label;

    public Label(int label) {
      this.label = label;
    }

    public int get() {
      return label;
    }

    @Override
    public String toString() {
      return String.valueOf(label);
    }
  }

  /**
   * @author arcadoss
   */
  public static class ExecutionContext {
    LinkedList<Label> scopeChain;
    Label thisObj;
    Label varObj;

    public LinkedList<Label> getScopeChain() {
      return scopeChain;
    }

    public Label getThisObj() {
      return thisObj;
    }

    public Label getVarObj() {
      return varObj;
    }

    //todo : initialize objects here
  }

  /**
   * @author arcadoss
   */
  public static class Property implements BaseObj<Property> {
    private static final int PropCount = 4;
    private Value value;
    private BaseObj absent;
    private BaseObj readOnly;
    private BaseObj dontDelete;
    private BaseObj dontEnum;
    private List<BaseObj> properties;

    public Property() {
      value = new Value();
      absent = new SimpleObj();
      readOnly = new BoolObj();
      dontDelete = new BoolObj();
      dontEnum = new BoolObj();

      properties = makeList(absent, readOnly, dontDelete, dontEnum);
    }

    public Property(Value value) {
      this.value = value;
      absent = new SimpleObj(SimpleObj.BOTTOM);
      readOnly = new BoolObj(BoolObj.FALSE);
      dontDelete = new BoolObj(BoolObj.FALSE);
      dontEnum = new BoolObj(BoolObj.FALSE);

      properties = makeList(absent, readOnly, dontDelete, dontEnum);
    }

    public Property(Value value, List<BaseObj> properties) {
      if (properties.size() != PropCount) {
        throw new IllegalArgumentException("Trying to initialize Property object with wrong parameters");
      }
      this.value = value;
      this.absent = properties.get(0);
      this.readOnly = properties.get(1);
      this.dontDelete = properties.get(2);
      this.dontEnum = properties.get(3);
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

    private static List<BaseObj> makeList(BaseObj absent, BaseObj readOnly, BaseObj dnd, BaseObj dne) {
      List<BaseObj> out = new ArrayList<BaseObj>(PropCount);

      out.add(absent);
      out.add(readOnly);
      out.add(dnd);
      out.add(dne);

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


  public static <K, V extends BaseObj<V>> Map<K, V> joinMaps(Map<K, V> map1, Map<K, V> map2) {
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

  public static <T> Set<T> joinSets(Set<T> context1, Set<T> context2) {
    Set<T> out = new HashSet<T>(context1);
    out.addAll(context2);
    return out;
  }

}
