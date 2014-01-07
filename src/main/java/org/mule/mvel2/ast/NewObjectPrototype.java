package org.mule.mvel2.ast;

import org.mule.mvel2.ParserContext;
import org.mule.mvel2.integration.VariableResolverFactory;
import org.mule.mvel2.integration.impl.MapVariableResolverFactory;

import java.util.HashMap;

/**
 * @author Mike Brock
 */
public class NewObjectPrototype extends ASTNode {
  private Function function;

  public NewObjectPrototype(ParserContext pCtx, Function function) {
    super(pCtx);
    this.function = function;
  }

  @Override
  public Object getReducedValue(Object ctx, Object thisValue, VariableResolverFactory factory) {
    final MapVariableResolverFactory resolverFactory = new MapVariableResolverFactory(new HashMap<String, Object>(), factory);
    function.getCompiledBlock().getValue(ctx, thisValue, resolverFactory);
    return new PrototypalFunctionInstance(function, resolverFactory);
  }

  @Override
  public Object getReducedValueAccelerated(Object ctx, Object thisValue, VariableResolverFactory factory) {
    return getReducedValue(ctx, thisValue, factory);
  }
}
