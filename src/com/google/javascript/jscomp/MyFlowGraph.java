package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;
import com.google.javascript.jscomp.graph.GraphNode;
import com.google.javascript.jscomp.graph.LinkedDirectedGraph;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.ArrayList;
import java.util.List;

/**
 * User: arcadoss
 * Date: 14.02.11
 * Time: 20:11
 */
//public class MyFlowGraph extends LinkedDirectedGraph<MyNode, MyFlowGraph.Branch> {
public class MyFlowGraph extends LinkedDirectedGraph {
  public MyFlowGraph() {
    super(false, true);
  }

  protected MyFlowGraph(boolean useNodeAnnotations, boolean useEdgeAnnotations) {
    super(useNodeAnnotations, useEdgeAnnotations);
  }

  public class Creator implements CompilerPass {
    MyFlowGraph flowGraph;
    DiGraphNode<MyNode, Branch> pseudoRoot;
    DiGraphNode<MyNode, Branch> pseudoExit;

    public Creator() {
      this.flowGraph = new MyFlowGraph();
      this.pseudoRoot = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_PSEUDO_ROOT, null));
      this.pseudoExit = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_PSEUDO_EXIT, null));
    }

    public void process(Node externs, Node root) {
      try {
        DiGraphNode<MyNode, Branch> first = null;
        List<DiGraphNode<MyNode, Branch>> leafs = null;

        rebuild(root, first, leafs);
        flowGraph.connect(pseudoRoot, Branch.MY_UNCOND, first);

      } catch (UnimplTransformEx unimplTransformEx) {
        unimplTransformEx.printStackTrace();
      }
    }

    public TempValue rebuild(Node entry,
//                          List<DiGraphEdge<MyNode, Branch>> predcessors,
                             DiGraphNode<MyNode, Branch> first,
                             List<DiGraphNode<MyNode, Branch>> leafs) throws UnimplTransformEx {
      String newTemp = null;

      switch (entry.getType()) {
        case Token.IF:
          return handleIf(entry, first, leafs);
        case Token.WHILE:
          return handleWhile(entry, first, leafs);

        default:
          throw new UnimplTransformEx(entry);
      }

    }

    private TempValue handleWhile(Node entry, DiGraphNode<MyNode, Branch> first, List<DiGraphNode<MyNode, Branch>> leafs) {
      return null;
    }

    private TempValue handleIf(Node entry, DiGraphNode<MyNode, Branch> first, List<DiGraphNode<MyNode, Branch>> leafs) throws UnimplTransformEx {
      Node condition = entry.getFirstChild();
      Node thenBlock = condition.getNext();
      Node elseBlock = thenBlock.getNext();

      DiGraphNode<MyNode, Branch> thenNode = null;
      DiGraphNode<MyNode, Branch> elseNode = null;
      List<DiGraphNode<MyNode, Branch>> condLeafs = new ArrayList<DiGraphNode<MyNode, Branch>>();
      List<TempValue> operands = new ArrayList<TempValue>();

      TempValue val = rebuild(condition, first, condLeafs);
      operands.add(val);
      GraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, operands));
      for (GraphNode leaf : leafs) {
        flowGraph.connect(leaf, Branch.MY_UNCOND, ifNode);
      }

      rebuild(thenBlock, thenNode, leafs);
      flowGraph.connect(ifNode, Branch.MY_TRUE, thenNode);

      if (elseBlock != null) {
        rebuild(elseBlock, elseNode, leafs);
        flowGraph.connect(ifNode, Branch.MY_FALSE, elseNode);
      }

      return null;
    }
  }

  public class UnimplTransformEx extends Exception {
    public UnimplTransformEx(Node node) {
      super("Transformation of " + node.toString() + " is unimplemented");
    }
  }

  public static enum Branch {
    MY_UNCOND,
    MY_TRUE,
    MY_FALSE,
    MY_EXEPT,
    MY_CALL,
    MY_RETURN;
  }


  public class TempValue {
  }
}