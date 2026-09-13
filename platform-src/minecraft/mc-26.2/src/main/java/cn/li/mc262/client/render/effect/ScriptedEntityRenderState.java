package cn.li.mc262.client.render.effect;

import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.world.entity.Entity;

import java.util.Collections;
import java.util.List;

/** Frame-local data extracted from a scripted entity for render submission. */
public final class ScriptedEntityRenderState<T extends Entity> extends EntityRenderState {
    public T entity;
    public float partialTick;
    public float yRot;
    public float xRot;
    public int entityId;
    public int ageTicks;
    public String rendererId = "";
    public String rendererKey = "";
    public int lifeTicks = 15;
    public List<ScriptedRenderAccess.ArcDataView> activeArcs = Collections.emptyList();
    public final BlockModelRenderState blockModel = new BlockModelRenderState();
    public boolean behaviorHit;
}
