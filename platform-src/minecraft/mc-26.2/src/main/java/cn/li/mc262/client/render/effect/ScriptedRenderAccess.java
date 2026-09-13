package cn.li.mc262.client.render.effect;

import cn.li.mcbase.entity.ScriptedEntitySpecAccess;
import cn.li.mc262.entity.ScriptedBlockBodyEntity;
import cn.li.mc262.entity.ScriptedEffectEntity;
import cn.li.mcbase.entity.spec.ScriptedEffectSpec;
import cn.li.mcbase.entity.spec.ScriptedMarkerSpec;
import cn.li.mcbase.entity.spec.ScriptedRaySpec;
import net.minecraft.world.entity.Entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class ScriptedRenderAccess {

    private ScriptedRenderAccess() {
    }

    static ScriptedEffectSpec getEffectSpec(Entity entity) {
        return ScriptedEntitySpecAccess.getScriptedEffectSpec(entity.getType());
    }

    static ScriptedMarkerSpec getMarkerSpec(Entity entity) {
        return ScriptedEntitySpecAccess.getScriptedMarkerSpec(entity.getType());
    }

    static ScriptedRaySpec getRaySpec(Entity entity) {
        return ScriptedEntitySpecAccess.getScriptedRaySpec(entity.getType());
    }

    static int getAgeTicks(Entity entity) {
        return entity instanceof ScriptedEffectEntity effect ? effect.getAgeTicks() : entity.tickCount;
    }

    static String getSyncedBlockId(Entity entity) {
        return entity instanceof ScriptedBlockBodyEntity body
                ? body.getSyncedBlockId()
                : "minecraft:stone";
    }

    static boolean isBehaviorHit(Entity entity) {
        return entity instanceof ScriptedBlockBodyEntity body && body.isBehaviorHit();
    }

    static List<ArcDataView> getActiveArcs(Entity entity) {
        if (!(entity instanceof ScriptedEffectEntity effect)) {
            return Collections.emptyList();
        }
        List<ScriptedEffectEntity.ArcData> arcs = effect.getActiveArcs();
        if (arcs.isEmpty()) {
            return Collections.emptyList();
        }
        List<ArcDataView> out = new ArrayList<>(arcs.size());
        for (ScriptedEffectEntity.ArcData arc : arcs) {
            out.add(ArcDataView.from(arc));
        }
        return List.copyOf(out);
    }

    static final class ArcDataView {
        final float[][][] strands;
        final int lifeTicks;
        final float phase;
        final float flickerSeed;

        private ArcDataView(float[][][] strands, int lifeTicks, float phase, float flickerSeed) {
            this.strands = strands;
            this.lifeTicks = lifeTicks;
            this.phase = phase;
            this.flickerSeed = flickerSeed;
        }

        static ArcDataView from(ScriptedEffectEntity.ArcData arc) {
            float[][][] strands = new float[arc.strands.length][][];
            for (int i = 0; i < arc.strands.length; i++) {
                float[][] source = arc.strands[i];
                strands[i] = new float[source.length][];
                for (int j = 0; j < source.length; j++) {
                    strands[i][j] = source[j].clone();
                }
            }
            return new ArcDataView(strands, arc.lifeTicks, arc.phase, arc.flickerSeed);
        }
    }
}
