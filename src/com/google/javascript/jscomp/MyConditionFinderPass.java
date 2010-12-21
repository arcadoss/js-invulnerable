package com.google.javascript.jscomp;

import com.google.javascript.rhino.Node;

/**
 * Created by IntelliJ IDEA.
 * User: arcadoss
 * Date: 07.12.10
 * Time: 2:39
 * To change this template use File | Settings | File Templates.
 */
public class MyConditionFinderPass implements CompilerPass {
   private final AbstractCompiler compiler;

    public MyConditionFinderPass(AbstractCompiler compiler) {
        this.compiler = compiler;
    }

    @Override
    public void process(Node externs, Node root) {
        // Use the MemoizedScopeCreator instance from TypeCheck if available
        // as FunctionTypeBuilder warns about existing types if TypedScopeCreator is
        // ran a second time.
        ScopeCreator scopeCreator = compiler.getScopeCreator();
        if (scopeCreator == null) {
            // The TypedScopeCreator gives us correct handling of namespaces,
            // while the default NodeTraversal only gives us a
            // SyntacticScopeCreator.
            scopeCreator = new TypedScopeCreator(compiler);
        }

        NodeTraversal codeTraversal = new NodeTraversal(compiler,
                new Traversal(false), scopeCreator);


    }
}
