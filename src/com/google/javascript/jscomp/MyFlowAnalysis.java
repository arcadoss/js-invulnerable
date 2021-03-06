/*
 * Copyright 2008 The Closure Compiler Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.javascript.jscomp;


import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.javascript.jscomp.MyFlowGraph.Branch;
import com.google.javascript.jscomp.NodeTraversal.AbstractPostOrderCallback;
import com.google.javascript.jscomp.Scope.Var;
import com.google.javascript.jscomp.graph.Annotation;
import com.google.javascript.jscomp.graph.DiGraph;
import com.google.javascript.jscomp.graph.DiGraph.DiGraphNode;
import com.google.javascript.rhino.Node;
import com.google.javascript.rhino.Token;

import java.util.*;

/**
 * A framework to help writing static program analysis. A subclass of
 * this framework should specify how a single node changes the state
 * of a program. This class finds a safe estimate (a fixed-point) for
 * the whole program. The proven facts about the program will be
 * annotated with
 * {@link com.google.javascript.jscomp.graph.GraphNode#setAnnotation} to the
 * given control flow graph's nodes in form of {@link com.google.javascript.jscomp.LatticeElement}
 * after calling {@link #analyze()}.
 * <p/>
 * <p>As a guideline, the following is a list of behaviors that any analysis
 * can take:
 * <ol>
 * <li>Flow Direction: Is the analysis a forward or backward analysis?
 * <li>Lattice Elements: How does the analysis represent the state of the
 * program at any given point?
 * <li>JOIN Operation: Given two incoming paths and a lattice state value, what
 * can the compiler conclude at the join point?
 * <li>Flow Equations: How does an instruction modify the state of program in
 * terms of lattice values?
 * <li>Initial Entry Value: What can the compiler assume at the beginning of the
 * program?
 * <li>Initial Estimate: What can the compiler assume at each point of the
 * program? (What is the BOTTOM value of the lattice) By definition this lattice
 * JOIN {@code x} for any {@code x} must also be {@code x}.
 * </ol>
 * To make these behaviors known to the framework, the following steps must be
 * taken.
 * <ol>
 * <li>Flow Direction: Implement {@link #isForward()}.
 * <li>Lattice Elements: Implement {@link com.google.javascript.jscomp.LatticeElement}.
 * <li>JOIN Operation: Implement
 * {@link com.google.javascript.jscomp.JoinOp#apply}.
 * <li>Flow Equations: Implement
 * {@link #flowThrough(MyNode, AnalyzerState)}.
 * <li>Initial Entry Value: Implement {@link #createEntryLattice()}.
 * <li>Initial Estimate: Implement {@link #createInitialEstimateLattice()}.
 * </ol>
 * <p/>
 * <p>Upon execution of the {@link #analyze()} method, nodes of the input
 * control flow graph will be annotated with a {@link FlowState} object that
 * represents maximum fixed point solution. Any previous annotations at the
 * nodes of the control flow graph will be lost.
 *
 * @param <L> Lattice element type.
 */
abstract class MyFlowAnalysis<L extends AnalyzerState> {

  private final MyFlowGraph cfg;
  final JoinOp<L> joinOp;
  final WhereOp<L> whereOp;

  protected final Set<DiGraphNode<MyNode, Branch>> orderedWorkSet;

  /*
   * Feel free to increase this to a reasonable number if you are finding that
   * more and more passes need more than 100000 steps before finding a
   * fixed-point. If you just have a special case, consider calling
   * {@link #analyse(int)} instead.
   */
  public static final int MAX_STEPS = 100000;

  /**
   * Constructs a data flow analysis.
   * <p/>
   * <p>Typical usage
   * <pre>
   * DataFlowAnalysis dfa = ...
   * dfa.analyze();
   * </pre>
   * <p/>
   * {@link #analyze()} annotates the result to the control flow graph by
   * means of {@link com.google.javascript.jscomp.graph.DiGraph.DiGraphNode#setAnnotation} without any
   * modification of the graph itself. Additional calls to {@link #analyze()}
   * recomputes the analysis which can be useful if the control flow graph
   * has been modified.
   *
   * @param targetCfg The control flow graph object that this object performs
   *                  on. Modification of the graph requires a separate call to
   *                  {@link #analyze()}.
   * @see #analyze()
   */
  MyFlowAnalysis(MyFlowGraph targetCfg, JoinOp<L> joinOp, WhereOp<L> whereOp) {
    this.cfg = targetCfg;
    this.joinOp = joinOp;
    this.whereOp = whereOp;
    Comparator<DiGraphNode<MyNode, Branch>> nodeComparator =
        cfg.getOptionalNodeComparator(isForward());
    if (nodeComparator != null) {
      this.orderedWorkSet = Sets.newTreeSet(nodeComparator);
    } else {
      this.orderedWorkSet = Sets.newLinkedHashSet();
    }
  }

  /**
   * Returns the control flow graph that this analysis was performed on.
   * Modifications can be done on this graph, however, the only time that the
   * annotations are correct is after {@link #analyze()} is called and before
   * the graph has been modified.
   */
  final MyFlowGraph getCfg() {
    return cfg;
  }

  /**
   * Returns the lattice element at the exit point.
   */
  L getExitLatticeElement() {
    DiGraphNode<MyNode, Branch> node = getCfg().getImplicitReturn();
    FlowState<L> state = node.getAnnotation();
    return state.getIn();
  }

  @SuppressWarnings("unchecked")
  protected L join(L latticeA, L latticeB) {
    return joinOp.apply(Lists.<L>newArrayList(latticeA, latticeB));
  }

  protected boolean shouldAdd(DiGraphNode<MyNode, Branch> from, DiGraphNode<MyNode, Branch> to, Branch value) {

    FlowState<L> state = from.getAnnotation();
    Set<Branch> possibleVal = whereOp.apply(state.getIn());

    if (possibleVal.contains(value) && to != cfg.getImplicitReturn()) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Checks whether the analysis is a forward flow analysis or backward flow
   * analysis.
   *
   * @return {@code true} if it is a forward analysis.
   */
  abstract boolean isForward();

  /**
   * Computes the output state for a given node and input state.
   *
   * @param node  The node.
   * @param input Input lattice that should be read-only.
   * @return Output lattice.
   */
  abstract L flowThrough(MyNode node, L input);

  /**
   * Finds a fixed-point solution using at most {@link #MAX_STEPS}
   * iterations.
   *
   * @see #analyze(int)
   */
  final void analyze() {
    analyze(MAX_STEPS);
  }

  /**
   * Finds a fixed-point solution. The function has the side effect of replacing
   * the existing node annotations with the computed solutions using {@link
   * com.google.javascript.jscomp.graph.GraphNode#setAnnotation(com.google.javascript.jscomp.graph.Annotation)}.
   * <p/>
   * <p>Initially, each node's input and output flow state contains the value
   * given by {@link #createInitialEstimateLattice()} (with the exception of the
   * entry node of the graph which takes on the {@link #createEntryLattice()}
   * value. Each node will use the output state of its predecessor and compute a
   * output state according to the instruction. At that time, any nodes that
   * depends on the node's newly modified output value will need to recompute
   * their output state again. Each step will perform a computation at one node
   * until no extra computation will modify any existing output state anymore.
   *
   * @param maxSteps Max number of iterations before the method stops and throw
   *                 a {@link MaxIterationsExceededException}. This will prevent the
   *                 analysis from going into a infinite loop.
   */
  final void analyze(int maxSteps) {
    initialize();
    int step = 0;
    while (!orderedWorkSet.isEmpty()) {
      if (step > maxSteps) {
        throw new MaxIterationsExceededException(
            "Analysis did not terminate after " + maxSteps + " iterations");
      }
      DiGraphNode<MyNode, Branch> curNode = orderedWorkSet.iterator().next();
      orderedWorkSet.remove(curNode);
      joinInputs(curNode);

      Preconditions.checkArgument(isForward(), "backward analysis wasn't implemented");

      if (flow(curNode)) {
        // If there is a change in the current node, we want to grab the list
        // of nodes that this node affects.

        List<DiGraph.DiGraphEdge<MyNode, Branch>> nextEdges;

        if (isForward()) {
          nextEdges = curNode.getOutEdges();


          for (DiGraph.DiGraphEdge<MyNode, Branch> edge : nextEdges) {
            if (shouldAdd(curNode, edge.getDestination(), edge.getValue())) {
              orderedWorkSet.add(edge.getDestination());
            }
          }
        } else {
          nextEdges = curNode.getInEdges();

          for (DiGraph.DiGraphEdge<MyNode, Branch> edge : nextEdges) {
            if (shouldAdd(curNode, edge.getSource(), edge.getValue())) {
              orderedWorkSet.add(edge.getSource());
            }
          }
        }
      }
      step++;
    }
    if (isForward()) {
      joinInputs(getCfg().getImplicitReturn());
    }
  }

  /**
   * Gets the state of the initial estimation at each node.
   *
   * @return Initial state.
   */
  abstract L createInitialEstimateLattice();

  /**
   * Gets the incoming state of the entry node.
   *
   * @return Entry state.
   */
  abstract L createEntryLattice();

  /**
   * Initializes the work list and the control flow graph.
   */
  protected void initialize() {
    // TODO(user): Calling clear doesn't deallocate the memory in a
    // LinkedHashSet. Consider creating a new work set if we plan to repeatly
    // call analyze.
    orderedWorkSet.clear();
    for (DiGraphNode<MyNode, Branch> node : cfg.getDirectedGraphNodes()) {
      node.setAnnotation(new FlowState<L>(createInitialEstimateLattice(),
          createInitialEstimateLattice()));
      if (node != cfg.getImplicitReturn()) {
        orderedWorkSet.add(node);
      }
    }
  }

  /**
   * Performs a single flow through a node.
   *
   * @return {@code true} if the flow state differs from the previous state.
   */
  protected boolean flow(DiGraphNode<MyNode, Branch> node) {
    FlowState<L> state = node.getAnnotation();
    if (isForward()) {
      L outBefore = state.out;
      state.out = flowThrough(node.getValue(), state.in);
      return !outBefore.equals(state.out);
    } else {
      L inBefore = state.in;
      state.in = flowThrough(node.getValue(), state.out);
      return !inBefore.equals(state.in);
    }
  }

  /**
   * Computes the new flow state at a given node's entry by merging the
   * output (input) lattice of the node's predecessor (successor).
   *
   * @param node Node to compute new join.
   */
  protected void joinInputs(DiGraphNode<MyNode, Branch> node) {
    FlowState<L> state = node.getAnnotation();
    if (isForward()) {
      if (cfg.getEntry() == node) {
        state.setIn(createEntryLattice());
      } else {
        List<DiGraph.DiGraphEdge<MyNode, Branch>> inEdges = node.getInEdges();
        if (inEdges.size() == 1) {
          DiGraph.DiGraphEdge<MyNode, Branch> edge = inEdges.get(0);
          Branch edgeVal = edge.getValue();
          DiGraphNode<MyNode, Branch> fromNode = edge.getDestination();

          if (shouldAdd(fromNode, node, edgeVal)) {
            FlowState<L> inNodeState = fromNode.getAnnotation();
            state.setIn(inNodeState.getOut());
          }
        } else if (inEdges.size() > 1) {
          List<L> values = new ArrayList<L>(inEdges.size());

          for (DiGraph.DiGraphEdge<MyNode, Branch> edge : inEdges) {
            Branch edgeVal = edge.getValue();
            DiGraphNode<MyNode, Branch> fromNode = edge.getDestination();

            if (shouldAdd(fromNode, node, edgeVal)) {
              FlowState<L> inNodeState = fromNode.getAnnotation();
              values.add(inNodeState.getOut());
            }
          }
          if (!values.isEmpty()) {
            state.setIn(joinOp.apply(values));
          }
        }
      }
    }
  }

  /**
   * The in and out states of a node.
   *
   * @param <L> Input and output lattice element type.
   */
  static class FlowState<L extends LatticeElement> implements Annotation {
    private L in;
    private L out;

    /**
     * Private constructor. No other classes should create new states.
     *
     * @param inState  Input.
     * @param outState Output.
     */
    private FlowState(L inState, L outState) {
      Preconditions.checkNotNull(inState);
      Preconditions.checkNotNull(outState);
      this.in = inState;
      this.out = outState;
    }

    L getIn() {
      return in;
    }

    void setIn(L in) {
      Preconditions.checkNotNull(in);
      this.in = in;
    }

    L getOut() {
      return out;
    }

    void setOut(L out) {
      Preconditions.checkNotNull(out);
      this.out = out;
    }

    @Override
    public String toString() {
      return String.format("IN: %s OUT: %s", in, out);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(in, out);
    }
  }

  /**
   * The exception to be thrown if the analysis has been running for a long
   * number of iterations. Chances are the analysis is not monotonic, a
   * fixed-point cannot be found and it is currently stuck in an infinite loop.
   */
  static class MaxIterationsExceededException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    MaxIterationsExceededException(String msg) {
      super(msg);
    }
  }
}
