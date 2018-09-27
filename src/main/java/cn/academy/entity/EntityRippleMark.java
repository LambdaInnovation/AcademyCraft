package cn.academy.entity;

import cn.academy.client.render.entity.RippleMarkRender;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.util.Colors;
import cn.lambdalib2.util.GameTimer;
import cn.lambdalib2.util.entityx.EntityAdvanced;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.Color;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class EntityRippleMark extends EntityAdvanced
{

    public final Color color = Colors.white();
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