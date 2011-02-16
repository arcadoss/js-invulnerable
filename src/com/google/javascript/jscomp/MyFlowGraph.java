package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;
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
      this.pseudoRoot = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_PSEUDO_ROOT));
      this.pseudoExit = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_PSEUDO_EXIT));
    }

    public void process(Node externs, Node root) {
      try {
        DiGraphNode<MyNode, Branch> first = null;
        List<Pair> leafs = new ArrayList<Pair>();

        rebuild(root, first, leafs);
        flowGraph.connect(pseudoRoot, Branch.MY_UNCOND, first);
        for (Pair leaf : leafs) {
          flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), pseudoExit);
        }

      } catch (UnimplTransformEx unimplTransformEx) {
        unimplTransformEx.printStackTrace();
      }
    }

    public TempValue rebuild(Node entry,
                             DiGraphNode<MyNode, Branch> root,
                             List<Pair> leafs) throws UnimplTransformEx {
      switch (entry.getType()) {
        case Token.IF:
          return handleIf(entry, root, leafs);
        case Token.WHILE:
          return handleWhile(entry, root, leafs);
        case Token.DO:
          return handleDo(entry, root, leafs);

        default:
          throw new UnimplTransformEx(entry);
      }

    }

    private TempValue handleDo(Node entry, DiGraphNode<MyNode, Branch> first, List<Pair> leafs) throws UnimplTransformEx {
      Node bodyBlock = entry.getFirstChild();
      Node condBlock = bodyBlock.getNext();

      List<Pair> bodyLeafs = new ArrayList<Pair>();
      rebuild(bodyBlock, first, bodyLeafs);

      DiGraphNode<MyNode,Branch> condFirst = null;
      List<Pair> condLeafs = new ArrayList<Pair>();
      TempValue val = rebuild(condBlock, condFirst, condLeafs);

      DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
      for (Pair leaf : bodyLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), condFirst);
      }
      for (Pair leaf : condLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), ifNode);
      }
      flowGraph.connect(ifNode, Branch.MY_TRUE, first);
      leafs.add(new Pair(ifNode, Branch.MY_FALSE));

      return null;
    }

    private TempValue handleWhile(Node entry, DiGraphNode<MyNode, Branch> first, List<Pair> leafs) throws UnimplTransformEx {
      Node condBlock = entry.getFirstChild();
      Node bodyBlock = condBlock.getNext();
      List<Pair> condLeafs = new ArrayList<Pair>();


      TempValue val = rebuild(condBlock, first, condLeafs);
      DiGraphNode<MyNode, Branch> whileNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
      for (Pair leaf : condLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), whileNode);
      }

      List<Pair> bodyLeafs = new ArrayList<Pair>();
      DiGraphNode<MyNode,Branch> bodyNode = null;
      rebuild(bodyBlock, bodyNode, bodyLeafs);

      for (Pair leaf : bodyLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), first);
      }

      flowGraph.connect(whileNode, Branch.MY_TRUE, bodyNode);
      leafs.add(new Pair(whileNode, Branch.MY_FALSE));

      return null;
    }

    private TempValue handleIf(Node entry,
                               DiGraphNode<MyNode, Branch> first,
                               List<Pair> leafs) throws UnimplTransformEx {
      Node condition = entry.getFirstChild();
      Node thenBlock = condition.getNext();
      Node elseBlock = thenBlock.getNext();

      DiGraphNode<MyNode, Branch> thenNode = null;
      DiGraphNode<MyNode, Branch> elseNode = null;
      List<Pair> condLeafs = new ArrayList<Pair>();

      TempValue val = rebuild(condition, first, condLeafs);
      DiGraphNode<MyNode, Branch> ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
      for (Pair leaf : condLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), ifNode);
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

  private class Pair {
    DiGraphNode<MyNode, Branch> node;
    Branch annotaion;

    private Pair(DiGraphNode<MyNode, Branch> node) {
      this.node = node;
      this.annotaion = Branch.MY_UNCOND;
    }

    private Pair(DiGraphNode<MyNode, Branch> node, Branch annotaion) {
      this.node = node;
      this.annotaion = annotaion;
    }

    public DiGraphNode<MyNode, Branch> getNode() {
      return node;
    }

    public Branch getAnnotaion() {
      return annotaion;
    }
  }

  public class TempValue {
  }
}