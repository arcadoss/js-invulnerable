package com.google.javascript.jscomp;

/**
* @author arcadoss
*/
class MyValueAnalyzer extends MyFlowAnalysis<AnalyzerState> {
  MyValueAnalyzer(MyFlowGraph targetCfg, JoinOp<AnalyzerState> analyzerStateJoinOp, WhereOp<AnalyzerState> whereOp) {
    super(targetCfg, analyzerStateJoinOp, whereOp);
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
        break;

      case READ_VARIABLE:
        break;

      case WRITE_VARIABLE:
        break;

      case CONSTANT:
        break;

      case READ_PROPERTY:
        break;

      case WRITE_PROPERTY:
        break;

      case DELETE_PROPERTY:
        break;

      case IF:
        break;

      case ENTRY:
        break;

      case EXIT:
        break;

      case EXIT_EXC:
        break;

      case CALL:
        break;

      case AFTER_CALL:
        break;

      case CONSTRUCT:
        break;

      case RETURN:
        break;

      case THROW:
        break;

      case CATCH:
        break;

      case FOR_IN:
        break;

      case WITH:
        break;

      case BITOR:
      case BITXOR:
      case BITAND:
      case AND:
      case OR:
      case LSH:
      case RSH:
      case URSH:
      case ADD:
      case SUB:
      case MUL:
      case DIV:
      case MOD:
      case EQ:
      case NE:
      case SHNE:
      case SHEQ:
      case LT:
      case LE:
      case GT:
      case GE:
      case IN:
      case NEG:
      case POS:
      case BITNOT:
      case NOT:
      case INSTANCEOF:
      case TYPEOF:
      case INC:
      case DEC:
        break;

      case HOOK:
        break;

      case PSEUDO_NODE:
      case PSEUDO_ROOT:
      case PSEUDO_EXIT:
        throw new IllegalStateException("Pseudo nodes meeted during flow traversal");
    }
    return null;
  }

  @Override
  AnalyzerState createInitialEstimateLattice() {
    return AnalyzerState.bottom() ;
  }

  @Override
  AnalyzerState createEntryLattice() {
    return AnalyzerState.createGlobal();
  }
}
