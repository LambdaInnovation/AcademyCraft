package cn.academy.vanilla.meltdowner.entity;

import cn.academy.vanilla.meltdowner.client.render.RenderDiamondShield;
import cn.lambdalib2.registry.mc.RegEntity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@RegEntity
public class EntityDiamondShield extends EntityAdvanced {

    public static RenderDiamondShield renderer;
    
    public static final float SIZE = 1.8f;
    
    final EntityPlayer player;

    public EntityDiamondShield(EntityPlayer _player) {
        super(_player.getEntityWorld());
        player = _player;
        this.setSize(SIZE, SIZE);
    }
    
    @Override
    public void onUpdate() {
        super.onUpdate();
        
        Motion3D mo = new Motion3D(player, true).move(1);
        mo.py -= 0.5;
        setPosition(mo.px, mo.py, mo.pz);
        
        this.rotationYaw = player.rotationYawHead;
        this.rotationPitch = player.rotationPitch;
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
