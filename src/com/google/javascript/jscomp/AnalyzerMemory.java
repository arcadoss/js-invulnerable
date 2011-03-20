package com.google.javascript.jscomp;

import java.util.ArrayList;
import com.google.javascript.jscomp.AnalyzerState.AbsObject;

/**
 * @author arcadoss
 */
public class AnalyzerMemory {
  private final ArrayList<AbsObject> memory;
  private final int memorySize = 5000;
  private int lastUsed;

  public AnalyzerMemory() {
    this.memory = new ArrayList<AbsObject>(memorySize);
    this.lastUsed = 0;
  }

  public int createObject() {
    memory.add(lastUsed++, new AbsObject());
    return lastUsed;
  }

  public AbsObject getObject(int label) {
    return memory.get(label);
  }
}
