package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.LinkedDirectedGraph;

/**
 * User: arcadoss
 * Date: 14.02.11
 * Time: 20:11
 */
public class MyFlowGraph<T, N> extends LinkedDirectedGraph<T, N> {
//public class MyFlowGraph extends LinkedDirectedGraph<MyNode, MyFlowGraph.Branch> {
//public class MyFlowGraph extends LinkedDirectedGraph {
  public MyFlowGraph() {
    super(false, true);
  }

  protected MyFlowGraph(boolean useNodeAnnotations, boolean useEdgeAnnotations) {
    super(useNodeAnnotations, useEdgeAnnotations);
  }

  public static enum Branch {
    /** analyzer will always traverse throught this node */
    UNCOND,
    /** analyzer will never traverse throught this node */
    NEVER,
    /** analyzer will traverse throught this node when 'IF' node's result was true*/
    TRUE,
    /** analyzer will traverse throught this node when 'IF' node's result was false*/
    FALSE,
    /** analyzer will traverse throught this node when exception occured*/
    EXEPT,
    /** analyzer will traverse throught this node when function was called*/
    CALL,
    /** analyzer will traverse throught this node when returning from function*/
    RETURN;
  }

}