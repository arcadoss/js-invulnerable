package com.google.javascript.jscomp;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.javascript.jscomp.AnalyzerState.*;

/**
 * @author arcadoss
 */
class MyValueAnalyzer extends MyFlowAnalysis<AnalyzerState> {
  AnalyzerMemory memory;

  MyValueAnalyzer(MyFlowGraph targetCfg, JoinOp<AnalyzerState> analyzerStateJoinOp, WhereOp<AnalyzerState> whereOp) {
    super(targetCfg, analyzerStateJoinOp, whereOp);
    memory = new AnalyzerMemory();
  }

  @Override
  boolean isForward() {
    return true;
  }

  @Override
  AnalyzerState flowThrough(MyNode node, AnalyzerState input) {
    switch (node.getCommand()) {
      case SKIP:
        return input;

      case DECLARE_VARIABLE:
        return handleDeclare(input, node.getOperands());

      case READ_VARIABLE:
        return handleReadVariable(input, node.getOperands());

      case WRITE_VARIABLE:
        return handleWriteVariable(input, node.getOperands());

      case CONSTANT:
        return handleConstant(input, node.getOperands());

      case READ_PROPERTY:
        return handleReadProperty(input, node.getOperands());

      case WRITE_PROPERTY:
        return handleWriteProperty(input, node.getOperands());

      case DELETE_PROPERTY:
        return handleDeleteProperty(input, node.getOperands());

      case IF:
        return handleIf(input, node.getOperands());

      case ENTRY:
        return handleEntry(input, node.getOperands());

      case EXIT:
        return handleExit(input, node.getOperands());

      case EXIT_EXC:
        return handleExitExc(input, node.getOperands());

      case CALL:
        return handleCall(input, node.getOperands());

      case AFTER_CALL:
        return handleAfterCall(input, node.getOperands());

      case CONSTRUCT:
        return handleConstruct(input, node.getOperands());

      case RETURN:
        return handleReturn(input, node.getOperands());

      case THROW:
        return handleThrow(input, node.getOperands());

      case CATCH:
        return handleCatch(input, node.getOperands());

      case FOR_IN:
        return handleForIn(input, node.getOperands());

      case WITH:
        return handleWith(input, node.getOperands());

      case BITOR:
        return handleBitor(input, node.getOperands());

      case BITXOR:
        return handleBitxor(input, node.getOperands());

      case BITAND:
        return handleBitand(input, node.getOperands());

      case AND:
        return handleAnd(input, node.getOperands());

      case OR:
        return handleOr(input, node.getOperands());

      case LSH:
        return handleLsh(input, node.getOperands());

      case RSH:
        return handleRsh(input, node.getOperands());

      case URSH:
        return handleUrsh(input, node.getOperands());

      case ADD:
        return handleAdd(input, node.getOperands());

      case SUB:
        return handleSub(input, node.getOperands());

      case MUL:
        return handleMul(input, node.getOperands());

      case DIV:
        return handleDiv(input, node.getOperands());

      case MOD:
        return handleMod(input, node.getOperands());

      case EQ:
        return handleEq(input, node.getOperands());

      case NE:
        return handleNe(input, node.getOperands());

      case SHNE:
        return handleShne(input, node.getOperands());

      case SHEQ:
        return handleSheq(input, node.getOperands());

      case LT:
        return handleLt(input, node.getOperands());

      case LE:
        return handleLe(input, node.getOperands());

      case GT:
        return handleGt(input, node.getOperands());

      case GE:
        return handleGe(input, node.getOperands());

      case IN:
        return handleIn(input, node.getOperands());

      case NEG:
        return handleNeg(input, node.getOperands());

      case POS:
        return handlePos(input, node.getOperands());

      case BITNOT:
        return handleBitnot(input, node.getOperands());

      case NOT:
        return handleNot(input, node.getOperands());

      case INSTANCEOF:
        return handleInstanceof(input, node.getOperands());

      case TYPEOF:
        return handleTypeof(input, node.getOperands());

      case INC:
        return handleInc(input, node.getOperands());

      case DEC:
        return handleDec(input, node.getOperands());

      case HOOK:
        return handleHook(input, node.getOperands());


      case PSEUDO_NODE:
      case PSEUDO_ROOT:
      case PSEUDO_EXIT:
        throw new IllegalStateException("Pseudo nodes meeted during flow traversal");
    }
    return null;
  }

  private AnalyzerState handleReadVariable(AnalyzerState input, List<MyValuable> operands) {
    AnalyzerState output = new AnalyzerState(input);

    String fromVar = (String) operands.get(0).getValue();
    String tempVar = (String) operands.get(1).getValue();

    Store heap = output.getStore();
    Stack stack = output.getStack();
    Set<ExecutionContext> runningContexts = stack.getContext();
    Map<String,Value> temp = stack.getTempValues();

    Value value = new Value();
    Value undefValue = Value.makeUndef();
    for (ExecutionContext context : runningContexts) {
      LinkedList<Label> chain = context.getScopeChain();

      boolean changed = false;
      for (Label hierarchyObject : chain) {
        if (heap.contains(hierarchyObject)) {
          changed = true;
          AbsObject obj = heap.get(hierarchyObject);
          value.union(obj.getValue(fromVar));
          break;
        }
      }

      if (!changed) {
        // it should be raised exception here, but i have no time
        value.union(undefValue);
      }
    }

    temp.put(tempVar, value);
    output.marker.toUncond();

    return output;
  }

  private AnalyzerState handleDeclare(AnalyzerState input, List<MyValuable> operands) {
    Preconditions.checkPositionIndex(0, operands.size());

    AnalyzerState output = new AnalyzerState(input);
    String varName = (String) operands.get(0).getValue();

    Label label = memory.createObject();
    AbsObject obj = memory.getObject(label);

    Stack stack = output.getStack();
    Store heap = output.getStore();
    Set<ExecutionContext> runningContexts = stack.getContext();

    for (ExecutionContext context : runningContexts) {
      Label varObjLabel = context.getVarObj();
      AbsObject varObj = memory.getObject(varObjLabel);
      Value val = Value.makeObj(label);
      Property props = new Property(val);

      varObj.put(varName, props);
    }

    return output;
  }

  private AnalyzerState handleAdd(AnalyzerState input, List<MyValuable> operands) {
    String lValName = (String) operands.get(0).getValue();
    String rValName = (String) operands.get(1).getValue();
    String resName = (String) operands.get(2).getValue();

    AnalyzerState output = new AnalyzerState(input);

    Map<String, Value> temp = output.getStack().getTempValues();

    Value lVal = temp.get(lValName);
    Value rVal = temp.get(rValName);

    temp.put(resName, Value.add(lVal, rVal));
    output.getMarker().toUncond();

    return output;
  }


  private AnalyzerState handleAfterCall(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleAnd(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleBitand(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleBitnot(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleBitor(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleBitxor(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleCall(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleCatch(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleConstant(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleConstruct(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleDec(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleDeleteProperty(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleDiv(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleEntry(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleEq(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleExit(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleExitExc(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleForIn(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleGe(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleGt(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleHook(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleIf(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleIn(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleInc(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleInstanceof(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleLe(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleLsh(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleLt(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleMod(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleMul(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleNe(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleNeg(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleNot(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleOr(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handlePos(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleReadProperty(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleReturn(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleRsh(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleSheq(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleShne(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleSub(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleThrow(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleTypeof(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleUrsh(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleWith(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleWriteProperty(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  private AnalyzerState handleWriteVariable(AnalyzerState input, List<MyValuable> operands) {
    return input;
  }


  @Override
  AnalyzerState createInitialEstimateLattice() {
    return AnalyzerState.bottom();
  }

  @Override
  AnalyzerState createEntryLattice() {
    return AnalyzerState.createGlobal();
  }
}
