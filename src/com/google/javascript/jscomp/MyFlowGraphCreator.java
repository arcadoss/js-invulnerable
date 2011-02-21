package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.*;

/**
 * User: arcadoss
 * Date: 17.02.11
 * Time: 15:09
 */
public class MyFlowGraphCreator implements CompilerPass {
  private final AbstractCompiler compiler;

  MyFlowGraph flowGraph;
  DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> pseudoRoot;
  DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> pseudoExit;
  List<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>> loopsRoots = new ArrayList<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>>();
  List<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>> loopsBreaks = new LinkedList<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>>();
  Map labels = new HashMap<String, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>>();

  public MyFlowGraphCreator(AbstractCompiler compiler) {
    this.compiler = compiler;
    this.flowGraph = new MyFlowGraph();
    this.pseudoRoot = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_PSEUDO_ROOT));
    this.pseudoExit = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_PSEUDO_EXIT));
  }

  public void process(Node externs, Node root) {
    try {
      DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first = null;
      List<Pair> leafs = new ArrayList<Pair>();

      rebuild(root, first, leafs);
      flowGraph.connect(pseudoRoot, MyFlowGraph.Branch.MY_UNCOND, first);
      for (Pair leaf : leafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), pseudoExit);
      }


    } catch (UnimplTransformEx unimplTransformEx) {
      unimplTransformEx.printStackTrace();
    } catch (UnexpectedNode unexpectedNode) {
      unexpectedNode.printStackTrace();
    }
  }

  public MyAbsValue rebuild(Node entry, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> root, List<Pair> leafs) throws UnimplTransformEx, UnexpectedNode {
    switch (entry.getType()) {
      case Token.IF:
        return handleIf(entry, root, leafs);
      case Token.WHILE:
        return handleWhile(entry, root, leafs);
      case Token.DO:
        return handleDo(entry, root, leafs);
      case Token.SWITCH:
        return handleSwitch(entry, root, leafs);
      case Token.SCRIPT:
      case Token.BLOCK:
        return handleBlock(entry, root, leafs);
      case Token.VAR:
        return handleVar(entry, root, leafs);

      default:
        throw new UnimplTransformEx(entry);
    }

  }

  private MyAbsValue handleVar(Node entry, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first, List<Pair> leafs) throws UnexpectedNode, UnimplTransformEx {
    Node varBlock = entry.getFirstChild();
    MyAbsValue varName;
    DiGraph.DiGraphNode varNode;
    Pair toNextNode;

    // get variable's name
    varName = MyAbsValue.newString(varBlock.getString());
    varNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_DECLARE_VARIABLE, varName));
    first = varNode;

    if (varBlock.hasOneChild()) {
      List<Pair> valueLeafs = new ArrayList<Pair>();
      DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> valueFirst = null;
      MyAbsValue tempName;

      tempName = rebuild(varBlock.getFirstChild(), valueFirst, valueLeafs);
      DiGraph.DiGraphNode assignNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_WRITE_VARIABLE, tempName, varName));

      flowGraph.connect(varNode, MyFlowGraph.Branch.MY_UNCOND, valueFirst);
      for (Pair leaf : valueLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), assignNode);
      }

      toNextNode = new Pair(assignNode, MyFlowGraph.Branch.MY_UNCOND);
    } else if (varBlock.hasMoreThanOneChild()) {
      throw new UnexpectedNode(varBlock);
    } else {
      toNextNode = new Pair(varNode, MyFlowGraph.Branch.MY_UNCOND);
    }

    while ((varBlock = varBlock.getNext()) != null) {
      varName = MyAbsValue.newString(varBlock.getString());
      varNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_DECLARE_VARIABLE, varName));
      flowGraph.connect(toNextNode.getNode(), toNextNode.getAnnotaion(), varNode);

      if (varBlock.hasOneChild()) {
        List<Pair> valueLeafs = new ArrayList<Pair>();
        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> valueFirst = null;
        MyAbsValue tempName;

        tempName = rebuild(varBlock.getFirstChild(), valueFirst, valueLeafs);
        DiGraph.DiGraphNode assignNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_WRITE_VARIABLE, tempName, varName));

        flowGraph.connect(varNode, MyFlowGraph.Branch.MY_UNCOND, valueFirst);
        for (Pair leaf : valueLeafs) {
          flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), assignNode);
        }

        toNextNode = new Pair(assignNode, MyFlowGraph.Branch.MY_UNCOND);
      } else if (varBlock.hasMoreThanOneChild()) {
        throw new UnexpectedNode(varBlock);
      } else {
        toNextNode = new Pair(varNode, MyFlowGraph.Branch.MY_UNCOND);
      }
    }

    leafs.add(toNextNode);

    return null;
  }

  private MyAbsValue handleBlock(Node entry, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first, List<Pair> leafs) throws UnimplTransformEx, UnexpectedNode {
    List<Pair> prevLeafs = new ArrayList<Pair>();
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> currFirst = first;

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

  private MyAbsValue handleSwitch(Node entry, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first, List<Pair> leafs) throws UnimplTransformEx, UnexpectedNode {
    Node expBranch = entry.getFirstChild();
    Node defaultBranch = null;

    List<Pair> expLeafs = new ArrayList<Pair>();
    MyAbsValue val = rebuild(expBranch, first, expLeafs);

    Node child = entry.getNext();
    List<Pair> toNextCase = new ArrayList<Pair>();

    if (child.getType() == Token.CASE) {
      Node condBranch = child.getFirstChild();
      Node blockBranch = condBranch.getNext();

      List<Pair> condLeafs = new ArrayList<Pair>();
      DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> condNode = null;
      MyAbsValue caseVal = rebuild(condBranch, condNode, condLeafs);

      for (Pair leaf : expLeafs) {
        flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), condNode);
      }

      DiGraph.DiGraphNode caseNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, caseVal));

      DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> blockNode = null;
      List<Pair> blockLeafs = new ArrayList<Pair>();
      rebuild(blockBranch, blockNode, blockLeafs);

      flowGraph.connect(caseNode, MyFlowGraph.Branch.MY_TRUE, blockNode);
      toNextCase.add(new Pair(caseNode, MyFlowGraph.Branch.MY_FALSE));

      if (loopsBreaks.size() == 0) {
        toNextCase.addAll(blockLeafs);
      } else {
        leafs.addAll(blockLeafs);
        // FIXME: nested switch will fail here
        loopsBreaks.clear();
      }

    } else {
      Node blockBrach = child.getFirstChild();
      DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> blockNode = null;
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
        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> condNode = null;
        MyAbsValue caseVal = rebuild(condBranch, condNode, condLeafs);

        for (Pair leaf : toNextCase) {
          flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), condNode);
        }
        toNextCase.clear();

        DiGraph.DiGraphNode caseNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, caseVal));

        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> blockNode = null;
        List<Pair> blockLeafs = new ArrayList<Pair>();
        rebuild(blockBranch, blockNode, blockLeafs);

        flowGraph.connect(caseNode, MyFlowGraph.Branch.MY_TRUE, blockNode);
        toNextCase.add(new Pair(caseNode, MyFlowGraph.Branch.MY_FALSE));

        if (loopsBreaks.size() == 0) {
          toNextCase.addAll(blockLeafs);
        } else {
          leafs.addAll(blockLeafs);
          // FIXME: nested switch will fail here
          loopsBreaks.clear();
        }

      } else {
        Node blockBrach = child.getFirstChild();
        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> blockNode = null;
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

  private MyAbsValue handleDo(Node entry, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first, List<Pair> leafs) throws UnimplTransformEx, UnexpectedNode {
    Node bodyBlock = entry.getFirstChild();
    Node condBlock = bodyBlock.getNext();


    List<Pair> bodyLeafs = new ArrayList<Pair>();

    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> condFirst = null;
    List<Pair> condLeafs = new ArrayList<Pair>();
    MyAbsValue val = rebuild(condBlock, condFirst, condLeafs);

    // in 'do .. continue .. while (condition)' continue jumps to 'condition'
    loopsRoots.add(condFirst);

    rebuild(bodyBlock, first, bodyLeafs);

    DiGraph.DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
    for (Pair leaf : bodyLeafs) {
      flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), condFirst);
    }
    for (Pair leaf : condLeafs) {
      flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), ifNode);
    }
    flowGraph.connect(ifNode, MyFlowGraph.Branch.MY_TRUE, first);
    leafs.add(new Pair(ifNode, MyFlowGraph.Branch.MY_FALSE));

    loopsRoots.remove(loopsRoots.size() - 1);
    for (DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> breakNode : loopsBreaks) {
      leafs.add(new Pair(breakNode, MyFlowGraph.Branch.MY_UNCOND));
    }
    loopsBreaks.clear();

    return null;
  }

  private MyAbsValue handleWhile(Node entry, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first, List<Pair> leafs) throws UnimplTransformEx, UnexpectedNode {
    Node condBlock = entry.getFirstChild();
    Node bodyBlock = condBlock.getNext();
    List<Pair> condLeafs = new ArrayList<Pair>();

    loopsRoots.add(first);

    MyAbsValue val = rebuild(condBlock, first, condLeafs);
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> whileNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
    for (Pair leaf : condLeafs) {
      flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), whileNode);
    }

    List<Pair> bodyLeafs = new ArrayList<Pair>();
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> bodyNode = null;
    rebuild(bodyBlock, bodyNode, bodyLeafs);

    for (Pair leaf : bodyLeafs) {
      flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), first);
    }

    flowGraph.connect(whileNode, MyFlowGraph.Branch.MY_TRUE, bodyNode);
    leafs.add(new Pair(whileNode, MyFlowGraph.Branch.MY_FALSE));

    loopsRoots.remove(loopsRoots.size() - 1);
    for (DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> breakNode : loopsBreaks) {
      leafs.add(new Pair(breakNode, MyFlowGraph.Branch.MY_UNCOND));
    }
    loopsBreaks.clear();

    return null;
  }

  private MyAbsValue handleIf(Node entry,
                              DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> first,
                              List<Pair> leafs) throws UnimplTransformEx, UnexpectedNode {
    Node condition = entry.getFirstChild();
    Node thenBlock = condition.getNext();
    Node elseBlock = thenBlock.getNext();

    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> thenNode = null;
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> elseNode = null;
    List<Pair> condLeafs = new ArrayList<Pair>();

    MyAbsValue val = rebuild(condition, first, condLeafs);
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.MY_IF, val));
    for (Pair leaf : condLeafs) {
      flowGraph.connect(leaf.getNode(), leaf.getAnnotaion(), ifNode);
    }

    rebuild(thenBlock, thenNode, leafs);
    flowGraph.connect(ifNode, MyFlowGraph.Branch.MY_TRUE, thenNode);

    if (elseBlock != null) {
      rebuild(elseBlock, elseNode, leafs);
      flowGraph.connect(ifNode, MyFlowGraph.Branch.MY_FALSE, elseNode);
    }

    return null;
  }

  public MyFlowGraph getFlowGraph() {
    return flowGraph;
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


  private class UnexpectedNode extends Throwable {
    public UnexpectedNode(Node child) {
      super("Unexpected node " + child);
    }
  }

  private class UnimplTransformEx extends Exception {
    public UnimplTransformEx(Node node) {
      super("Transformation of " + node.toString() + " is unimplemented");
    }
  }

}
