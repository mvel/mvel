package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ExecutionStack;
import org.mvel2.util.ParseTools;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class Stacklang extends BlockNode {
  List<Instruction> instructionList;
  ParserContext pCtx;

  public Stacklang(char[] expr, int blockStart, int blockOffset, int fields,
                   ParserContext pCtx) {
    this.expr = expr;
    this.blockStart = blockStart;
    this.blockOffset = blockOffset;
    this.fields = fields | ASTNode.STACKLANG;


    String[] instructions = new String(expr, blockStart, blockOffset).split(";");

    instructionList = new ArrayList<Instruction>(instructions.length);
    for (String s : instructions) {
      instructionList.add(parseInstruction(s.trim()));
    }

    this.pCtx = pCtx;
  }

  @Override
  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    ExecutionStack stk = new ExecutionStack();
    stk.push(getReducedValue(stk, thisValue, factory));
    if (stk.isReduceable()) {
      while (true) {
        stk.op();
        if (stk.isReduceable()) {
          stk.xswap();
        }
        else {
          break;
        }
      }
    }
    return stk.peek();
  }

  @Override
  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    ExecutionStack stack = (ExecutionStack) ctx;
    Map<String, Integer> jumptable = null;

    for (int i1 = 0, instructionListSize = instructionList.size(); i1 < instructionListSize; i1++) {
      Instruction instruction = instructionList.get(i1);
      switch (instruction.opcode) {
        case Operator.STORE:
          stack.push(factory.createVariable(instruction.expr, stack.pop()).getValue());
          break;
        case Operator.LOAD:
          stack.push(factory.getVariableResolver(instruction.expr).getValue());
          break;
        case Operator.GETFIELD:
          try {
            if (stack.isEmpty() || !(stack.peek() instanceof Class)) {
              throw new CompileException("getfield without class", expr, blockStart);
            }

            stack.push(((Class) stack.pop()).getField(instruction.expr).get(stack.pop()));
          }
          catch (Exception e) {
            throw new CompileException("field access error", expr, blockStart, e);
          }
          break;
        case Operator.STOREFIELD:
          try {
            if (stack.isEmpty() || !(stack.peek() instanceof Class)) {
              throw new CompileException("storefield without class", expr, blockStart);
            }

            Class cls = (Class) stack.pop();
            Object val = stack.pop();
            cls.getField(instruction.expr).set(stack.pop(), val);
            stack.push(val);
          }
          catch (Exception e) {
            throw new CompileException("field access error", expr, blockStart, e);
          }
          break;

        case Operator.LDTYPE:
          try {
            stack.push(ParseTools.createClass(instruction.expr, pCtx));
          }
          catch (ClassNotFoundException e) {
            throw new CompileException("error", expr, blockStart, e);
          }
          break;

        case Operator.INVOKE:
          ExecutionStack call = new ExecutionStack();
          while (!stack.isEmpty() && !(stack.peek() instanceof Class)) {
            call.push(stack.pop());
          }
          if (stack.isEmpty()) {
            throw new CompileException("invoke without class", expr, blockStart);
          }

          Object[] parms = new Object[call.size()];

          for (int i = 0; !call.isEmpty(); i++) parms[i] = call.pop();


          if ("<init>".equals(instruction.expr)) {
            Constructor c = ParseTools.getBestConstructorCandidate(parms, (Class) stack.pop(), false);

            try {
              stack.push(c.newInstance(parms));
            }
            catch (Exception e) {
              throw new CompileException("instantiation error", expr, blockStart, e);
            }
          }
          else {
            Class cls = (Class) stack.pop();

            Method m = ParseTools.getBestCandidate(parms, instruction.expr, cls,
                cls.getDeclaredMethods(), false);

            try {
              stack.push(m.invoke(stack.isEmpty() ? null : stack.pop(), parms));
            }
            catch (Exception e) {
              throw new CompileException("invokation error", expr, blockStart, e);
            }
          }
          break;
        case Operator.PUSH:
          stack.push(MVEL.eval(instruction.expr, ctx, factory));
          break;
        case Operator.POP:
          stack.pop();
          break;
        case Operator.DUP:
          stack.push(stack.peek());
          break;
        case Operator.LABEL:
          if (jumptable == null) {
            jumptable = new HashMap<String, Integer>();
          }
          jumptable.put(instruction.expr, i1);
          break;
        case Operator.JUMPIF:
           if (!stack.popBoolean()) continue;

        case Operator.JUMP:
          if (jumptable != null && jumptable.containsKey(instruction.expr)) {
            i1 = jumptable.get(instruction.expr);
          }
          else {
            for (int i2 = i1 + 1; i2 < instructionList.size(); i2++) {
              if (instruction.expr.equals(instructionList.get(i2).expr)) {
                i1 = i2;
              }
            }
          }
          break;
        case Operator.EQUAL:
          stack.push(stack.pop().equals(stack.pop()));
          break;
        case Operator.NEQUAL:
          stack.push(!stack.pop().equals(stack.pop()));
          break;

        case Operator.REDUCE:
          stack.op();
          break;
      }
    }

    return stack.pop();
  }

  private static class Instruction {
    int opcode;
    String expr;
  }

  private static Instruction parseInstruction(String s) {
    int split = s.indexOf(' ');

    Instruction instruction = new Instruction();

    String keyword = split == -1 ? s : s.substring(0, split);

    if (opcodes.containsKey(keyword)) {
      instruction.opcode = opcodes.get(keyword);
    }

    //noinspection StringEquality
    if (keyword != s) {
      instruction.expr = s.substring(split + 1);
    }

    return instruction;
  }

  static final Map<String, Integer> opcodes = new HashMap<String, Integer>();

  static {
    opcodes.put("push", Operator.PUSH);
    opcodes.put("pop", Operator.POP);
    opcodes.put("load", Operator.LOAD);
    opcodes.put("ldtype", Operator.LDTYPE);
    opcodes.put("invoke", Operator.INVOKE);
    opcodes.put("store", Operator.STORE);
    opcodes.put("getfield", Operator.GETFIELD);
    opcodes.put("storefield", Operator.STOREFIELD);
    opcodes.put("dup", Operator.DUP);
    opcodes.put("jump", Operator.JUMP);
    opcodes.put("jumpif", Operator.JUMPIF);
    opcodes.put("label", Operator.LABEL);
    opcodes.put("eq", Operator.EQUAL);
    opcodes.put("ne", Operator.NEQUAL);
    opcodes.put("reduce", Operator.REDUCE);
  }
}
