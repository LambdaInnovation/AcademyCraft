package cn.academy.entity;

import cn.academy.client.render.entity.RippleMarkRender;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityRippleMark extends EntityAdvanced
{
    
    @RegEntity.Render
    public static RippleMarkRender renderer;
    
    public final Color color = Color.white();
    public final double creationTime = GameTimer.getTime();

    public EntityRippleMark(World world) {
        super(world);
        setSize(2, 2);
    }
    
    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 1;
    }
    
    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {}

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {}

}