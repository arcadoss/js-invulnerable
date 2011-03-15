package com.google.javascript.jscomp;

import java.io.IOException;
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
  List<MyValuable> operands;

  @Override
  public String toString() {
    Appendable builder = new StringBuilder();

    try {
      builder.append(command.toString());
      if (!operands.isEmpty()) {
        builder.append("(");
        for (MyValuable oper : operands) {
          builder.append(oper.toString());
        }
        builder.append(")");
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    return builder.toString();
  }

  public MyNode(Type command) {
    this.command = command;
    this.operands = null;
  }

  public MyNode() {
    this.command = Type.PSEUDO_NODE;
    this.operands = null;
  }

  public MyNode(Type command, List<MyValuable> operands) {
    this.command = command;
    this.operands = operands;
  }

  public MyNode(Type command, MySubproduct... operands) {
    this.command = command;
    this.operands = new ArrayList<MyValuable>();
    for (MySubproduct oper : operands) {
      this.operands.add(oper);
    }
  }

  public List<MyValuable> getOperands() {
    return operands;
  }

  public void setOperands(MySubproduct[] arguments) {
    operands = new ArrayList<MyValuable>();

    for (MySubproduct op : arguments) {
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
    // not a note, should be removed during the second traversal
    NAN,

    // pseudo node
    PSEUDO_NODE,

    // pseudo root
    PSEUDO_ROOT,

    // pseudo exit
    PSEUDO_EXIT,

    // (x) declares a program variable named x with value undefined.
    DECLARE_VARIABLE,

    // (x, v) reads the value of a program variable named x into a temporary variable v.
    READ_VARIABLE,

    // (v, x) writes the value of a temporary variable v into a program variable named x.
    WRITE_VARIABLE,

    // (c, v) assigns a constant value c to the temporary variable v.
    CONSTANT,

    // (v1, v2, v3) performs an object property lookup, where v1 holds the base object, v2 holds the property name, and v3 gets the resulting value.
    READ_PROPERTY,

    // (v1, v2, v3) performs an object property write, where v1 holds the base object, v2 holds the property name, and v3 holds the value to be written.
    WRITE_PROPERTY,

    // (v1, v2, v3) deletes an object property, where v1 holds the base object, v2 holds the property name, and v3 gets the resulting value.
    DELETE_PROPERTY,

    // (v) represents conditional flow for e.g. if and while statements.
    IF,

    // (f, x1, ... xn) used for marking the unique entry of a function body. Here, f is the (optional) function name, and x1 , . . . , xn are formal parameters.
    ENTRY,

    // used for marking the unique normal exit of a function body.
    EXIT,

    // used for marking the unique exceptional exit of a function body.
    EXIT_EXC,

    // (w, v0, ... vn) A function call is represented by a pair of a call node and an after_call node. For a call node, wholds the function value and v0 , . . . , vn hold the values of this and the parameters.
    CALL,

    // (v) An after_call node is returned to after the call and contains a single variable for the returned value.
    AFTER_CALL,

    // (w, v0, ... vn) The construct nodes are similar to call nodes and are used for new expressions.
    CONSTRUCT,

    // (v) a function return.
    RETURN,

    // (v) represent throw statements of catch blocks.
    THROW,

    // (x) represent entries of catch blocks.
    CATCH,

    // (v1, v2) represent for in loop. Here v1 contains base object, v2 is temprorary variable for iterating
    FOR_IN,

    // (v) represent with statement
    WITH,

    // represent binary operations
    // (v1, v2, v3) v3 = v1 BIN_OP v2
    BITOR, BITXOR, BITAND,
    AND, OR,
    LSH, RSH, URSH, ADD, SUB, MUL, DIV, MOD,
    EQ, NE,
    SHNE, SHEQ,
    LT, LE, GT, GE,

    // represent unary operations
    // (v1, v2) v2 = UNAR_OP v1
    NEG, POS, BITNOT, NOT, INSTANCEOF, HOOK, TYPEOF;

  }

}
