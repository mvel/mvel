package org.mvel2.ast;

import com.sun.xml.internal.ws.wsdl.parser.ParserUtil;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ExecutionStack;
import org.mvel2.util.Make;
import org.mvel2.util.ParseTools;

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
    return getReducedValue(ctx, thisValue, factory);
  }

  @Override
  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    ExecutionStack stack = (ExecutionStack) ctx;

    for (Instruction instruction : instructionList) {
      switch (instruction.opcode) {
        case Operator.LOAD:
          stack.push(factory.getVariableResolver(instruction.expr).getValue());
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

          Class cls = (Class) stack.pop();
          Object[] parms = new Object[call.size()];
          for (int i = 0; !call.isEmpty(); i++) parms[i] = call.pop();

          Method m = ParseTools.getBestCandidate(parms, instruction.expr, cls, cls.getDeclaredMethods(), false);

          try {
            stack.push(m.invoke(stack.pop(), parms));
          }
          catch (Exception e) {
            throw new CompileException("invokation error", expr, blockStart, e);
          }
          break;
        case Operator.PUSH:
          stack.push(MVEL.eval(instruction.expr, ctx, factory));
          break;
        case Operator.POP:
          stack.pop();
      }
    }

    return stack.pop();
  }

  private static class Instruction {
    int opcode;
    String expr;
  }

  private Instruction parseInstruction(String s) {
    int split = s.indexOf(' ');

    Instruction instruction = new Instruction();

    String keyword = split == -1 ? s : s.substring(0, split);

    if (opcodes.containsKey(keyword)) {
      instruction.opcode = opcodes.get(keyword);
    }

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
  }
}
