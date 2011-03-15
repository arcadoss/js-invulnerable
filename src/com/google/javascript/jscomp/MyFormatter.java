package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
* @author arcadoss
*/
public class MyFormatter {
  private static final String INDENT = "  ";
  private static final String ARROW = " -> ";

  MyFlowGraph graph;

  HashMap<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>, Integer> assignments;
  int keyCount;
  Appendable builder;

  public MyFormatter(MyFlowGraph graph) {
    this.graph = graph;
    this.assignments = new HashMap<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>, Integer>();
    this.keyCount = 0;
  }

  public String toDot() throws IOException {
    StringBuilder builder = new StringBuilder();
    builder.append("digraph");
    builder.append(INDENT);
    builder.append(graph.getName());
    builder.append(" {\n");
    builder.append(INDENT);
    builder.append("node [color=lightblue2, style=filled];\n");

    Iterable<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>> nodes = graph.getDirectedGraphNodes();

    for (DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node : nodes) {
      getKeyAndAppend(node);
    }

    for (DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node : nodes) {
      List<DiGraph.DiGraphEdge<MyNode,MyFlowGraph.Branch>> outEdges = node.getOutEdges();
      String source = formatNodeName(getKeyAndAppend(node));

      for (DiGraph.DiGraphEdge<MyNode,MyFlowGraph.Branch> edge : outEdges) {
        DiGraph.DiGraphNode<MyNode,MyFlowGraph.Branch> destNode = edge.getDestination();
        String dest = formatNodeName(getKeyAndAppend(destNode));

        builder.append(INDENT);
        builder.append(source);
        builder.append(ARROW);
        builder.append(dest);
        builder.append(" [label=\"");
        builder.append(edge.getValue().toString());
        builder.append("\"];\n");

      }
    }

    builder.append("}\n");

    return builder.toString();
  }

  private String formatNodeName(int key) {
    return "node" + key;
  }

  int getKeyAndAppend(DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> node) throws IOException {
  Integer key = assignments.get(node);
  if (key == null) {
    key = keyCount++;
    assignments.put(node, key);
    builder.append(INDENT);
    builder.append(formatNodeName(key));
    builder.append(" [label=\"");
    builder.append(node.getValue().toString());
    builder.append("\" color=\"white\"];\n");
  }
  return key;
}

}
