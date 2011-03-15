package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arcadoss
 * Date: 17.02.11
 * Time: 15:13
 *
 * The aim is to create flow graph using reduced set of nodes. This achieved with recursieve AST traversal.
 * Objects of this class contains node visist results.
 */
public class MySubproduct {
  private static int tempVarCounter = 0;
  private static final String tempVarBase = "temp ";

  /**
   * Reference to the currently creating flow graph
   */
  private static MyFlowGraph graph;

  /**
   * Root of the created subgraph.
   */
  DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first;

  /**
   * Branches that sould be added to the next subgraph.
   */
  List<Pair> leafs;

  private MySubproduct() {
    first = null;
    leafs = new ArrayList<Pair>();
  }

  public static void setGraph(MyFlowGraph graph) {
    MySubproduct.graph = graph;
  }

  public boolean isVarName() {
    return false;
  }

  public void addLeaf(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node, MyFlowGraph.Branch annotaion) {
    leafs.add(new Pair(node, annotaion));
  }

  public void addLeaf(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node) {
    addLeaf(node, MyFlowGraph.Branch.UNCOND);
  }

  public void connectToFirst(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> preFirst) {
    connectToFirst(preFirst, MyFlowGraph.Branch.UNCOND);
  }

  public void connectToFirst(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> preFirst, MyFlowGraph.Branch cond) {
    graph.connect(preFirst.getValue(), cond, this.first.getValue());
  }

  public void connectLeafsTo(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> postLeafs) {
    for (Pair leaf : leafs) {
      graph.connect(leaf.getNodeValue(), leaf.getAnnotaion(), postLeafs.getValue());
    }
  }

  public void connectLeafsTo(MySubproduct postLeafs) {
    for (Pair leaf : leafs) {
      graph.connect(leaf.getNodeValue(), leaf.getAnnotaion(), postLeafs.getFirst().getValue());
    }
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

  public void addLeaf(List<Pair> leafs) {
    leafs.addAll(leafs);
  }

  /**
   * This class represents name of temprorary variable, which contains
   * value of program's vairable.
   */
  protected static class VarValue extends MySubproduct {
    String value;

    private VarValue(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * This class represents program's variable name that could be obtained from NAME AST node.
   */
  protected static class VarName extends MySubproduct {
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

  /**
   * This class represents constant string that could be obtained from STRING AST node
   */
  protected static class StrConst extends MySubproduct {
    String value;

    public StrConst(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * This class represent constant number that could be obtained from NUMBER AST node
   */
  protected static class NmbConst extends MySubproduct {
    double value;

    public NmbConst(double value) {
      this.value = value;
    }

    public double getValue() {
      return value;
    }
  }

  public static MySubproduct newNumber(double value) {
    return new NmbConst(value);
  }

  public static MySubproduct newString(String value) {
    return new StrConst(value);
  }

  public static MySubproduct newTemp() {
    tempVarCounter += 1;
    String newName = tempVarBase + tempVarCounter;
    return new VarValue(newName);
  }

  public static MySubproduct newVarName(String name) {
    return new VarName(name);
  }

  public static MySubproduct newBuffer() {
    return new MySubproduct();
  }

  public static MySubproduct newNan() {
    MySubproduct out = MySubproduct.newBuffer();

    DiGraph.DiGraphNode emptyNode = graph.createDirectedGraphNode(new MyNode(MyNode.Type.NAN));
    out.setFirst(emptyNode);
    out.addLeaf(emptyNode);
    return  out;
  }

  private class Pair {
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node;
    MyFlowGraph.Branch annotaion;

    private Pair(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node) {
      this.node = node;
      this.annotaion = MyFlowGraph.Branch.UNCOND;
    }

    private Pair(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node, MyFlowGraph.Branch annotaion) {
      this.node = node;
      this.annotaion = annotaion;
    }

    public MyNode getNodeValue() {
      return node.getValue();
    }

    public MyFlowGraph.Branch getAnnotaion() {
      return annotaion;
    }
  }
}
