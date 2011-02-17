package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.LinkedDirectedGraph;

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

  public static enum Branch {
    MY_UNCOND,
    MY_TRUE,
    MY_FALSE,
    MY_EXEPT,
    MY_CALL,
    MY_RETURN;
  }

}