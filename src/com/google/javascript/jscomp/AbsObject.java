package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;

import java.util.*;

/**
 * @author arcadoss
 */
public class AbsObject implements BaseObj<AbsObject> {
  Map<String, AnalyzerState.Property> properties;
  Set<LinkedList<AnalyzerState.Label>> scopeChains;
  boolean isFunction;
  boolean isInternal;
  DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> functionEntry;
  private static final String Prototype = "prototype";

  public AbsObject() {
    this.properties = new HashMap<String, AnalyzerState.Property>();
    this.scopeChains = new HashSet<LinkedList<AnalyzerState.Label>>();
    this.isFunction = false;
    this.functionEntry = null;
    this.isInternal = false;
  }

  public AbsObject(Map<String, AnalyzerState.Property> properties, Set<LinkedList<AnalyzerState.Label>> scopeChains) {
    this.properties = properties;
    this.scopeChains = scopeChains;
  }

  @Override
  public AbsObject union(AbsObject rValue) {
    Map<String, AnalyzerState.Property> newProp = AnalyzerState.joinMaps(properties, rValue.getProperties());
    Set<LinkedList<AnalyzerState.Label>> newChains = AnalyzerState.joinSets(scopeChains, rValue.getScopeChains());

    return new AbsObject(newProp, newChains);
  }

  public Map<String, AnalyzerState.Property> getProperties() {
    return properties;
  }

  public Set<LinkedList<AnalyzerState.Label>> getScopeChains() {
    return scopeChains;
  }

  public void put(String name, AnalyzerState.Property value) {
    properties.put(name, value);
  }

  public boolean has(String name) {
    return properties.containsKey(name);
  }


  public Value get(String name) {
    return properties.get(name).getValue();
  }

  public Value getProto() {
    return properties.get(Prototype).getValue();
  }

  public AnalyzerState call(AnalyzerState in) throws Exception {
    throw new Exception("Function unimplemented");
  }


}
