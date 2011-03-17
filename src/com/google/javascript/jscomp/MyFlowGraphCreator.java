package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.*;

/**
 * This compiler pass creates program's control flow graph using
 * special nodes set. It used for abstract interpretation.
 *
 * @author arcadoss
 */
public class MyFlowGraphCreator implements CompilerPass {
  private final AbstractCompiler compiler;

  MyFlowGraph flowGraph;
  final DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> entry;
  final DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> implicitReturn;
  final DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> exceptExit;

  /**
   * breakable represent a list of subgraphs. List's tail getLeafs() contains all leafs,
   * that should be created after subgraph processing.
   */
  LinkedList<MySubproduct> breakable;
  /**
   * continueable represent a list of subgraphs. List's tail getFirst() corresponds to
   * node that control flow reach after CONTINUE instruction
   */
  LinkedList<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>> continueable;
  /**
   * represent list of nodes that will receive control flow after exception
   */
  LinkedList<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>> catchers;
  /**
   * labels represent map 'Label name' -> 'Node where to jump'
   */
  Map<String, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>> labels;

  public MyFlowGraphCreator(AbstractCompiler compiler) {
    this.compiler = compiler;
    this.flowGraph = new MyFlowGraph();
    this.entry = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.PSEUDO_ROOT));
    this.implicitReturn = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.PSEUDO_EXIT));
    this.exceptExit = this.flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.EXIT_EXC));

    this.breakable = new LinkedList<MySubproduct>();
    this.continueable = new LinkedList<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>>();
    this.catchers = new LinkedList<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>>();
    this.labels = new HashMap<String, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>>();

    this.catchers.push(exceptExit);

    MySubproduct.setGraph(flowGraph);
  }

  public void process(Node externs, Node root) {
    try {
      MySubproduct result = rebuild(root);
      result.connectToFirst(entry);
      result.connectLeafsTo(implicitReturn);

    } catch (UnimplTransformEx unimplTransformEx) {
      unimplTransformEx.printStackTrace();
    } catch (UnexpectedNode unexpectedNode) {
      unexpectedNode.printStackTrace();
    }
  }

  public MySubproduct rebuild(Node entry) throws UnimplTransformEx, UnexpectedNode {
    switch (entry.getType()) {
      // control flow modifiers
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

      case Token.EMPTY:
        return handleEmpty(entry);
      case Token.SCRIPT:
      case Token.BLOCK:
        return handleBlock(entry);
      case Token.EXPR_RESULT:
        return handleExpression(entry);

      case Token.VAR:
        return handleVar(entry);
      case Token.STRING:
        return handleString(entry);
      case Token.NUMBER:
        return handleNumber(entry);
      case Token.NAME:
        return handleName(entry);

      case Token.GETPROP:
        return handleGetProperty(entry);
      case Token.GETELEM:
        return handleGetElem(entry);
      case Token.WITH:
        return handleWith(entry);
      case Token.NEW:
        return handleNew(entry);
//      case Token.DELPROP:
//        return handleDelProp(entry);

      // assignment operations
      case Token.ASSIGN:
        return handleAssign(entry);
      case Token.ASSIGN_BITOR:
        return handleAssignWith(entry, MyNode.Type.BITOR);
      case Token.ASSIGN_BITXOR:
        return handleAssignWith(entry, MyNode.Type.BITXOR);
      case Token.ASSIGN_BITAND:
        return handleAssignWith(entry, MyNode.Type.BITAND);
      case Token.ASSIGN_LSH:
        return handleAssignWith(entry, MyNode.Type.LSH);
      case Token.ASSIGN_RSH:
        return handleAssignWith(entry, MyNode.Type.RSH);
      case Token.ASSIGN_URSH:
        return handleAssignWith(entry, MyNode.Type.URSH);
      case Token.ASSIGN_ADD:
        return handleAssignWith(entry, MyNode.Type.ADD);
      case Token.ASSIGN_SUB:
        return handleAssignWith(entry, MyNode.Type.SUB);
      case Token.ASSIGN_MUL:
        return handleAssignWith(entry, MyNode.Type.MUL);
      case Token.ASSIGN_DIV:
        return handleAssignWith(entry, MyNode.Type.DIV);
      case Token.ASSIGN_MOD:
        return handleAssignWith(entry, MyNode.Type.MOD);

      case Token.CALL:
        return handleCall(entry);
      case Token.FUNCTION:
        return handleFunction(entry);
      case Token.RETURN:
        return handleReturn(entry);

      // unary operations
      case Token.NEG:
        return handleUnOp(entry, MyNode.Type.NEG);
      case Token.POS:
        return handleUnOp(entry, MyNode.Type.POS);
      case Token.BITNOT:
        return handleUnOp(entry, MyNode.Type.BITNOT);
      case Token.NOT:
        return handleUnOp(entry, MyNode.Type.NOT);
      case Token.TYPEOF:
        return handleUnOp(entry, MyNode.Type.TYPEOF);
      case Token.DEC:
        return handleIncDec(entry, MyNode.Type.DEC);
      case Token.INC:
        return handleIncDec(entry, MyNode.Type.INC);

      // binary operations
      case Token.BITOR:
        return handleBinOp(entry, MyNode.Type.BITOR);
      case Token.BITXOR:
        return handleBinOp(entry, MyNode.Type.BITXOR);
      case Token.BITAND:
        return handleBinOp(entry, MyNode.Type.BITAND);
      case Token.AND:
        return handleBinOp(entry, MyNode.Type.AND);
      case Token.OR:
        return handleBinOp(entry, MyNode.Type.OR);
      case Token.EQ:
        return handleBinOp(entry, MyNode.Type.EQ);
      case Token.NE:
        return handleBinOp(entry, MyNode.Type.NE);
      case Token.SHEQ:
        return handleBinOp(entry, MyNode.Type.SHEQ);
      case Token.SHNE:
        return handleBinOp(entry, MyNode.Type.SHNE);
      case Token.LT:
        return handleBinOp(entry, MyNode.Type.LT);
      case Token.LE:
        return handleBinOp(entry, MyNode.Type.LE);
      case Token.GT:
        return handleBinOp(entry, MyNode.Type.GT);
      case Token.GE:
        return handleBinOp(entry, MyNode.Type.GE);
      case Token.LSH:
        return handleBinOp(entry, MyNode.Type.LSH);
      case Token.RSH:
        return handleBinOp(entry, MyNode.Type.RSH);
      case Token.URSH:
        return handleBinOp(entry, MyNode.Type.URSH);
      case Token.ADD:
        return handleBinOp(entry, MyNode.Type.ADD);
      case Token.SUB:
        return handleBinOp(entry, MyNode.Type.SUB);
      case Token.MUL:
        return handleBinOp(entry, MyNode.Type.MUL);
      case Token.DIV:
        return handleBinOp(entry, MyNode.Type.DIV);
      case Token.MOD:
        return handleBinOp(entry, MyNode.Type.MOD);
      case Token.INSTANCEOF:
        return handleBinOp(entry, MyNode.Type.INSTANCEOF);
      case Token.IN:
        return handleBinOp(entry, MyNode.Type.IN);

      // ternary operations
      case Token.HOOK:
        return handleHook(entry);

      // special keywords
      case Token.THIS:
        return handleReadKeyword(entry, "this");
      case Token.NULL:
        return handleReadKeyword(entry, "null");
      case Token.TRUE:
        return handleReadKeyword(entry, "true");
      case Token.FALSE:
        return handleReadKeyword(entry, "false");

      // exceptions
      case Token.TRY:
        return handleTry(entry);
      case Token.THROW:
        return handleThrow(entry);
      case Token.CATCH:
        return handleCatch(entry);

      default:
        throw new UnimplTransformEx(entry);
    }

  }

//  private MySubproduct handleDelProp(Node entry) {
//    return null;
//  }

  private MySubproduct handleNew(Node entry) throws UnimplTransformEx, UnexpectedNode {
    MySubproduct result = MySubproduct.newTemp();

    assert entry.getFirstChild().getType() == Token.NAME;

    List<MySubproduct> list = new ArrayList<MySubproduct>();
    for (Node child : entry.children()) {
      MySubproduct param = readNameOrRebuild(child);
      list.add(param);
    }

    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> constNode
        = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.CONSTRUCT, list));
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> afterCall
        = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.AFTER_CALL, result));

    connect(list);
    result.setFirst(list.get(0).getFirst());
    list.get(list.size() - 1).connectLeafsTo(constNode);
    connect(constNode, MyFlowGraph.Branch.UNCOND, afterCall);
    result.addLeaf(afterCall);

    return result;
  }

  private void connect(List<MySubproduct> list) {
    Iterator<MySubproduct> iter = list.iterator();
    MySubproduct prev = iter.next();
    MySubproduct curr;

    while (iter.hasNext()) {
      curr = iter.next();
      prev.connectLeafsTo(curr);
    }
  }

  private MySubproduct handleIncDec(Node entry, MyNode.Type op) throws UnimplTransformEx, UnexpectedNode {
    MySubproduct out = null;

    Node expr = entry.getFirstChild();

    switch (expr.getType()) {
      case Token.NAME:
        MySubproduct varName = MySubproduct.newVarName(expr.getString());
        MySubproduct varValue = readNameOrRebuild(expr);

        MySubproduct opRes = MySubproduct.newTemp();

        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> opNode
            = flowGraph.createDirectedGraphNode(new MyNode(op, varValue, opRes));

        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> writeNode
            = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_VARIABLE, opRes, varName));

        if (expr.getBooleanProp(Node.INCRDECR_PROP)) {
          // if it is post increment or decrement
          out = MySubproduct.copyVal(varValue);
        } else {
          // if it is pre increment or decrement
          out = MySubproduct.copyVal(opRes);
        }

        out.setFirst(varValue.getFirst());
        varValue.connectLeafsTo(opNode);
        connect(opNode, MyFlowGraph.Branch.UNCOND, writeNode);
        out.addLeaf(writeNode);

        break;
      case Token.GETPROP:
      case Token.GETELEM:
        Node baseBlock = expr.getFirstChild();
        Node indexBlock = baseBlock.getNext();
        MySubproduct baseNode = readNameOrRebuild(baseBlock);
        MySubproduct indexNode = readNameOrRebuild(indexBlock);

        opRes = MySubproduct.newTemp();
        MySubproduct beforeVal = MySubproduct.newTemp();

        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> readNode
            = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.READ_PROPERTY, baseNode, indexNode, beforeVal));
        opNode = flowGraph.createDirectedGraphNode(new MyNode(op, beforeVal, opRes));
        writeNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_PROPERTY, baseNode, indexNode, opRes));

        if (expr.getBooleanProp(Node.INCRDECR_PROP)) {
          // if it is post increment or decrement
          out = MySubproduct.copyVal(beforeVal);
        } else {
          // if it is pre increment or decrement
          out = MySubproduct.copyVal(opRes);
        }

        out.setFirst(baseNode.getFirst());
        baseNode.connectLeafsTo(indexNode);
        indexNode.connectLeafsTo(readNode);
        connect(readNode, MyFlowGraph.Branch.UNCOND, opNode);
        connect(opNode, MyFlowGraph.Branch.UNCOND, writeNode);
        out.addLeaf(writeNode);

        break;
      default:
        throw new UnexpectedNode(expr);
    }

    return out;
  }

  private MySubproduct handleGetElem(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node objBlock = entry.getFirstChild();
    Node indexBlock = objBlock.getNext();

    MySubproduct object = readNameOrRebuild(objBlock);
    MySubproduct indexNode = rebuild(indexBlock);

    MySubproduct property = MySubproduct.newTemp();
    DiGraph.DiGraphNode readNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.READ_PROPERTY, object, indexNode, property));

    object.connectLeafsTo(indexNode.getFirst());
    indexNode.connectLeafsTo(readNode);
    property.addLeaf(readNode);
    property.setFirst(object.getFirst());

    return property;
  }

  private MySubproduct handleAssignWith(Node entry, MyNode.Type binOp) throws UnimplTransformEx, UnexpectedNode {
    Node childBlock = entry.getFirstChild();
    Node exprBlock = childBlock.getNext();

    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct rightVal = readNameOrRebuild(exprBlock);

    switch (childBlock.getType()) {
      case Token.NAME:
        MySubproduct opResult = MySubproduct.newTemp();
        MySubproduct leftVal = readNameOrRebuild(childBlock);
        MySubproduct dest = MySubproduct.newVarName(childBlock.getString());

        DiGraph.DiGraphNode binOpNode = flowGraph.createDirectedGraphNode(new MyNode(binOp, leftVal, rightVal, opResult));
        DiGraph.DiGraphNode writeNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_VARIABLE, opResult, dest));

        out.setFirst(rightVal.getFirst());
        rightVal.connectLeafsTo(leftVal);
        leftVal.connectLeafsTo(binOpNode);
        connect(binOpNode, MyFlowGraph.Branch.UNCOND, writeNode);
        out.addLeaf(writeNode);

        break;
      case Token.GETPROP:
      case Token.GETELEM:
        Node base = childBlock.getFirstChild();
        Node index = base.getNext();

        MySubproduct prop = rebuild(index);
        dest = readNameOrRebuild(base);

        leftVal = MySubproduct.newTemp();
        opResult = MySubproduct.newTemp();
        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> readNode
            = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.READ_PROPERTY, dest, prop, leftVal));
        binOpNode = flowGraph.createDirectedGraphNode(new MyNode(binOp, leftVal, rightVal, opResult));
        writeNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_PROPERTY, dest, prop, opResult));

        out.setFirst(dest.getFirst());
        dest.connectLeafsTo(prop);
        prop.connectLeafsTo(rightVal);
        rightVal.connectLeafsTo(readNode);
        connect(readNode, MyFlowGraph.Branch.UNCOND, binOpNode);
        connect(binOpNode, MyFlowGraph.Branch.UNCOND, writeNode);
        out.addLeaf(writeNode);

        break;
      default:
        throw new UnexpectedNode(entry);
    }

    return out;
  }

  private MySubproduct handleCatch(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node nameBlock = entry.getFirstChild();
    Node exprBlock = nameBlock.getNext();

    assert nameBlock.getType() == Token.NAME;

    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct name = MySubproduct.newString(nameBlock.getString());
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> catchNode
        = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.CATCH, name));
    MySubproduct exprNode = rebuild(exprBlock);
    exprNode.connectToFirst(catchNode);

    out.setFirst(catchNode);
    out.addLeaf(exprNode.getLeafs());

    return out;
  }

  private MySubproduct handleThrow(Node entry) throws UnimplTransformEx, UnexpectedNode {
    MySubproduct out = MySubproduct.newBuffer();

    Node exprBlock = entry.getFirstChild();
    MySubproduct exprNode = rebuild(exprBlock);
    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> throwNode
        = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.THROW, exprNode));

    exprNode.connectLeafsTo(throwNode);
    out.setFirst(exprNode.getFirst());

    connect(throwNode, MyFlowGraph.Branch.EXEPT, catchers.getLast());

    // TODO: should I add dead branch here

    return out;
  }

  private MySubproduct handleReadKeyword(Node entry, String keywordName) {
    MySubproduct out = MySubproduct.newTemp();
    MySubproduct keyword = MySubproduct.newString(keywordName);

    DiGraph.DiGraphNode readNode = flowGraph.createDirectedGraphNode(
        new MyNode(MyNode.Type.READ_VARIABLE, keyword, out));
    out.setFirst(readNode);
    out.addLeaf(readNode);

    return out;
  }

  // Warning: current parsers doesn't support catch (e if ...)
  private MySubproduct handleTry(Node entry) throws UnexpectedNode, UnimplTransformEx {
    Node exprBlock = entry.getFirstChild();
    Node catchBlock = exprBlock.getNext();
    Node finallyBlock = catchBlock.getNext();

    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct finallyNode = null;

    MySubproduct catchNode;

    // swap catch and try if there is no catch, but only try
    if (catchBlock.hasChildren() && catchBlock.getFirstChild().getType() != Token.CATCH
        || !catchBlock.hasChildren() && finallyBlock == null) {
      Node temp = catchBlock;
      catchBlock = finallyBlock;
      finallyBlock = temp;
    }

    // handle finally block
    if (finallyBlock != null && finallyBlock.hasChildren()) {
      finallyNode = rebuild(finallyBlock);
      catchers.push(finallyNode.getFirst()); // catch block may throw exception, put finally block first
    }

    // handle catch block
    if (!catchBlock.hasChildren()) {
      catchNode = MySubproduct.newNan();
    } else {
      Node cBlock = catchBlock.getFirstChild();
      if (cBlock == null || cBlock.getType() != Token.CATCH) {
        throw new UnexpectedNode(cBlock);
      }
      catchNode = rebuild(cBlock);
    }

    catchers.push(catchNode.getFirst());

    MySubproduct exprNode = rebuild(exprBlock);

    out.setFirst(exprNode.getFirst());
    if (finallyNode != null) {
      exprNode.connectLeafsTo(finallyNode);
      catchNode.connectLeafsTo(finallyNode);
      out.addLeaf(finallyNode.getLeafs());
    } else {
      out.addLeaf(exprNode.getLeafs());
      // TODO: Am I wrong here ?
      out.addLeaf(catchNode.getLeafs());
    }

    catchers.pop(); // pop catchNode
    catchers.pop(); // pop finallyNode

    return out;
  }

  private MySubproduct handleBlock(Node entry) throws UnimplTransformEx, UnexpectedNode {
    if (!entry.hasChildren()) {
      return MySubproduct.newNan();
    }

    Node block = entry.getFirstChild();

    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct firstBlock = rebuild(block);
    MySubproduct prevBlock = firstBlock;

    out.setFirst(firstBlock.getFirst());

    while ((block = block.getNext()) != null) {
      MySubproduct currBlock = rebuild(block);
      prevBlock.connectLeafsTo(currBlock.getFirst());
      prevBlock = currBlock;
    }

    out.addLeaf(prevBlock.getLeafs());
    return out;
  }

  private MySubproduct handleHook(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node cond = entry.getFirstChild();
    Node exprOnTrue = cond.getNext();
    Node exprOnFalse = exprOnTrue.getNext();

    MySubproduct condValue = readNameOrRebuild(cond);
    MySubproduct trueExpNode = readNameOrRebuild(exprOnTrue);
    MySubproduct falseExpNode = readNameOrRebuild(exprOnFalse);

    MySubproduct out = MySubproduct.newTemp();
    DiGraph.DiGraphNode hookNode = flowGraph.createDirectedGraphNode(
        new MyNode(MyNode.Type.HOOK, condValue, trueExpNode, falseExpNode, out));
    out.setFirst(condValue.getFirst());
    condValue.connectLeafsTo(trueExpNode);
    trueExpNode.connectLeafsTo(falseExpNode);
    falseExpNode.connectLeafsTo(hookNode);
    out.addLeaf(hookNode);

    return out;
  }

  private MySubproduct handleBinOp(Node entry, MyNode.Type binOp) throws UnimplTransformEx, UnexpectedNode {
    Node first = entry.getFirstChild();
    Node second = first.getNext();

    MySubproduct out = MySubproduct.newTemp();
    MySubproduct value1 = readNameOrRebuild(first);
    MySubproduct value2 = readNameOrRebuild(second);

    DiGraph.DiGraphNode opNode = flowGraph.createDirectedGraphNode(new MyNode(binOp, value1, value2, out));
    value1.connectLeafsTo(value2);
    value2.connectLeafsTo(opNode);
    out.setFirst(value1.getFirst());
    out.addLeaf(opNode);

    return out;
  }

  private MySubproduct handleUnOp(Node entry, MyNode.Type unOp) throws UnimplTransformEx, UnexpectedNode {
    Node expr = entry.getFirstChild();

    MySubproduct out = MySubproduct.newTemp();
    MySubproduct value = readNameOrRebuild(expr);
    DiGraph.DiGraphNode opNode = flowGraph.createDirectedGraphNode(new MyNode(unOp, value, out));
    value.connectLeafsTo(opNode);
    out.setFirst(value.getFirst());
    out.addLeaf(opNode);

    return out;
  }

  private MySubproduct handleReturn(Node entry) throws UnimplTransformEx, UnexpectedNode {
    if (entry.getFirstChild().getNext() != null)
      throw new UnexpectedNode(entry);

    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct retValue = readNameOrRebuild(entry.getFirstChild());
    DiGraph.DiGraphNode retNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.RETURN, retValue));
    out.setFirst(retNode);
    out.addLeaf(retNode);

    return out;
  }

  private MySubproduct handleFunction(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node nameBlock = entry.getFirstChild();
    Node paramBlock = nameBlock.getNext();
    Node expBlock = paramBlock.getNext();

    MySubproduct out = MySubproduct.newBuffer();

    MySubproduct nameNode = readNameOrRebuild(nameBlock);
    List<MySubproduct> list = new ArrayList<MySubproduct>();
    list.add(nameNode);
    for (Node param : paramBlock.children()) {
      MySubproduct name = rebuild(param);
      list.add(name);
    }

    DiGraph.DiGraphNode funEnter = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.ENTRY, list));
    DiGraph.DiGraphNode funExit = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.EXIT));
    DiGraph.DiGraphNode funExcept = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.EXIT_EXC));
    catchers.push(funExcept);

    MySubproduct expNode = rebuild(expBlock);

    out.setFirst(funEnter);
    connect(funEnter, MyFlowGraph.Branch.UNCOND, expNode.getFirst());
    expNode.connectLeafsTo(funExit);
    out.addLeaf(funEnter);

    catchers.pop();

    return out;
  }

  private MySubproduct handleCall(Node entry) throws UnimplTransformEx, UnexpectedNode {
    List<MySubproduct> list = new ArrayList<MySubproduct>();
    for (Node child : entry.children()) {
      list.add(readNameOrRebuild(child));
    }

    MySubproduct retVal = MySubproduct.newTemp();
    DiGraph.DiGraphNode callNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.CALL, list));
    DiGraph.DiGraphNode afterCallNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.AFTER_CALL, retVal));

    connect(list);
    retVal.setFirst(list.get(0).getFirst());
    list.get(list.size() - 1).connectLeafsTo(callNode);
    connect(callNode, MyFlowGraph.Branch.UNCOND, afterCallNode);
    connect(callNode, MyFlowGraph.Branch.EXEPT, catchers.getLast());
    retVal.addLeaf(afterCallNode);

    return retVal;
  }

  private MySubproduct handleWith(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node varBlock = entry.getFirstChild();
    Node exprBlock = varBlock.getNext();

    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct varNode = readNameOrRebuild(varBlock);
    MySubproduct exprNode = rebuild(exprBlock);
    DiGraph.DiGraphNode withNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WITH, varNode));

    out.setFirst(varNode.getFirst());
    varNode.connectLeafsTo(withNode);
    connect(withNode, MyFlowGraph.Branch.TRUE, exprNode.getFirst());
    out.addLeaf(withNode, MyFlowGraph.Branch.FALSE);
    exprNode.connectLeafsTo(withNode);

    return out;
  }

  private MySubproduct handleEmpty(Node entry) {
    MySubproduct out = MySubproduct.newNan();
    return out;
  }

  private MySubproduct handleContinue(Node entry) {
    MySubproduct out = MySubproduct.newBuffer();

    DiGraph.DiGraphNode nan = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.NAN));
    out.setFirst(nan);
    // TODO : should I add dead leaf here ?

    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> lastCont = continueable.getLast();
    connect(nan, MyFlowGraph.Branch.UNCOND, lastCont);

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

    MySubproduct object = readNameOrRebuild(objBlock);
    MySubproduct indexNode = readNameOrRebuild(indexBlock);

    if (indexBlock.getType() != Token.STRING) {
      throw new UnexpectedNode(indexBlock);
    }

    MySubproduct property = MySubproduct.newTemp();
    DiGraph.DiGraphNode readNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.READ_PROPERTY, object, indexNode, property));

    object.connectLeafsTo(indexNode.getFirst());
    indexNode.connectLeafsTo(readNode);
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

  private MySubproduct handleFor(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node forBlock = entry.getFirstChild().getNext().getNext().getNext();

    if (forBlock == null) {
      return handleForIn(entry);
    } else {
      return handleForCstyle(entry);
    }
  }

  private MySubproduct handleForCstyle(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node varBlock = entry.getFirstChild();
    Node condBlock = varBlock.getNext();
    Node incBlock = condBlock.getNext();
    Node exprBlock = incBlock.getNext();
    MySubproduct out = MySubproduct.newBuffer();

    MySubproduct varNode = readNameOrRebuild(varBlock);
    // TODO: condNode could have no value
    MySubproduct condNode = readNameOrRebuild(condBlock);
    MySubproduct incNode = rebuild(incBlock);

    breakable.push(out);
    continueable.push(condNode.getFirst());
    MySubproduct exprNode = rebuild(exprBlock);
    continueable.pop();
    breakable.pop();

    out.setFirst(varNode.getFirst());
    varNode.connectLeafsTo(condNode);
    DiGraph.DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, condNode));
    condNode.connectLeafsTo(ifNode);
    connect(ifNode, MyFlowGraph.Branch.TRUE, exprNode.getFirst());
    out.addLeaf(ifNode, MyFlowGraph.Branch.FALSE);
    exprNode.connectLeafsTo(incNode);
    incNode.connectLeafsTo(condNode);

    return out;
  }

  private MySubproduct handleForIn(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node nameBlock = entry.getFirstChild();
    Node objBlock = nameBlock.getNext();
    Node exprBlock = objBlock.getNext();

    MySubproduct objNode = readNameOrRebuild(objBlock);
    MySubproduct iterVar = MySubproduct.newTemp();
    MySubproduct out = MySubproduct.newBuffer();

    breakable.push(out);

    // TODO: for (var bla-bla in bla) will fail here
    switch (nameBlock.getType()) {
      case Token.NAME:
        MySubproduct nameNode = MySubproduct.newVarName(nameBlock.getString());

        DiGraph.DiGraphNode forinNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.FOR_IN, iterVar, objNode));
        DiGraph.DiGraphNode writeNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_VARIABLE, iterVar, nameNode));

        continueable.push(forinNode);
        MySubproduct exprNode = rebuild(exprBlock);

        out.setFirst(objNode.getFirst());

        objNode.connectLeafsTo(forinNode);
        connect(forinNode, MyFlowGraph.Branch.TRUE, writeNode);
        out.addLeaf(forinNode, MyFlowGraph.Branch.FALSE);
        exprNode.connectToFirst(writeNode);
        exprNode.connectLeafsTo(forinNode);

        break;
      case Token.GETPROP:
      case Token.GETELEM:
        Node base = nameBlock.getFirstChild();
        Node index = base.getNext();

        MySubproduct dest = readNameOrRebuild(base);
        MySubproduct prop = readNameOrRebuild(index);

        forinNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.FOR_IN, iterVar, objNode));
        writeNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_PROPERTY, dest, prop, iterVar));

        continueable.push(forinNode);
        exprNode = rebuild(exprBlock);

        out.setFirst(dest.getFirst());
        dest.connectLeafsTo(prop);
        prop.connectLeafsTo(objNode);

        objNode.connectLeafsTo(forinNode);
        connect(forinNode, MyFlowGraph.Branch.TRUE, writeNode);
        out.addLeaf(forinNode, MyFlowGraph.Branch.FALSE);
        exprNode.connectToFirst(writeNode);
        exprNode.connectLeafsTo(forinNode);

        break;
      default:
        throw new UnexpectedNode(entry);
    }

    continueable.pop();
    breakable.pop();

    return out;
  }

  private MySubproduct handleAssign(Node entry) throws UnimplTransformEx, UnexpectedNode {
    Node childBlock = entry.getFirstChild();
    Node exprBlock = childBlock.getNext();

    MySubproduct dest = null;
    MySubproduct prop = null;
    MySubproduct out = MySubproduct.newBuffer();
    MySubproduct exprNode = readNameOrRebuild(exprBlock);

    switch (childBlock.getType()) {
      case Token.NAME:
        dest = MySubproduct.newVarName(childBlock.getString());

        out.setFirst(exprNode.getFirst());

        DiGraph.DiGraphNode writeNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_VARIABLE, exprNode, dest));
        exprNode.connectLeafsTo(writeNode);
        out.addLeaf(writeNode);

        break;
      case Token.GETPROP:
      case Token.GETELEM:
        Node base = childBlock.getFirstChild();
        Node index = base.getNext();
        dest = readNameOrRebuild(base);
        prop = readNameOrRebuild(index);
        dest.connectLeafsTo(prop);
        out.setFirst(dest.getFirst());
        writeNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.WRITE_PROPERTY, dest, prop, exprNode));
        prop.connectLeafsTo(exprNode);
        exprNode.connectLeafsTo(writeNode);
        out.addLeaf(writeNode);

        break;
      default:
        throw new UnexpectedNode(entry);
    }

    return out;
  }

  private MySubproduct handleExpression(Node entry) throws UnimplTransformEx, UnexpectedNode {
    if (entry.hasOneChild()) {
      return rebuild(entry.getFirstChild());
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


    while ((child = child.getNext()) != null) {
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
    continueable.push(condNode.getFirst());

    MySubproduct bodyNode = rebuild(bodyBlock);
    bodyNode.connectLeafsTo(condNode);
    DiGraph.DiGraphNode ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, condNode));
    condNode.connectLeafsTo(ifNode);

    connect(ifNode, MyFlowGraph.Branch.TRUE, bodyNode.getFirst());

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
    continueable.add(condNode.getFirst());

    DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> ifNode = flowGraph.createDirectedGraphNode(new MyNode(MyNode.Type.IF, condNode));
    condNode.connectLeafsTo(ifNode);

    MySubproduct bodyNode = rebuild(bodyBlock);
    bodyNode.connectLeafsTo(condNode);

    connect(ifNode, MyFlowGraph.Branch.TRUE, bodyNode.getFirst());
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

    connect(ifNode, MyFlowGraph.Branch.TRUE, thenNode.getFirst());

    if (elseBlock != null) {
      MySubproduct elseNode = rebuild(elseBlock);
      out.addLeaf(elseNode.getLeafs());
      connect(ifNode, MyFlowGraph.Branch.FALSE, elseNode.getFirst());
    } else {
      out.addLeaf(ifNode, MyFlowGraph.Branch.FALSE);
    }

    return out;
  }

  /**
   * Supporing function for rebuilding node that should result in
   * either program's variable name either temprorary variable name.
   * (e.g. if expBlock.getType() is NAME, result is name of temprorary variable,
   * containing value readed from program variable named expBlock.getString())
   *
   * @param expBlock node that should be processed
   * @return
   * @throws UnimplTransformEx
   * @throws UnexpectedNode
   */
  private MySubproduct readNameOrRebuild(Node expBlock) throws UnimplTransformEx, UnexpectedNode {
    MySubproduct value = rebuild(expBlock);
    MySubproduct out = null;

    if (value.getNodeRes().isVarName()) {
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

  private void connect(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> src,
                       MyFlowGraph.Branch branch, DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> dest) {
    flowGraph.connect(src.getValue(), branch, dest.getValue());
    return;
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
