package cn.li.mcbase.entity;

import cn.li.mcbase.entity.spec.ScriptedEffectSpec;

import javax.annotation.Nullable;

/** Effect-entity surface shared by versioned ScriptedEffectEntity implementations. */
public interface IScriptedEffectEntity extends IScriptedOwnedEntity {
    @Nullable
    ScriptedEffectSpec getEffectSpec();
}
