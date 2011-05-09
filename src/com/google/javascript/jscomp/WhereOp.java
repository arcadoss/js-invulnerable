package com.google.javascript.jscomp;
import com.google.common.base.Function;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * @author arcadoss
 */
public class WhereOp<L extends AnalyzerState> implements Function<L, Set<MyFlowGraph.Branch>> {

  @Override
  public Set<MyFlowGraph.Branch> apply(@Nullable L analyzerState) {
    return analyzerState.getMarker().getMarkers();
  }

  @Override
  public boolean equals(@Nullable Object o) {
    return false;
  }
}
