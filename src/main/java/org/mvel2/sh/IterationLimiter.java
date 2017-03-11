package org.mvel2.sh;

import org.mvel2.ParserContext;

import com.sun.org.apache.xalan.internal.xsltc.compiler.Parser;

public class IterationLimiter {

	private Integer maxIterations;
	private int iterations = 0; 
	 
	public IterationLimiter( Object pCtx){
	    
	    if(pCtx != null &&  pCtx instanceof ParserContext){
	    	maxIterations = ((ParserContext)pCtx).getMaxLoopIterationsBeforeExit();
	    }
	    this.reset();
	}
	
	public void reset(){
		this.iterations = 0;
	}
	
	public void increment(){
		
		this.iterations++;
		if(maxIterations != null && iterations > maxIterations){
			throw new RuntimeException("Loop Iterations Count Exceeded");
		}
	}
	
}
