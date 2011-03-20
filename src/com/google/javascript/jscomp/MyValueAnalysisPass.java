package com.google.javascript.jscomp;

import com.google.common.collect.Lists;
import com.google.javascript.rhino.Node;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arcadoss
 * Date: 07.12.10
 * Time: 2:39
 * To change this template use File | Settings | File Templates.
 */
public class MyValueAnalysisPass implements CompilerPass {
  private final AbstractCompiler compiler;
  private List<Node> conditions = Lists.newLinkedList();

  public MyValueAnalysisPass(AbstractCompiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void process(Node externs, Node root) {

  }

  private class ValueAnalyzer extends MyFlowAnalysis<AnalyzerState> {
    ValueAnalyzer(MyFlowGraph targetCfg, JoinOp<AnalyzerState> analyzerStateJoinOp) {
      super(targetCfg, analyzerStateJoinOp);
    }

    @Override
    boolean isForward() {
      return true;
    }

    @Override
    AnalyzerState flowThrough(MyNode node, AnalyzerState input) {
      switch (node.getCommand()) {
        case IF:
          break;

      }
      return null;
    }

    @Override
    AnalyzerState createInitialEstimateLattice() {
      return null;
    }

    @Override
    AnalyzerState createEntryLattice() {
      return null;
    }
  }

  private class MyJoinOp extends JoinOp.BinaryJoinOp<AnalyzerState>
  {
    @Override
    AnalyzerState apply(AnalyzerState latticeA, AnalyzerState latticeB) {
      return null;
    }
  }

}
