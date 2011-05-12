package com.google.javascript.jscomp;

/**
 * @author arcadoss
 */
public interface MyLatticeElement<A extends MyLatticeElement> {
  A union(A rValue);
  boolean lessThen(A rValue);
}
