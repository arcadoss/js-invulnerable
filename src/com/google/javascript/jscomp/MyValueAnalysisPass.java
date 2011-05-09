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
  private MyFlowGraph flowGraph;

  public MyValueAnalysisPass(AbstractCompiler compiler) {
    this.compiler = compiler;
    this.flowGraph = null;
  }

  @Override
  public void process(Node externs, Node root) {
    MyFlowGraphCreator creator = new MyFlowGraphCreator(compiler);
    creator.process(externs, root);
    flowGraph = creator.getFlowGraph();
    MyValueAnalyzer analyzer = new MyValueAnalyzer(flowGraph, new MyJoinOp(), new WhereOp<AnalyzerState>());
    analyzer.analyze();
  }

  public MyFlowGraph getFlowGraph() {
    return flowGraph;
  }

  private class MyJoinOp extends JoinOp.BinaryJoinOp<AnalyzerState> {
    @Override
    AnalyzerState apply(AnalyzerState latticeA, AnalyzerState latticeB) {
      return latticeA.union(latticeB);
    }
  }

}
