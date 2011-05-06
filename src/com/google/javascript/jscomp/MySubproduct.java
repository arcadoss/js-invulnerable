package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * @author arcadoss
 *         Date: 17.02.11
 *         Time: 15:13
 *         <p/>
 *         The aim is to create flow graph using reduced set of nodes. This achieved with recursieve AST traversal.
 *         Objects of this class contains node visist results.
 */
public class MySubproduct {
  private static int tempVarCounter = 0;
  private static final String tempVarBase = "tmp_";

  /**
   * Reference to the currently creating flow graph
   */
  private static MyFlowGraph graph;

  MyValuable nodeRes;

  /**
   * Root of the created subgraph.
   */
  DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first;

  /**
   * Branches that sould be added to the next subgraph.
   */
  List<Pair> leafs;

  private MySubproduct() {
    this.nodeRes = null;
    this.first = null;
    this.leafs = new ArrayList<Pair>();
  }

  public MyValuable getNodeRes() {
    return nodeRes;
  }

  public void setNodeRes(MyValuable nodeRes) {
    this.nodeRes = nodeRes;
  }

  /**
   * This class represents name of temprorary variable, which contains
   * value of program's vairable.
   */
  protected static class VarValue extends MyValuable {
    String value;

    private VarValue(String value) {
      this.value = value;
      this.type = MyValuable.Type.STR;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * This class represents program's variable name that could be obtained from NAME AST node.
   */
  protected static class VarName extends MyValuable {
    String value;

    @Override
    public boolean isVarName() {
      return true;
    }

    private VarName(String value) {
      this.value = value;
      this.type = MyValuable.Type.STR;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * This class represents constant string that could be obtained from STRING AST node
   */
  protected static class StrConst extends MyValuable {
    String value;

    public StrConst(String value) {
      this.value = value;
      this.type = MyValuable.Type.STR;
    }

    public String getValue() {
      return value;
    }
  }

  /**
   * This class represent constant number that could be obtained from NUMBER AST node
   */
  protected static class NmbConst extends MyValuable {
    double value;

    public NmbConst(double value) {
      this.value = value;
      this.type = MyValuable.Type.NMB;
    }

    public Double getValue() {
      return value;
    }
  }

  protected static class NodeBuffer extends MyValuable {
    public NodeBuffer() {
      this.type = MyValuable.Type.NONE;
    }

    @Override
    public String toString() {
      return "none";
    }

    public Object getValue() {
      throw new IllegalStateException("Class node buffer shouldn't return any values");
    }
  }

  public static MySubproduct copyVal(MySubproduct from) {
    MySubproduct out = new MySubproduct();
    out.setNodeRes(from.getNodeRes());
    return out;
  }

  public static MySubproduct newNumber(double value) {
    MySubproduct out = new MySubproduct();
    out.setNodeRes(new NmbConst(value));
    return out;
  }

  public static MySubproduct newString(String value) {
    MySubproduct out = new MySubproduct();
    out.setNodeRes(new StrConst(value));
    return out;
  }

  public static MySubproduct newTemp() {
    tempVarCounter += 1;
    String newName = tempVarBase + tempVarCounter;
    MySubproduct out = new MySubproduct();
    out.setNodeRes(new VarValue(newName));
    return out;
  }

  public static MySubproduct newVarName(String name) {
    MySubproduct out = new MySubproduct();
    out.setNodeRes(new VarName(name));
    return out;
  }

  public static MySubproduct newBuffer() {
    MySubproduct out = new MySubproduct();
    out.setNodeRes(new NodeBuffer());
    return out;
  }

  public static MySubproduct newNan() {
    MySubproduct out = MySubproduct.newBuffer();

    DiGraph.DiGraphNode emptyNode = graph.createDirectedGraphNode(new MyNode(MyNode.Type.SKIP));
    out.setFirst(emptyNode);
    out.addLeaf(emptyNode);
    return out;
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


  public static void setGraph(MyFlowGraph graph) {
    MySubproduct.graph = graph;
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
    this.leafs.addAll(leafs);
  }
}
