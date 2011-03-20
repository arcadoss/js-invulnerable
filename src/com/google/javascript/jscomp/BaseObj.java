package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public interface BaseObj {
  <A extends BaseObj> A union(A rValue);
}
