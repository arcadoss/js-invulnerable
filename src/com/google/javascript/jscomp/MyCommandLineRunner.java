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

//        myCoolOptions.skipAllPasses = false;
//        myCoolOptions.nameAnonymousFunctionsOnly = false;
//        myCoolOptions.devMode = DevMode.OFF;
    myCoolOptions.checkSymbols = true;
    myCoolOptions.checkShadowVars = CheckLevel.WARNING;
    myCoolOptions.aggressiveVarCheck = CheckLevel.WARNING;
    myCoolOptions.checkFunctions = CheckLevel.WARNING;
    myCoolOptions.checkMethods = CheckLevel.WARNING;
//        myCoolOptions.checkDuplicateMessages = false;
//        myCoolOptions.allowLegacyJsMessages = false;
//        myCoolOptions.strictMessageReplacement = false;
    myCoolOptions.checkSuspiciousCode = true;
    myCoolOptions.checkControlStructures = true;
//        myCoolOptions.checkUndefinedProperties = CheckLevel.OFF;
//        myCoolOptions.checkUnusedPropertiesEarly = false;
    myCoolOptions.checkTypes = true;
    myCoolOptions.tightenTypes = true;
    myCoolOptions.inferTypesInGlobalScope = true;
    myCoolOptions.checkTypedPropertyCalls = true;
//        myCoolOptions.reportMissingOverride = CheckLevel.OFF;
    myCoolOptions.reportUnknownTypes = CheckLevel.WARNING;
//        myCoolOptions.checkRequires = CheckLevel.OFF;
//        myCoolOptions.checkProvides = CheckLevel.OFF;
//        myCoolOptions.checkGlobalNamesLevel = CheckLevel.OFF;
//        myCoolOptions.brokenClosureRequiresLevel = CheckLevel.ERROR;
//        myCoolOptions.checkGlobalThisLevel = CheckLevel.OFF;
    myCoolOptions.checkUnreachableCode = CheckLevel.WARNING;
//        myCoolOptions.checkMissingReturn = CheckLevel.OFF;
//        myCoolOptions.checkMissingGetCssNameLevel = CheckLevel.OFF;
//        myCoolOptions.checkMissingGetCssNameBlacklist = null;
//        myCoolOptions.checkEs5Strict = false;
//        myCoolOptions.checkCaja = false;
    myCoolOptions.computeFunctionSideEffects = true;
    myCoolOptions.chainCalls = true;


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
