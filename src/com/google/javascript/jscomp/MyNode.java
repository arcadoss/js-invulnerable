package com.google.javascript.jscomp;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: arcadoss
 * Date: 14.02.11
 * Time: 20:26
 * To change this template use File | Settings | File Templates.
 */
public class MyNode {
  Type command;
  List<MyAbsValue> operands;

  public MyNode(Type command) {
    this.command = command;
    this.operands = null;
  }

  public MyNode() {
    this.command = Type.MY_PSEUDO_NODE;
    this.operands = null;
  }

  public MyNode(Type command, List<MyAbsValue> operands) {
    this.command = command;
    this.operands = operands;
  }

  public MyNode(Type command, MyAbsValue... operands) {
    this.command = command;
    this.operands = new ArrayList<MyAbsValue>();
    for (MyAbsValue oper : operands) {
      this.operands.add(oper);
    }
  }

  public List<MyAbsValue> getOperands() {
    return operands;
  }

  public void setOperands(MyAbsValue[] arguments) {
    operands = new ArrayList<MyAbsValue>();

    for (MyAbsValue op : arguments) {
      operands.add(op);
    }
  }

  public Type getCommand() {
    return command;
  }

  public void setCommand(Type command) {
    this.command = command;
  }

  public static enum Type {
    // pseudo node
    MY_PSEUDO_NODE,

    // pseudo root
    MY_PSEUDO_ROOT,

    // pseudo exit
    MY_PSEUDO_EXIT,

    // (x) declares a program variable named x with value undefined.
    MY_DECLARE_VARIABLE,

    // (x, v) reads the value of a program variable named x into a temporary variable v.
    MY_READ_VARIABLE,

    // (v, x) writes the value of a temporary variable v into a program variable named x.
    MY_WRITE_VARIABLE,

    // (c, v) assigns a constant value c to the temporary variable v.
    MY_CONSTANT,

    // (v1, v2, v3) performs an object property lookup, where v1 holds the base object, v2 holds the property name, and v3 gets the resulting value.
    MY_READ_PROPERTY,

    // (v1, v2, v3) performs an object property write, where v1 holds the base object, v2 holds the property name, and v3 holds the value to be written.
    MY_WRITE_PROPERTY,

    // (v1, v2, v3) deletes an object property, where v1 holds the base object, v2 holds the property name, and v3 gets the resulting value.
    MY_DELETE_PROPERTY,

    // (v) represents conditional flow for e.g. if and while statements.
    MY_IF,

    // (f, x1, ... xn) used for marking the unique entry of a function body. Here, f is the (optional) function name, and x1 , . . . , xn are formal parameters.
    MY_ENTRY,

    // used for marking the unique normal exit of a function body.
    MY_EXIT,

    // used for marking the unique exceptional exit of a function body.
    MY_EXIT_EXC,

    // (w, v0, ... vn) A function call is represented by a pair of a call node and an after_call node. For a call node, wholds the function value and v0 , . . . , vn hold the values of this and the parameters.
    MY_CALL,

    // (v) An after_call node is returned to after the call and contains a single variable for the returned value.
    MY_AFTER_CALL,

    // (w, v0, ... vn) The construct nodes are similar to call nodes and are used for new expressions.
    MY_CONSTRUCT,

    // (v) a function return.
    MY_RETURN,

    // (v) represent throw statements of catch blocks.
    MY_THROW,

    // (x) represent entries of catch blocks.
    MY_CATCH,

    // represent for in loop
    MY_FOR_IN,

    // represent with statement
    MY_WITH,

    // represent binary operations
    MY_OP_BIN,

    // represent unary operations
    MY_OP_UNAR;

  }

}
