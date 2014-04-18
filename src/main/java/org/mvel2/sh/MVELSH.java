package org.mvel2.sh;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jline.console.ConsoleReader;
import org.fusesource.jansi.Ansi;
import org.mvel2.CompileException;
import org.mvel2.MVEL;
import org.mvel2.ParserContext;
import org.mvel2.integration.VariableResolver;
import org.mvel2.integration.VariableResolverFactory;
import org.mvel2.integration.impl.ClassImportResolverFactory;
import org.mvel2.integration.impl.MapVariableResolverFactory;

import static java.lang.Character.isJavaIdentifierPart;
import static java.util.Arrays.asList;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.WHITE;
import static org.fusesource.jansi.Ansi.ansi;
import static org.mvel2.MVEL.analyze;
import static org.mvel2.MVEL.eval;
import static org.mvel2.compiler.AbstractParser.LITERALS;
import static org.mvel2.compiler.AbstractParser.OPERATORS;

public class MVELSH {
  private final ConsoleReader consoleReader;

  private final VariableResolverFactory shellVariableContext;
  private final StringBuilder commandBuffer = new StringBuilder();

  private MVELSH(VariableResolverFactory shellVariableContext) {
    this.shellVariableContext = shellVariableContext;
    try {
      consoleReader = new ConsoleReader(System.in, System.out);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    MVELSH.create().start();
  }

  public static MVELSH create() {
    return new MVELSH(new MapVariableResolverFactory());
  }

  public void start() {
    try {

      consoleReader.setPrompt(renderPrompt());
      consoleReader.setExpandEvents(false);
      consoleReader.addCompleter(this::complete);

      shellVariableContext.createVariable("__SHELL_VARIABLE_CONTEXT", shellVariableContext);

      for (BuiltinFunction builtinFunction : BuiltinFunction.values()) {
        eval(builtinFunction.functionDeclaration, shellVariableContext);
      }

      consoleReader.println("MVELSH 3.0 (C)2014.");

      String input;
      while ((input = consoleReader.readLine()) != null) {
        color(ansi().reset());
        processInput(input);
        consoleReader.setPrompt(renderPrompt());
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String renderPrompt() {
    BufferState bufferState = analyzeBufferState(commandBuffer);
    StringBuilder promptBuffer = new StringBuilder();
    if (bufferState.shouldDeferExecution()) {
      promptBuffer.append("> ");
      promptBuffer.append(createPadding(bufferState.codeBlockDepth * 2));
    } else if (bufferState.whitespaceOnly || !bufferState.hasCR) {
      promptBuffer.append("$ ");
    }
    return promptBuffer.toString();
  }

  private void processInput(String input) throws IOException {
    BufferState bufferState = analyzeBufferState(commandBuffer.append(input));
    if (!bufferState.whitespaceOnly && !bufferState.shouldDeferExecution()) {
      String expression = commandBuffer.toString();
      try {
        Class returnType = MVEL.analyze(expression, computeParserContext());
        Object eval = eval(expression, shellVariableContext);

        if (returnType != void.class) {
          consoleReader.println(ansi().fgBright(WHITE)
              .render(String.valueOf(eval)).reset().toString());
        }
      } catch (CompileException e) {
        consoleReader.println(ansi().fg(RED).render(e.getMessage()).reset().toString());
      }
      clearCommandBuffer();
    }
  }

  private String createPadding(int spaces) {
    char[] paddingChars = new char[spaces];
    for (int i = 0; i < spaces; i++) paddingChars[i] = ' ';
    return new String(paddingChars);
  }

  private void clearCommandBuffer() {
    commandBuffer.delete(0, commandBuffer.length());
  }

  public BufferState analyzeBufferState(StringBuilder inBuf) {
    char[] buffer = new char[inBuf.length()];
    inBuf.getChars(0, inBuf.length(), buffer, 0);
    boolean whitespaceOnly = true;
    boolean hasCR = false;
    int commentDepth = 0, codeBlockDepth = 0, expressionBlockDepth = 0;
    for (int i = 0; i < buffer.length; i++) {
      switch (buffer[i]) {
        case '/':
          if (i + 1 < buffer.length && buffer[i + 1] == '*') {
            commentDepth++;
          }
          break;
        case '*':
          if (i + 1 < buffer.length && buffer[i + 1] == '/') {
            commentDepth--;
          }
          break;

        case '(':
          expressionBlockDepth++;
          break;
        case ')':
          expressionBlockDepth--;
          break;
        case '{':
          codeBlockDepth++;
          break;
        case '}':
          codeBlockDepth--;
          break;
        case '\r':
        case '\n':
          hasCR = true;
          break;
        default:
          if (!Character.isWhitespace(buffer[i])) {
            whitespaceOnly = false;
          }
      }
    }

    return new BufferState(commentDepth, codeBlockDepth, expressionBlockDepth, hasCR,
        whitespaceOnly);
  }

  public int complete(String stringInput, int index, List<CharSequence> matchCandidates) {
    int endIndex = stringInput.lastIndexOf('.');
    if (endIndex == -1) {
      int i = stringInput.length();
      //noinspection StatementWithEmptyBody
      while (i > 0 && isJavaIdentifierPart(stringInput.charAt(i - 1))) --i;
      String match = stringInput.substring(i);

      shellVariableContext.getKnownVariables().stream()
          .filter((s) -> startsWithFuzzy(s, match)).forEach(matchCandidates::add);
      getDynamicImports().keySet().stream()
          .filter((s) -> startsWithFuzzy(s, match)).forEach(matchCandidates::add);
      LITERALS.keySet().stream()
          .filter((m) -> startsWithFuzzy(m, match)).forEach(matchCandidates::add);
      OPERATORS.keySet().stream()
          .filter((m) -> startsWithFuzzy(m, match)).forEach(matchCandidates::add);

      return i;
    }

    ParserContext localContext = computeParserContext();

    String matchRoot = stringInput.substring(0, endIndex + 1);
    Class outputType;
    try {
      outputType = analyze(stringInput.substring(0, endIndex), localContext);
    } catch (CompileException e) {
      return 0;
    }

    if (outputType != null) {
      if (outputType.isArray()) {
        String arrayLength = matchRoot + "length";
        if (startsWithFuzzy(arrayLength, stringInput)) {
          matchCandidates.add(arrayLength);
        }
      }

      if (outputType == Class.class) {
        outputType = (Class) eval(stringInput.substring(0, endIndex), shellVariableContext);
      }

      asList(outputType.getMethods()).stream().map((m) -> matchRoot + m.getName())
          .filter((m) -> startsWithFuzzyIgnoreCase(m, stringInput)).forEach(matchCandidates::add);
      asList(outputType.getFields()).stream().map((f) -> matchRoot + f.getName())
          .filter((m) -> startsWithFuzzyIgnoreCase(m, stringInput)).forEach(matchCandidates::add);
    }

    return 0;
  }

  public void color(Ansi ansi) {
    try {
      consoleReader.print(ansi.toString());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private ParserContext computeParserContext() {
    ParserContext parserContext = ParserContext.create();
    for (String varName : shellVariableContext.getKnownVariables()) {
      VariableResolver variableResolver = shellVariableContext.getVariableResolver(varName);
      Object value = variableResolver.getValue();
      if (value == null) continue;
      parserContext.addInput(varName, value.getClass());
    }

    getDynamicImports().entrySet().stream()
        .forEach(e -> parserContext.addImport(e.getKey(), e.getValue()));

    for (Map.Entry<String, Class> entry : getDynamicImports().entrySet()) {
      parserContext.addImport(entry.getKey(), entry.getValue());
    }

    return parserContext;
  }

  public static boolean startsWithFuzzyIgnoreCase(String target, String test) {
    return startsWithFuzzy(target.toLowerCase(), test.toLowerCase());
  }

  public static boolean startsWithFuzzy(String target, String test) {
    if (target.startsWith(test)) return true;

    char[] chars = test.toCharArray();
    if (chars.length < 2) return false;

    char tmp;
    tmp = chars[chars.length - 1];
    chars[chars.length - 1] = chars[chars.length - 2];
    chars[chars.length - 2] = tmp;

    return target.startsWith(new String(chars));
  }

  private Map<String, Class> getDynamicImports() {
    HashMap<String, Class> results = new HashMap<>();
    VariableResolverFactory factory = shellVariableContext;
    do {
      if (factory instanceof ClassImportResolverFactory) {
        ClassImportResolverFactory ciFactory = (ClassImportResolverFactory) factory;
        ciFactory.getDynamicImports().forEach((k, v) -> results.put(k, (Class) v));
      }
    } while ((factory = factory.getNextFactory()) != null);
    return results;
  }

  public static class BufferState {
    public final int codeCommentDepth;
    public final int codeBlockDepth;
    public final int expressionBlockDepth;
    public final boolean hasCR;
    public final boolean whitespaceOnly;

    public BufferState(
        int codeCommentDepth,
        int codeBlockDepth,
        int expressionBlockDepth,
        boolean hasCR, boolean whitespaceOnly) {
      this.codeCommentDepth = codeCommentDepth;
      this.codeBlockDepth = codeBlockDepth;
      this.expressionBlockDepth = expressionBlockDepth;
      this.hasCR = hasCR;
      this.whitespaceOnly = whitespaceOnly;
    }

    public boolean shouldDeferExecution() {
      return codeCommentDepth + codeBlockDepth + expressionBlockDepth != 0;
    }
  }
}
