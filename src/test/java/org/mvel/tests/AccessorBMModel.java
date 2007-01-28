package org.mvel.tests;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mvel.Accessor;
import org.mvel.ExecutableStatement;
import org.mvel.integration.VariableResolverFactory;
import org.mvel.util.FastList;


public class AccessorBMModel implements Accessor {
	private ExecutableStatement p0;
	private ExecutableStatement p1;
	private ExecutableStatement p2;
	private ExecutableStatement p3;
	
	
	public Object getValue(Object ctx, Object elCtx, VariableResolverFactory variableFactory) {
//          Object[] array = new Object[2];
//          
//          Object[] array2 = new Object[2];
//          array2[0] =  p0.getValue(elCtx, variableFactory);
//          array2[1] =  p1.getValue(elCtx, variableFactory);
//          array[0] = array2;
//          
//          Object[] array3 = new Object[2];
//          array3[0] = p2.getValue(elCtx, variableFactory);
//          array3[1] = p3.getValue(elCtx, variableFactory);
//          
//          array[1] = array3;
//
//
//          return array;
		
		 List list = new ArrayList(10);
		 list.add(p0.getValue(elCtx, variableFactory));
		 return list;
		
//		Map map = new HashMap();
//		map.put(p0.getValue(elCtx, variableFactory), p1.getValue(elCtx, variableFactory));
//		
//		return map;
    }
}
