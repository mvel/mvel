package org.mvel2.ast;

import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.Operator;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.util.ExecutionStack;
import org.mvel2.util.Make;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mike Brock <cbrock@redhat.com>
 */
public class Stacklang extends BlockNode {
  List<Instruction> instructionList;

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
      instruction.expr = s.substring(split+1);
    }

    return instruction;
  }

  static final Map<String, Integer> opcodes = new HashMap<String, Integer>();

  static {
    opcodes.put("push", Operator.PUSH);
    opcodes.put("pop", Operator.POP);
    opcodes.put("load", Operator.LOAD);
  }
}
