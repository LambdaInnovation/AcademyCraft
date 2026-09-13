package cn.li.mcbase.entity;

/** Block-body projectile surface shared across MC versions. */
public interface IScriptedBlockBodyEntity {
    void setSyncedBlockId(String blockId);

    void setPlaceWhenCollide(boolean placeWhenCollide);
}
