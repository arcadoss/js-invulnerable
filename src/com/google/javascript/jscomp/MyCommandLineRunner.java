package com.google.javascript.jscomp;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arcadoss
 * Date: 26.11.10
 * Time: 20:42
 * To change this template use File | Settings | File Templates.
 */
public class MyCommandLineRunner extends CommandLineRunner {
    public MyCommandLineRunner(String[] args) {
        super(args);

        CommandLineConfig commandLineConfig = getCommandLineConfig();

        commandLineConfig.setPrintAst(true);
    }

    @Override
    protected Compiler createCompiler() {
        Compiler compiler = new Compiler(getErrorPrintStream());
        PassConfig myPasses = new DefaultPassConfig(createOptions());

        compiler.setPassConfig(myPasses);
        return compiler;
    }

    @Override
    protected CompilerOptions createOptions() {
        CompilerOptions myCoolOptions = new CompilerOptions();

        myCoolOptions.ideMode = true;

        return myCoolOptions;
    }

    public static void main(String[] args) {
        MyCommandLineRunner runner = new MyCommandLineRunner(args);

        if (runner.shouldRunCompiler()) {
            runner.run();
        } else {
            System.exit(-1);
        }
    }

}
