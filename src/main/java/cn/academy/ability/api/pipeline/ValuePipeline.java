/**
 * 
 */
package cn.academy.ability.api.pipeline;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.academy.core.AcademyCraft;

/**
 * ValuePipeline receives value as input and returns the same type of value as output.
 * Each value is assigned with a key(channel), so they can be uniquely identified.
 * You can register PipelineListeners to the ValuePipeline and use @SubscribePipeline
 * to specify pipeline subscriber methods. Those methods will be called (in no particular order)
 *  when you pipe values into this ValuePipeline. They will modify the values and you will
 *  get the modified output.
 *  
 * @see SubscribePipeline
 * @see PipelineListener
 * @author WeAthFolD
 */
public class ValuePipeline {
	
	private enum Type { INT, FLOAT, DOUBLE };
	
	Map< String, List<SubscriberVisitor> > visitors = new HashMap();

	/**
	 * Register an PipelineListener into the pipeline.
	 */
	public void register(PipelineListener listener) {
		for(Method m : listener.getClass().getMethods()) {
			SubscribePipeline anno = m.getAnnotation(SubscribePipeline.class);
			if(anno != null) {
				String key = anno.value();
				List<SubscriberVisitor> list = visitors.get(key);
				if(list == null) {
					list = new ArrayList();
					visitors.put(key, list);
				}
				
				list.add(new SubscriberVisitor(m, listener));
			}
		}
	}
	
	/**
	 * Pipe an int value
	 * @return processed value
	 */
	public int pipeInt(String key, int value, Object ...parameters) {
		return (int) pipe(key, value, parameters);
	}
	
	/**
	 * Pipe a float value
	 * @return processed value
	 */
	public float pipeFloat(String key, float value, Object ...parameters) {
		return (float) pipe(key, value, parameters);
	}
	
	/**
	 * Pipe a double value
	 * @return processed value
	 */
	public double pipeDouble(String key, double value, Object ...parameters) {
		return (double) pipe(key, value, parameters);
	}
	
	private Object pipe(String key, Object value, Object ...pars) {
		List<SubscriberVisitor> list = visitors.get(key);
		if(list == null)
			return value;
		
		Object[] args = buildParArr(value, pars);
		for(SubscriberVisitor visitor : list) {
			if(visitor.object.isListenerActivated())
				value = visitor.pipe(args);
		}
		
		return value;
	}
	
	private Object[] buildParArr(Object a, Object ...bs) {
		Object[] ret = new Object[bs.length + 1];
		ret[0] = a;
		for(int i = 0; i < bs.length; ++i)
			ret[i + 1] = bs[i];
		return ret;
	}
	
	private class SubscriberVisitor {
		
		final Method theMethod;
		final PipelineListener object;
		
		public SubscriberVisitor(Method aMethod, PipelineListener _object) {
			theMethod = aMethod;
			object = _object;
			
			// Check the signature
			if(aMethod.getParameterCount() == 0)
				throw new IllegalArgumentException("Parcount must > 1");
			
			Class ttype = aMethod.getParameters()[0].getType();
			Class retType = aMethod.getReturnType();
			if(ttype != retType)
				throw new IllegalArgumentException("Inconsistent ret type and param type");
		}
		
		public Object pipe(Object ...args) {
			try {
				return theMethod.invoke(object, args);
			} catch(Exception e) {
				AcademyCraft.log.warn("Pipeline invocation failed on " + theMethod + ". Please check your implementation.");
				return args[0];
			}
		}
		
	}
	
}
