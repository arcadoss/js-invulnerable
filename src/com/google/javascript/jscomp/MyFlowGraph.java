package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;
import com.google.javascript.jscomp.graph.LinkedDirectedGraph;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.*;

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
    List<DiGraphNode<MyNode, Branch>> loopsRoots = new ArrayList<DiGraphNode<MyNode, Branch>>();
    List<DiGraphNode<MyNode, Branch>> loopsBreaks = new LinkedList<DiGraphNode<MyNode, Branch>>();
    Map labels = new HashMap<String, DiGraphNode<MyNode, Branch>>();

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

    public TempValue rebuild(Node entry, DiGraphNode<MyNode, Branch> root, List<Pair> leafs) throws UnimplTransformEx {
      switch (entry.getType()) {
        case Token.IF:
          return handleIf(entry, root, leafs);
        case Token.WHILE:
          return handleWhile(entry, root, leafs);
        case Token.DO:
          return handleDo(entry, root, leafs);
        case Token.SWITCH:
          return handleSwitch(entry, root, leafs);
        case Token.BLOCK:
          return handleBlock(entry, root, leafs);


        default:
          throw new UnimplTransformEx(entry);
      }

    }

    private TempValue handleBlock(Node entry, DiGraphNode<MyNode, Branch> first, List<Pair> leafs) throws UnimplTransformEx {
      List<Pair> prevLeafs = new ArrayList<Pair>();
      DiGraphNode<MyNode,Branch> currFirst = first;

      for (Node child : entry.children()) {
        List<Pair> currLeafs = new ArrayList<Pair>();
        rebuild(child, currFirst, currLeafs);

        for (Pair prev : prevLeafs) {
          flowGraph.connect(prev.getNode(), prev.getAnnotaion(), currFirst);
        }
        prevLeafs = currLeafs;
        // TODO: is it right?
        currFirst = null;
      }
      leafs.addAll(prevLeafs);
      return null;
    }

    private TempValue handleSwitch(Node entry, DiGraphNode<MyNode, Branch> first, List<Pair> leafs) throws UnimplTransformEx {
      Node expBranch = entry.getFirstChild();
      Node defaultBranch = null;

      List<Pair> expLeafs = new ArrayList<Pair>();
      TempValue val = rebuild(expBranch, first, expLeafs);

      Node child = entry.getNext();
      List<Pair> toNextCase = new ArrayList<Pair>();

      if (child.getType() == Token.CASE) {
        Node condBranch = child.getFirstChild();
        Node blockBranch = condBranch.getNext();

        List<Pair> condLeafs = new ArrayList<Pair>();
        DiGraphNode<MyNode, Branch> condNode = null;
        TempValue caseVal = rebuild(condBranch, condNode, condLeafs);

        for (Pair leaf : expLeafs) {
          flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), condNode);
        }

        DiGraphNode caseNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, caseVal));

        DiGraphNode<MyNode, Branch> blockNode = null;
        List<Pair> blockLeafs = new ArrayList<Pair>();
        rebuild(blockBranch, blockNode, blockLeafs);

        flowGraph.connect(caseNode, Branch.MY_TRUE, blockNode);
        toNextCase.add(new Pair(caseNode, Branch.MY_FALSE));

        if (loopsBreaks.size() == 0) {
          toNextCase.addAll(blockLeafs);
        } else {
          leafs.addAll(blockLeafs);
          // FIXME: nested switch will fail here
          loopsBreaks.clear();
        }

      } else {
        Node blockBrach = child.getFirstChild();
        DiGraphNode<MyNode, Branch> blockNode = null;
        List<Pair> blockLeafs = new ArrayList<Pair>();

        rebuild(blockBrach, blockNode, blockLeafs);
        for (Pair leaf : expLeafs) {
          flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), blockNode);
        }

        if (loopsBreaks.size() == 0) {
          toNextCase.addAll(blockLeafs);
        } else {
          leafs.addAll(blockLeafs);
          // FIXME: nested switch will fail here
          loopsBreaks.clear();
        }
      }


      while ((child = entry.getNext()) != null) {
        if (child.getType() == Token.CASE) {
          Node condBranch = child.getFirstChild();
          Node blockBranch = condBranch.getNext();

          List<Pair> condLeafs = new ArrayList<Pair>();
          DiGraphNode<MyNode, Branch> condNode = null;
          TempValue caseVal = rebuild(condBranch, condNode, condLeafs);

          for (Pair leaf : toNextCase) {
            flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), condNode);
          }
          toNextCase.clear();

          DiGraphNode caseNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, caseVal));

          DiGraphNode<MyNode, Branch> blockNode = null;
          List<Pair> blockLeafs = new ArrayList<Pair>();
          rebuild(blockBranch, blockNode, blockLeafs);

          flowGraph.connect(caseNode, Branch.MY_TRUE, blockNode);
          toNextCase.add(new Pair(caseNode, Branch.MY_FALSE));

          if (loopsBreaks.size() == 0) {
            toNextCase.addAll(blockLeafs);
          } else {
            leafs.addAll(blockLeafs);
            // FIXME: nested switch will fail here
            loopsBreaks.clear();
          }

        } else {
          Node blockBrach = child.getFirstChild();
          DiGraphNode<MyNode, Branch> blockNode = null;
          List<Pair> blockLeafs = new ArrayList<Pair>();

          rebuild(blockBrach, blockNode, blockLeafs);
          for (Pair leaf : toNextCase) {
            flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), blockNode);
          }
          toNextCase.clear();

          if (loopsBreaks.size() == 0) {
            toNextCase.addAll(blockLeafs);
          } else {
            leafs.addAll(blockLeafs);
            // FIXME: nested switch will fail here
            loopsBreaks.clear();
          }

        }

      }
      return null;
    }

    private TempValue handleDo(Node entry, DiGraphNode<MyNode, Branch> first, List<Pair> leafs) throws UnimplTransformEx {
      Node bodyBlock = entry.getFirstChild();
      Node condBlock = bodyBlock.getNext();


      List<Pair> bodyLeafs = new ArrayList<Pair>();

      DiGraphNode<MyNode, Branch> condFirst = null;
      List<Pair> condLeafs = new ArrayList<Pair>();
      TempValue val = rebuild(condBlock, condFirst, condLeafs);

      // in 'do .. continue .. while (condition)' continue jumps to 'condition'
      loopsRoots.add(condFirst);

      rebuild(bodyBlock, first, bodyLeafs);

      DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
      for (Pair leaf : bodyLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), condFirst);
      }
      for (Pair leaf : condLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), ifNode);
      }
      flowGraph.connect(ifNode, Branch.MY_TRUE, first);
      leafs.add(new Pair(ifNode, Branch.MY_FALSE));

      loopsRoots.remove(loopsRoots.size() - 1);
      for (DiGraphNode<MyNode, Branch> breakNode : loopsBreaks) {
        leafs.add(new Pair(breakNode, Branch.MY_UNCOND));
      }
      loopsBreaks.clear();

      return null;
    }

    private TempValue handleWhile(Node entry, DiGraphNode<MyNode, Branch> first, List<Pair> leafs) throws UnimplTransformEx {
      Node condBlock = entry.getFirstChild();
      Node bodyBlock = condBlock.getNext();
      List<Pair> condLeafs = new ArrayList<Pair>();

      loopsRoots.add(first);

      TempValue val = rebuild(condBlock, first, condLeafs);
      DiGraphNode<MyNode, Branch> whileNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
      for (Pair leaf : condLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), whileNode);
      }

      List<Pair> bodyLeafs = new ArrayList<Pair>();
      DiGraphNode<MyNode, Branch> bodyNode = null;
      rebuild(bodyBlock, bodyNode, bodyLeafs);

      for (Pair leaf : bodyLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), first);
      }

      flowGraph.connect(whileNode, Branch.MY_TRUE, bodyNode);
      leafs.add(new Pair(whileNode, Branch.MY_FALSE));

      loopsRoots.remove(loopsRoots.size() - 1);
      for (DiGraphNode<MyNode, Branch> breakNode : loopsBreaks) {
        leafs.add(new Pair(breakNode, Branch.MY_UNCOND));
      }
      loopsBreaks.clear();

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