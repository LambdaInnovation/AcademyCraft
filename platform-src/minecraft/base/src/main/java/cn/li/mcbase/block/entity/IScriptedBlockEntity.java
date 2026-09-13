package cn.li.mcbase.block.entity;

/**
 * Version-neutral scripted block-entity surface implemented by each version's
 * AbstractScriptedBlockEntity.
 */
public interface IScriptedBlockEntity {
    Object getCustomState();

    void setCustomState(Object state);
}
