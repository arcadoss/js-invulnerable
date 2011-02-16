package com.google.javascript.jscomp;

import com.google.common.collect.Lists;
import com.google.javascript.rhino.Node;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arcadoss
 * Date: 07.12.10
 * Time: 2:39
 * To change this template use File | Settings | File Templates.
 */
public class MyConditionFinderPass implements CompilerPass {
  private final AbstractCompiler compiler;
  private List<Node> conditions = Lists.newLinkedList();

  public MyConditionFinderPass(AbstractCompiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void process(Node externs, Node root) {
    // Use the MemoizedScopeCreator instance from TypeCheck if available
    // as FunctionTypeBuilder warns about existing types if TypedScopeCreator is
    // ran a second time.
    ScopeCreator scopeCreator = compiler.getScopeCreator();
    if (scopeCreator == null) {
      // The TypedScopeCreator gives us correct handling of namespaces,
      // while the default NodeTraversal only gives us a
      // SyntacticScopeCreator.
      scopeCreator = new TypedScopeCreator(compiler);
    }

//        NodeTraversal codeTraversal = new NodeTraversal(compiler,
//                new ConditionFace(), scopeCreator);
//
//        codeTraversal.traverse(root);

  }

  private class ConditionFinder extends DataFlowAnalysis<Node, MyLatticeElement> {
    private ConditionFinder(ControlFlowGraph<Node> targetCfg, MyJoinOp myLatticeElemJoinOp) {
      super(targetCfg, myLatticeElemJoinOp);
    }

    @Override
    boolean isForward() {
      return false;
    }

    @Override
    MyLatticeElement flowThrough(Node node, MyLatticeElement input) {
      return null;
    }

    @Override
    MyLatticeElement createInitialEstimateLattice() {
      return new MyLatticeElement(false);
    }

    @Override
    MyLatticeElement createEntryLattice() {
      return new MyLatticeElement(false);
    }
  }

  private class MyLatticeElement implements LatticeElement {
    private MyLatticeElement(boolean mayAffect) {
      this.mayAffect = mayAffect;
    }

    boolean mayAffect;
  }

  private class MyJoinOp extends JoinOp.BinaryJoinOp<MyLatticeElement>
  {
    @Override
    MyLatticeElement apply(MyLatticeElement latticeA, MyLatticeElement latticeB) {
      MyLatticeElement element = new MyLatticeElement(latticeA.mayAffect || latticeB.mayAffect);
      return element;
    }
  }

  private class ConditionFace implements NodeTraversal.Callback {
    @Override
    public boolean shouldTraverse(NodeTraversal nodeTraversal, Node n, Node parent) {
      return true;
    }

    @Override
    public void visit(NodeTraversal t, Node n, Node parent) {
    }
  }

  public List<Node> getConditions() {
    return conditions;
  }
}
