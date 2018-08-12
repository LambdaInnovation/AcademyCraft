package cn.academy.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * Entities that are only spawned in client and is not persistent. Used to reduce syntax overhead.
 * @author WeAthFolD
 */
public class LocalEntity extends Entity {

    public LocalEntity(World world) {
        super(world);
    }

    @Override
    protected void entityInit() {}

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) { }
}