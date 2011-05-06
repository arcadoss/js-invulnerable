package com.google.javascript.jscomp;

import com.google.javascript.jscomp.graph.Annotation;
import com.google.javascript.rhino.Node;

/**
 * @author arcadoss
 */
public class MyAnnotation implements Annotation {
  String jsCode;
  Node astNode;
}
