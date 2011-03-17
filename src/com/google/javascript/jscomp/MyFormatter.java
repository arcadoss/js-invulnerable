package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.DiGraph;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import com.google.javascript.jscomp.MyNode.Type;

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
    this.builder = new StringBuilder();
    this.graph = graph;
    this.assignments = new HashMap<DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch>, Integer>();
    this.keyCount = 0;
  }

  public String toDot() throws IOException {
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
      List<DiGraph.DiGraphEdge<MyNode, MyFlowGraph.Branch>> outEdges = node.getOutEdges();
      String source = formatNodeName(getKeyAndAppend(node));

      for (DiGraph.DiGraphEdge<MyNode, MyFlowGraph.Branch> edge : outEdges) {
        DiGraph.DiGraphNode<MyNode, MyFlowGraph.Branch> destNode = edge.getDestination();
        String dest = formatNodeName(getKeyAndAppend(destNode));

        builder.append(INDENT);
        builder.append(source);
        builder.append(ARROW);
        builder.append(dest);
        builder.append(" [label=\"");
        if (edge.getValue() != MyFlowGraph.Branch.UNCOND) {
          builder.append(edge.getValue().toString());
        }
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
      builder.append(color(node.getValue()));
      builder.append("];\n");

//    builder.append("\" color=\"white\"];\n");
    }
    return key;
  }

  String color(MyNode node) {
    final String preamble = "\" color=";
    final String afterword = ", style=filled";
    String color;

    switch (node.getCommand()) {
      case PSEUDO_EXIT:
      case PSEUDO_ROOT:
      case EXIT_EXC:
        color = "navajowhite1";
        break;
      case EXIT:
      case ENTRY:
        color = "lightcoral";
        break;
      case IF:
        color = "darkolivegreen1";
        break;
      case CALL:
      case AFTER_CALL:
        color = "violet";
        break;
      case CONSTRUCT:
        color = "plum1";
        break;
      default:
        return "\"";
    }

    return preamble + color + afterword;
  }

}
