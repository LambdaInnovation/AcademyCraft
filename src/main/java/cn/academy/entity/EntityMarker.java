package cn.academy.entity;

import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.Color;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class EntityMarker extends EntityAdvanced
{

    public Entity target = null;
    public Color color = Colors.white();
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