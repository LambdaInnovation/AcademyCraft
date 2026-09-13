package cn.li.mcbase.entity;

import cn.li.mcbase.entity.spec.ScriptedRaySpec;

import javax.annotation.Nullable;

/** Ray-entity surface shared by versioned ScriptedRayEntity implementations. */
public interface IScriptedRayEntity extends IScriptedOwnedEntity {
    @Nullable
    ScriptedRaySpec getRaySpec();
}
