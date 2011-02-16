package com.google.javascript.jscomp;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.javascript.rhino.Node;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arcadoss
 * Date: 06.12.10
 * Time: 12:45
 * To change this template use File | Settings | File Templates.
 */
public class MyPassConfig extends PassConfig {
    private PassFactory inferTypes = new PassFactory("inferTypes", false) {
        @Override
        protected CompilerPass createInternal(final AbstractCompiler compiler) {
            return new CompilerPass() {
                @Override
                public void process(Node externs, Node root) {
                    Preconditions.checkNotNull(topScope);
                    Preconditions.checkNotNull(typedScopeCreator);

                    makeTypeInference(compiler).process(externs, root);
                }
            };
        }
    };
    private PassFactory resolveTypes = new PassFactory("resolveTypes", false) {
        @Override
        protected CompilerPass createInternal(AbstractCompiler compiler) {
            return new GlobalTypeResolver(compiler);
        }
    };

    public MyPassConfig(CompilerOptions options) {
        super(options);
    }

    @Override
    protected List<PassFactory> getChecks() {
        List<PassFactory> checks = Lists.newArrayList();

        checks.add(inferTypes.makeOneTimePass());
        checks.add(resolveTypes.makeOneTimePass());

        return checks;
    }

    /**
     * We don't want ot optimize code. We are trying to analyze it.
     *
     * @return null
     */
    @Override
    protected List<PassFactory> getOptimizations() {
        return null;
    }

    @Override
    State getIntermediateState() {
        return null;
    }

    @Override
    void setIntermediateState(State state) {

    }

    private class GlobalTypeResolver implements CompilerPass {
        private final AbstractCompiler compiler;

        GlobalTypeResolver(AbstractCompiler compiler) {
            this.compiler = compiler;
        }

        @Override
        public void process(Node externs, Node root) {
            if (topScope == null) {
                typedScopeCreator =
                        new MemoizedScopeCreator(new TypedScopeCreator(compiler));
                topScope = typedScopeCreator.createScope(root.getParent(), null);
            } else {
                compiler.getTypeRegistry().resolveTypesInScope(topScope);
            }
        }
    }
}
