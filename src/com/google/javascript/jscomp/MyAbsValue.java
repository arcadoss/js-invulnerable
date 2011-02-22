package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * User: arcadoss
 * Date: 17.02.11
 * Time: 15:13
 */
public class MyAbsValue {
  private static int tempVarCounter = 0;
  private static final String tempVarBase = "temp ";

  DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first;
  List<Pair> leafs;


  private MyAbsValue() {
    first = null;
    leafs = new ArrayList<Pair>();
  }

  public boolean isVarName() {
    return false;
  }

  public void addLeaf(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node, MyFlowGraph.Branch annotaion) {
    leafs.add(new Pair(node, annotaion));
  }
  public void addLeaf(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node) {
    addLeaf(node, MyFlowGraph.Branch.MY_UNCOND);
  }

  public DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> getFirst() {
    return first;
  }

  public void setFirst(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first) {
    this.first = first;
  }

  public List<Pair> getLeafs() {
    return leafs;
  }



  private static class VarName extends MyAbsValue {
    String value;

    @Override
    public boolean isVarName() {
      return true;
    }

    private VarName(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  private static class VarValue extends MyAbsValue {
    String value;

    private VarValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

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

  public static MyAbsValue newTemp() {
    tempVarCounter += 1;
    String newName = tempVarBase + tempVarCounter;
    return new VarValue(newName);
  }

  public static MyAbsValue newVarName(String name) {
    return new VarName(name);
  }

  private class Pair {
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node;
    MyFlowGraph.Branch annotaion;

    private Pair(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node) {
      this.node = node;
      this.annotaion = MyFlowGraph.Branch.MY_UNCOND;
    }

    private Pair(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node, MyFlowGraph.Branch annotaion) {
      this.node = node;
      this.annotaion = annotaion;
    }

    public DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> getNode() {
      return node;
    }

    public MyFlowGraph.Branch getAnnotaion() {
      return annotaion;
    }
  }
}
