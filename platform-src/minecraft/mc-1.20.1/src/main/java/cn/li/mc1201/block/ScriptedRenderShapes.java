package cn.li.mc1201.block;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Keyword;
import clojure.lang.RT;
import cn.li.mcbase.clj.ClojureInterop;
import net.minecraft.world.level.block.RenderShape;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Shared scripted-block render-shape fallback policy.
 * Content-specific render shape metadata must be supplied by content descriptors.
 */
public final class ScriptedRenderShapes {

    private static final ConcurrentMap<String, RenderShape> SHAPE_CACHE = new ConcurrentHashMap<>();
    private static final Keyword RENDERING_KEY = Keyword.intern(null, "rendering");
    private static final Keyword RENDER_SHAPE_KEY = Keyword.intern(null, "render-shape");

    private ScriptedRenderShapes() {
    }

    public static RenderShape resolveDefault(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return RenderShape.MODEL;
        }
        return SHAPE_CACHE.computeIfAbsent(blockId, ScriptedRenderShapes::resolveFromMetadata);
    }

    private static RenderShape resolveFromMetadata(String blockId) {
        try {
            ClojureInterop.requireNamespace("cn.li.mcmod.protocol.metadata");

            IFn getBlockSpec = Clojure.var("cn.li.mcmod.protocol.metadata", "get-block-spec");
            Object blockSpec = getBlockSpec.invoke(blockId);
            if (blockSpec == null) {
                return RenderShape.MODEL;
            }

            Object rendering = RT.get(blockSpec, RENDERING_KEY);
            Object renderShape = RT.get(rendering, RENDER_SHAPE_KEY);
            return parseRenderShape(renderShape);
        } catch (Throwable ignored) {
            return RenderShape.MODEL;
        }
    }

    private static RenderShape parseRenderShape(Object renderShape) {
        if (renderShape == null) {
            return RenderShape.MODEL;
        }

        String raw = (renderShape instanceof Keyword kw) ? kw.getName() : String.valueOf(renderShape);
        String normalized = raw.trim().toLowerCase(Locale.ROOT).replace('_', '-');

        return switch (normalized) {
            case "invisible" -> RenderShape.INVISIBLE;
            case "entityblock-animated", "entityblockanimated" -> RenderShape.ENTITYBLOCK_ANIMATED;
            case "model" -> RenderShape.MODEL;
            default -> RenderShape.MODEL;
        };
    }
}
