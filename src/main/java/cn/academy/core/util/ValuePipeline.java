/**
 * 
 */
package cn.academy.core.util;

import cn.academy.core.AcademyCraft;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ValuePipeline receives value as input and returns the same type of value as output.
 * Each value is assigned with a key(channel), so they can be uniquely identified.
 * You can register any Object to the ValuePipeline and use @SubscribePipeline
 * to specify pipeline subscriber methods. Those methods will be called (in no particular order)
 *  when you pipe values into this ValuePipeline. They will modify the values and you will
 *  get the modified output. <br>
 * 
 * The key must be specified acording the following rule: <br>
 *     <code>[ns1].[ns2]. ... . [key]</code> <br>
 * each keyword in '[' and ']' must only consists of alphabetic letters or '_' or '-' or numbers.
 * 
 * And thus the Listener can use wildcard matching on keys using other characters. 
 * this allows them to perform operations on more than one key. <br>
 * 
 * You are expected to register all the listeners before 
 * any value is piped so we can cache for efficiency.
 * 
 * @see SubscribePipeline
 * @author WeAthFolD
 */
public class ValuePipeline {
    
    /*
     * Current supported wildcard(s):
     * 
     * ? -- one of any keyword. 
     *     e.g. "electromaster.?.consumption" matches "electromaster.arcgen.consumption", "electromaster.railgun.consumption" and so on.
     * 
     * $<keyword> -- contains the following keyword.
     *  e.g. "lol.$233" matches "lol.233", "lol.23333", "lol.44423333" and so on.
     */
    
    private enum Type { INT, FLOAT, DOUBLE };
    
    List<SubscriberVisitor> visitors = new ArrayList();
    Map< String, List<SubscriberVisitor> > cachedVisitors = new HashMap();
    
    private interface RuleNode {
        boolean accepts(String keyword);
    }
    
    private class NodeKeyword implements RuleNode {
        final String kwd;
        
        public NodeKeyword(String _kwd) {
            checkKeyword(_kwd);
            kwd = _kwd;
        }

        @Override
        public boolean accepts(String keyword) {
            return keyword.equals(kwd);
        }
    }
    
    private class NodeAny implements RuleNode {

        @Override
        public boolean accepts(String keyword) {
            return true;
        }
        
    }
    
    private class NodeContains implements RuleNode {
        final String kwd;
        
        public NodeContains(String _kwd) {
            checkKeyword(_kwd);
            kwd = _kwd;
        }
        
        @Override
        public boolean accepts(String keyword) {
            return keyword.contains(kwd);
        }
    }
    
    private class Rule {
        
        List<RuleNode> rules = new ArrayList();
        String _str;
        
        public Rule(String str) {
            // Parse the str in the ctor
            String[] strs = split(str);
            for(String s : strs) {
                if(s.equals("?")) {
                    rules.add(new NodeAny());
                } else if(s.charAt(0) == '$'){
                    rules.add(new NodeContains(s.substring(1)));
                } else {
                    rules.add(new NodeKeyword(s));
                }
            }
            _str = str;
        }
        
        public boolean matches(String[] input) {
            if(rules.size() != input.length)
                return false;
            for(int i = 0; i < input.length; ++i) {
                if(!rules.get(i).accepts(input[i]))
                    return false;
            }
            return true;
        }
    }
    
    private void checkKeyword(String kwd) {
        for(int i = 0; i < kwd.length(); ++i) {
            char ch = kwd.charAt(i);
            if(!Character.isAlphabetic(ch) && 
                !Character.isDigit(ch) && ch 
                != '-'  && ch != '_')
                throw new RuntimeException("Invalid keyword " + kwd + ": '" + ch + "' is not a valid character");
        }
    }
    
    public void register(Object listener) {
        for(Method m : listener.getClass().getMethods()) {
            SubscribePipeline anno = m.getAnnotation(SubscribePipeline.class);
            if(anno != null) {
                visitors.add(new SubscriberVisitor(m, listener, new Rule(anno.value())));
            }
        }
    }
    
    private String[] split(String str) {
        List<String> ret = new ArrayList();
        
        int last = 0;
        for(int i = 0; i < str.length(); ++i) {
            char ch = str.charAt(i);
            if(ch == '.' || i == str.length() - 1) {
                if(i == str.length() - 1) i += 1;
                if(i == last) {
                    throw new RuntimeException("Invalid pattern");
                }
                ret.add(str.substring(last, i));
                last = i + 1;
            }
        }
        
        return ret.toArray(new String[ret.size()]);
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
        List<SubscriberVisitor> list = cachedVisitors.get(key);
        if(list == null) { // build the cache
            cachedVisitors.put(key, list = new ArrayList());
            
            String[] kwds = split(key);
            for(String s : kwds) 
                checkKeyword(s);
            
            for(SubscriberVisitor sv : visitors) {
                if(sv.rule.matches(kwds))
                    list.add(sv);
            }
            
//            System.out.println("Build list for " + key + ", visitors: {");
//            for(SubscriberVisitor v : list) {
//                System.out.println("\t" + v.object.getClass().getName() + "/" + v.theMethod.getName());
//            }
//            System.out.println("}");
        }
        
        if(list.size() == 0)
            return value;
        
        //System.out.print("['" + key + "' " + value + " ");
        Object[] args = buildParArr(value, pars);
        for(SubscriberVisitor visitor : list) {
            value = visitor.pipe(args);
            //System.out.print("-(" + visitor.theMethod.getName() + ")>" + value + " ");
        }
        //System.out.print("]\n");
        
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
        final Object object;
        final Rule rule;
        
        public SubscriberVisitor(Method aMethod, Object _object, Rule _rule) {
            theMethod = aMethod;
            object = _object;
            rule = _rule;
            
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
