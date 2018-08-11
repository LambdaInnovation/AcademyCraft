package cn.academy.vanilla.teleporter.entity;

import cn.academy.vanilla.teleporter.client.RenderMarker;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityMarker extends EntityAdvanced
{

    public static RenderMarker renderer;

    public Entity target = null;
    public Color color = Color.white();
    public boolean ignoreDepth = false;

    public EntityMarker(Entity entity) {
        this(entity.getEntityWorld());
        setPosition(entity.posX, entity.posY, entity.posZ);
        setSize(0.5f, 0.5f);
        target = entity;
    }

    public EntityMarker(World world) {
        super(world);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        if (target != null)
            setPosition(target.posX, target.posY, target.posZ);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        setDead();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
    }

}