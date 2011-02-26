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
  /**
   * breakable represent a list of subgraphs. List's tail getLeafs() contains all leafs,
   * that should be created after subgraph processing.
   */
  LinkedList<MySubproduct> breakable;
  /**
   * continueable represent a list of subgraphs. List's tail getFirst() corresponds to
   * node that control flow reach after CONTINUE instruction
   */
  LinkedList<MySubproduct> continueable;
  /**
   * labels represent map 'Label name' -> 'Node where to jump'
   */
  Map labels = new HashMap<String, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>>();

  int tempVarCounter;

  public MyFlowGraphCreator(AbstractCompiler compiler) {
    this.compiler = compiler;
    this.flowGraph = new MyFlowGraph();
    this.pseudoRoot = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.PSEUDO_ROOT));
    this.pseudoExit = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.PSEUDO_EXIT));
    this.tempVarCounter = 0;
    this.breakable = new LinkedList<MySubproduct>();
    this.continueable = new LinkedList<MySubproduct>();

    MySubproduct.setGraph(flowGraph);
  }

  public void process(Node externs, Node root) {
    try {
      MySubproduct result = rebuild(root);
      result.connectToFirst(pseudoRoot);
      result.connectLeafsTo(pseudoExit);

    } catch (UnimplTransformEx unimplTransformEx) {
      unimplTransformEx.printStackTrace();
    } catch (UnexpectedNode unexpectedNode) {
      unexpectedNode.printStackTrace();
    }
  }

  public MySubproduct rebuild(Node entry) throws UnimplTransformEx, UnexpectedNode {
    switch (entry.getType()) {
      case Token.IF:
        return handleIf(entry);
      case Token.WHILE:
        return handleWhile(entry);
      case Token.DO:
        return handleDo(entry);
      case Token.FOR:
        return handleFor(entry);
      case Token.SWITCH:
        return handleSwitch(entry);
      case Token.BREAK:
        return handleBreak(entry);
      case Token.CONTINUE:
        return handleContinue(entry);
      case Token.SCRIPT:
      case Token.BLOCK:
        return handleBlock(entry);
      case Token.VAR:
        return handleVar(entry);
      case Token.STRING:
        return handleString(entry);
      case Token.NUMBER:
        return handleNumber(entry);
      case Token.EXPR_RESULT:
        return handleExpression(entry);
      case Token.ASSIGN:
        return handleAssign(entry);
      case Token.NAME:
        return handleName(entry);
      case Token.GETPROP:
        return handleGetProperty(entry);

      default:
        throw new UnimplTransformEx(entry);
    }

  }

  private MySubproduct handleContinue(Node entry) {
    MySubproduct out = MySubproduct.newBuffer();

    DiGraph.DiGraphNode nan = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.NAN));
    out.setFirst(nan);
    // TODO : should I add dead leaf here ?

    MySubproduct lastCont = continueable.getLast();
    flowGraph.connect(nan, MyFlowGraph.Branch.UNCOND, lastCont.getFirst());

    return out;
  }

  private MySubproduct handleBreak(Node entry) {
    MySubproduct out = MySubproduct.newBuffer();

    DiGraph.DiGraphNode nan = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.NAN));
    out.setFirst(nan);
    // TODO : should I add dead leaf here ?

    MySubproduct lastBreakable = breakable.getLast();
    lastBreakable.addLeaf(nan);

    return out;
  }

  private MySubproduct handleGetProperty(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node objBlock = entry.getFirstChild();
    Node indexBlock = objBlock.getNext();

    // TODO : check operations order
    MySubproduct object = readNameOrRebuild(objBlock);
    MySubproduct indexNode = rebuild(indexBlock);

    if (indexBlock.getType() != Token.STRING) {
      throw new UnexpectedNode(indexBlock);
    }

    object.connectLeafsTo(indexNode.getFirst());

    MySubproduct property = MySubproduct.newTemp();
    DiGraph.DiGraphNode readNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.READ_PROPERTY, object, indexNode, property));
    property.addLeaf(readNode);
    property.setFirst(object.getFirst());

    return property;
  }

  /**
   * Create temprorary variable with program's variable name. Doesn't affect flow graph. Necessary nodes should be created in other handlers.
   *
   * @param entry AST node that should be processed with this handler
   * @return result of the processing
   * @see #flowGraph
   * @see MySubproduct
   */
  private MySubproduct handleName(Node entry) {
    MySubproduct varName = MySubproduct.newVarName(entry.getString());
    return varName;
  }

  private MySubproduct handleFor(Node entry) {
    Node forBlock = entry.getFirstChild().getNext().getNext().getNext();

    if (forBlock == null) {
      return handleForIn(entry);
    } else {
      return handleForCstyle(entry);
    }
  }

  private MySubproduct handleForCstyle(Node entry) {
    return null;
  }

  private MySubproduct handleForIn(Node entry) {
    return null;
  }

  private MySubproduct handleAssign(Node entry) {
    return null;
  }

  private MySubproduct handleExpression(Node entry) throws UnimplTransformEx, UnexpectedNode {
    if (entry.hasOneChild()) {
      return rebuild(entry);
    } else {
      throw new UnimplTransformEx(entry);
    }
  }

  private MySubproduct handleNumber(Node entry) {
    MySubproduct tempVar = MySubproduct.newTemp();
    MySubproduct constValue = MySubproduct.newNumber(entry.getDouble());

    DiGraph.DiGraphNode first = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.CONSTANT, constValue, tempVar));
    tempVar.setFirst(first);
    tempVar.addLeaf(first);

    return tempVar;
  }

  private MySubproduct handleString(Node entry) {
    MySubproduct tempVar = MySubproduct.newTemp();
    MySubproduct constValue = MySubproduct.newString(entry.getString());

    DiGraph.DiGraphNode first = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.CONSTANT, constValue, tempVar));
    tempVar.setFirst(first);
    tempVar.addLeaf(first);

    return tempVar;
  }

  private MySubproduct handleVar(Node entry) throws UnexpectedNode, UnimplTransformEx {
    MySubproduct toNextNode = MySubproduct.newBuffer();

    // get first variable declaration
    Node varBlock = entry.getFirstChild();
    MySubproduct varName = MySubproduct.newVarName(varBlock.getString());
    DiGraph.DiGraphNode varDecl = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.DECLARE_VARIABLE, varName));
    varName.setFirst(varDecl);

    // shouldn't be overwritten or used in this method
    toNextNode.setFirst(varDecl);

    if (varBlock.hasOneChild()) {
      // if variable has initialization block
      MySubproduct varValue = readNameOrRebuild(varBlock.getFirstChild());
      DiGraph.DiGraphNode varInit = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_VARIABLE, varValue, varName));

      varValue.connectToFirst(varDecl);
      varValue.connectLeafsTo(varInit);

      toNextNode.addLeaf(varInit);
    } else if (varBlock.hasMoreThanOneChild()) {
      // if something totally unexpected happened
      throw new UnexpectedNode(varBlock);
    } else {
      // if it hasn't initialization block
      toNextNode.addLeaf(varDecl);
    }

    while ((varBlock = varBlock.getNext()) != null) {
      varName = MySubproduct.newVarName(varBlock.getString());
      varDecl = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.DECLARE_VARIABLE, varName));
      varName.setFirst(varDecl);
      toNextNode.connectLeafsTo(varDecl);
      toNextNode.getLeafs().clear();

      if (varBlock.hasOneChild()) {
        MySubproduct varValue = readNameOrRebuild(varBlock.getFirstChild());
        DiGraph.DiGraphNode varInit = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_VARIABLE, varValue, varName));

        varValue.connectToFirst(varDecl);
        varValue.connectLeafsTo(varInit);

        toNextNode.addLeaf(varInit);

      } else if (varBlock.hasMoreThanOneChild()) {
        throw new UnexpectedNode(varBlock);
      } else {
        toNextNode.addLeaf(varDecl);
      }
    }

    return toNextNode;
  }

  private MySubproduct handleBlock(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node block = entry.getFirstChild();

    MySubproduct firstBlock = rebuild(block);
    MySubproduct prevBlock = firstBlock;

    while ((block = block.getNext()) != null) {
      MySubproduct currBlock = rebuild(block);
      prevBlock.connectLeafsTo(currBlock.getFirst());
      prevBlock = currBlock;
    }

    prevBlock.setFirst(firstBlock.getFirst());
    return prevBlock;
  }

  private MySubproduct handleSwitch(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node variableBlock = entry.getFirstChild();
    Node child = variableBlock.getNext();
    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct toNextCase = MySubproduct.newBuffer();

    MySubproduct switchVar = readNameOrRebuild(variableBlock);
    out.setFirst(switchVar.getFirst());
    breakable.push(out);

    switch (child.getType()) {
      case Token.CASE:
        Node condBlock = child.getFirstChild();
        Node exprBlock = condBlock.getNext();

        MySubproduct caseVal = readNameOrRebuild(condBlock);
        switchVar.connectLeafsTo(caseVal);

        MySubproduct cmpRes = MySubproduct.newTemp();
        DiGraph.DiGraphNode eqNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.EQ, switchVar, caseVal, cmpRes));
        caseVal.connectLeafsTo(eqNode);

        DiGraph.DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, caseVal));
        MySubproduct exprNode = rebuild(exprBlock);
        exprNode.connectToFirst(ifNode, MyFlowGraph.Branch.TRUE);

        toNextCase.addLeaf(ifNode, MyFlowGraph.Branch.FALSE);
        toNextCase.addLeaf(exprNode.getLeafs());
        break;

      case Token.DEFAULT:
        exprBlock = child.getFirstChild();

        exprNode = rebuild(exprBlock);
        switchVar.connectLeafsTo(exprNode);
        toNextCase.addLeaf(exprNode.getLeafs());

        break;
      default:
        throw new UnexpectedNode(child);
    }


    while ((child = entry.getNext()) != null) {
      switch (child.getType()) {
        case Token.CASE:
          Node condBlock = child.getFirstChild();
          Node exprBlock = condBlock.getNext();

          MySubproduct caseVal = readNameOrRebuild(condBlock);
          toNextCase.connectLeafsTo(caseVal);
          toNextCase.getLeafs().clear();

          MySubproduct cmpRes = MySubproduct.newTemp();
          DiGraph.DiGraphNode eqNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.EQ, switchVar, caseVal, cmpRes));
          caseVal.connectLeafsTo(eqNode);

          DiGraph.DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, caseVal));
          MySubproduct exprNode = rebuild(exprBlock);
          exprNode.connectToFirst(ifNode, MyFlowGraph.Branch.TRUE);

          toNextCase.addLeaf(ifNode, MyFlowGraph.Branch.FALSE);
          toNextCase.addLeaf(exprNode.getLeafs());
          break;

        case Token.DEFAULT:
          exprBlock = child.getFirstChild();

          exprNode = rebuild(exprBlock);
          toNextCase.connectLeafsTo(exprNode);
          toNextCase.getLeafs().clear();
          toNextCase.addLeaf(exprNode.getLeafs());

          break;
        default:
          throw new UnexpectedNode(child);
      }
    }

    breakable.pop();
    return out;
  }

  private MySubproduct handleDo(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node bodyBlock = entry.getFirstChild();
    Node condBlock = bodyBlock.getNext();
    MySubproduct out = MySubproduct.newBuffer();

    MySubproduct condNode = readNameOrRebuild(condBlock);

    breakable.push(out);
    continueable.push(condNode);

    MySubproduct bodyNode = rebuild(bodyBlock);
    bodyNode.connectLeafsTo(condNode);
    DiGraph.DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, condNode));
    condNode.connectLeafsTo(ifNode);

    flowGraph.connect(ifNode, MyFlowGraph.Branch.TRUE, bodyNode.getFirst());

    out.setFirst(bodyNode.getFirst());
    out.addLeaf(ifNode, MyFlowGraph.Branch.FALSE);

    breakable.pop();
    continueable.pop();

    return out;
  }

  private MySubproduct handleWhile(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node condBlock = entry.getFirstChild();
    Node bodyBlock = condBlock.getNext();
    MySubproduct out = MySubproduct.newBuffer();

    MySubproduct condNode = readNameOrRebuild(condBlock);
    out.setFirst(condNode.getFirst());
    breakable.add(out);
    continueable.add(condNode);

    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, condNode));
    condNode.connectLeafsTo(ifNode);

    MySubproduct bodyNode = rebuild(bodyBlock);
    bodyNode.connectLeafsTo(condNode);

    flowGraph.connect(ifNode, MyFlowGraph.Branch.TRUE, bodyNode);
    out.addLeaf(ifNode, MyFlowGraph.Branch.FALSE);

    return out;
  }

  private MySubproduct handleIf(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node condBlock = entry.getFirstChild();
    Node thenBlock = condBlock.getNext();
    Node elseBlock = thenBlock.getNext();
    MySubproduct out = MySubproduct.newBuffer();

    MySubproduct condNode = readNameOrRebuild(condBlock);
    out.setFirst(condNode.getFirst());
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, condNode));
    condNode.connectLeafsTo(ifNode);

    MySubproduct thenNode = rebuild(thenBlock);
    out.addLeaf(thenNode.getLeafs());

    flowGraph.connect(ifNode, MyFlowGraph.Branch.TRUE, thenNode);

    if (elseBlock != null) {
      MySubproduct elseNode = rebuild(elseBlock);
      out.addLeaf(elseNode.getLeafs());
      flowGraph.connect(ifNode, MyFlowGraph.Branch.FALSE, elseNode);
    } else {
      out.addLeaf(ifNode, MyFlowGraph.Branch.FALSE);
    }

    return out;
  }

  /**
   * Supporing function for rebuilding node that should result in
   * either program's variable name either temprorary variable name
   *
   * @param expBlock node that should be processed
   * @return
   * @throws UnimplTransformEx
   * @throws UnexpectedNode
   */
  private MySubproduct readNameOrRebuild(Node expBlock) throws UnimplTransformEx, UnexpectedNode {
    MySubproduct value = rebuild(expBlock);
    MySubproduct out = null;

    if (value.isVarName()) {
      out = MySubproduct.newTemp();
      DiGraph.DiGraphNode readNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.READ_VARIABLE, value, out));
      out.setFirst(readNode);
      out.addLeaf(readNode);
    } else {
      out = value;
    }

    return out;
  }

  public MyFlowGraph getFlowGraph() {
    return flowGraph;
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
