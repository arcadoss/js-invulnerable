package com.google.javascript.jscomp;

import com.google.common.collect.Lists;
import com.google.javascript.rhino.Node;

import java.util.List;

/**
 * @author arcadoss
 */
public class MyValueAnalysisPass implements CompilerPass {
  private final AbstractCompiler compiler;
  private List<Node> conditions = Lists.newLinkedList();

  public MyValueAnalysisPass(AbstractCompiler compiler) {
    this.compiler = compiler;
  }

  @Override
  public void process(Node externs, Node root) {
    MyFlowGraphCreator creator = new MyFlowGraphCreator(compiler);
    creator.process(externs, root);
    MyValueAnalyzer analyzer = new MyValueAnalyzer(creator.getFlowGraph(), new MyJoinOp());
    analyzer.analyze();
  }

  private class MyJoinOp extends JoinOp.BinaryJoinOp<AnalyzerState> {
    @Override
    AnalyzerState apply(AnalyzerState latticeA, AnalyzerState latticeB) {
      return latticeA.union(latticeB);
    }
  }

}
