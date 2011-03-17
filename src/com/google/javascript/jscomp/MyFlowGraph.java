package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;
import com.google.javascript.jscomp.graph.GraphvizGraph;
import com.google.javascript.jscomp.graph.LinkedDirectedGraph;

import java.util.Comparator;

/**
 * User: arcadoss
 * Date: 14.02.11
 * Time: 20:11
 */
public class MyFlowGraph extends LinkedDirectedGraph<MyNode, MyFlowGraph.Branch> {
  private DiGraphNode<MyNode, Branch> implicitReturn;
  private DiGraphNode<MyNode, Branch> entry;

  public MyFlowGraph() {
    super(false, true);
  }

  protected MyFlowGraph(boolean useNodeAnnotations, boolean useEdgeAnnotations) {
    super(useNodeAnnotations, useEdgeAnnotations);
  }

  @Override
  public String getName() {
    return "MyFG";
  }

  public DiGraphNode<MyNode,Branch> getImplicitReturn() {
    return implicitReturn;
  }

  public DiGraphNode<MyNode,Branch> getEntry() {
    return entry;
  }

  public Comparator<DiGraphNode<MyNode, Branch>> getOptionalNodeComparator(boolean forward) {
    return null;
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