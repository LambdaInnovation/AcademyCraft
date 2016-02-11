package cn.academy.core.config;

import cn.lambdalib.util.generic.MathUtils;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Floats;
import com.typesafe.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConfigEnv {

    public static ConfigEnv global = new ConfigEnv();
    public static ConfigEnv create() {
        ConfigEnv ret = new ConfigEnv();
        ret.setParent(global, "");
        return ret;
    }

    public interface FloatPipe {
        float apply(float value);
    }

    private class Pipe<T> {
        final Predicate<String> pathTester;
        final T pipe;

        Pipe(Predicate<String> tester, T _pipe) {
            pathTester = tester;
            pipe = _pipe;
        }
    }

    public static Predicate<String> path(String path) {
        return input -> input.equals(path);
    }

    public static Predicate<String> regex(String regex) {
        Pattern p = Pattern.compile(regex);
        return input -> {
            Matcher matcher = p.matcher(regex);
            return matcher.matches();
        };
    }

    private final Config config;

    private ConfigEnv parent;
    private String pipePrefix;

    private final List<Pipe<FloatPipe>> floatPipes = new ArrayList<>();

    private final LoadingCache<String, List<FloatPipe>> floatPipeCache =
            CacheBuilder.newBuilder().maximumSize(100).build(new CacheLoader<String, List<FloatPipe>>() {
                @Override
                public List<FloatPipe> load(String key) throws Exception {
                    List<FloatPipe> ret = new ArrayList<>();
                    for (Pipe<FloatPipe> pipe : floatPipes) {
                        if (pipe.pathTester.test(key)) {
                            ret.add(pipe.pipe);
                        }
                    }
                    return ret;
                }
            });

    ConfigEnv() {
        this(ACConfig.getRawConfig());
    }

    ConfigEnv(Config custom) {
        config = custom;
    }

    void setParent(ConfigEnv _parent, String _prefix) {
        parent = _parent;
        pipePrefix = _prefix;
    }

    public float lerpf(String path, float progress) {
        List<Double> list = config.getDoubleList(path);

        return pipeFloat(path, (float) MathUtils.lerp(list.get(0), list.get(1), progress));
    }

    public float getFloat(String path) {
        return pipeFloat(path, (float) config.getDouble(path));
    }

    public boolean getBoolean(String path) {
        return config.getBoolean(path);
    }

    public int getInt(String path) {
        return config.getInt(path);
    }

    public float[] getFloatArray(String path) {
        return Floats.toArray(config.getDoubleList(path));
    }

    public ConfigEnv getEnv(String path) {
        ConfigEnv ret = new ConfigEnv(config.getConfig(path));

        ret.setParent(this, path);

        return ret;
    }

    public void addFloatPipe(Predicate<String> pathSelector, FloatPipe pipe) {
        floatPipes.add(new Pipe<>(pathSelector, pipe));
    }

    public float pipeFloat(String path, float initial) {
        if (parent != null) {
            initial = parent.pipeFloat(pipePrefix + path, initial);
        }
        for (FloatPipe pipe : floatPipeCache.getUnchecked(path)) {
            initial = pipe.apply(initial);
        }
        return initial;
    }

}
