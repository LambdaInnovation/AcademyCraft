/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block.wind;

import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import cn.academy.core.block.TileGeneratorBase;
import cn.academy.energy.IFConstants;
import cn.academy.energy.ModuleEnergy;
import cn.academy.energy.api.IFItemManager;
import cn.academy.energy.client.render.block.RenderWindGenBase;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cn.lambdalib.multiblock.BlockMulti;
import cn.lambdalib.multiblock.IMultiTile;
import cn.lambdalib.multiblock.InfoBlockMulti;
import cn.lambdalib.util.generic.MathUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileWindGenBase extends TileGeneratorBase implements IMultiTile {
    
    public static double MAX_GENERATION_SPEED = 15;
    
    private static final IFItemManager itemManager = IFItemManager.instance;
    
    @SideOnly(Side.CLIENT)
    @RegTileEntity.Render
    public static RenderWindGenBase renderer;
    
    // CLIENT STATES
    public TileWindGenMain mainTile;
    public boolean complete;
    public boolean noObstacle;
    public int untilUpdate;
    
    public TileWindGenBase() {
        super("windgen_base", 1, 20000, IFConstants.LATENCY_MK3);
    }

    @Override
    public double getGeneration(double required) {
        double sim = getSimulatedGeneration();
        return Math.min(required, sim);
    }
    
    // TODO: Improve the fomula?
    public double getSimulatedGeneration() {
        if(complete && noObstacle) {
            int y = mainTile.yCoord;
            double heightFactor = MathUtils.lerp(0.5, 1, 
                MathUtils.clampd(0, 1, (y - 70.0) / 90.0));
            return heightFactor * MAX_GENERATION_SPEED;
        } else {
            return 0.0;
        }
    }
    
    private void updateChargeOut() {
        ItemStack stack = this.getStackInSlot(0);
        if(stack != null && itemManager.isSupported(stack)) {
            double cur = getEnergy();
            if(cur > 0) {
                cur = Math.min(getBandwidth(), cur);
                double left = itemManager.charge(stack, cur);
                
                this.setEnergy(getEnergy() - (cur - left));
            }
        }
    }
    
    // InfoBlockMulti delegates
    InfoBlockMulti info = new InfoBlockMulti(this);
    
    @Override
    public void updateEntity() {
        super.updateEntity();
        info.update();
        
        if(++untilUpdate == 10) {
            untilUpdate = 0;
            mainTile = findMainTile();
            complete = mainTile != null;
            noObstacle = (mainTile != null && mainTile.noObstacle);
        }
        
        updateChargeOut();
    }
    
    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        info = new InfoBlockMulti(this, tag);
    }
    
    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        info.save(tag);
    }

    @Override
    public InfoBlockMulti getBlockInfo() {
        return info;
    }

    @Override
    public void setBlockInfo(InfoBlockMulti i) {
        info = i;
    }
    
    @SideOnly(Side.CLIENT)
    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        Block block = getBlockType();
        if(block instanceof BlockMulti) {
            return ((BlockMulti) block).getRenderBB(xCoord, yCoord, zCoord, info.getDir());
        } else {
            return super.getRenderBoundingBox();
        }
    }

    public boolean isCompleteStructure() {
        return findMainTile() != null;
    }
    
    private TileWindGenMain findMainTile() {
        int state = 1;
        int pillars = 0;
        
        for(int y = yCoord + 2; state < 2; ++y) {
            TileEntity te = worldObj.getTileEntity(xCoord, y, zCoord);
            Block block = worldObj.getBlock(xCoord, y, zCoord);
            if(state == 1) {
                if(block == ModuleEnergy.windgenPillar) {
                    ++pillars;
                    if(pillars > WindGenerator.MAX_PILLARS)
                        break;
                } else if(te instanceof TileWindGenMain) {
                    TileWindGenMain gen = (TileWindGenMain) te;
                    if(gen.getBlockInfo().getSubID() == 0) {
                        return pillars >= WindGenerator.MIN_PILLARS ? gen : null;
                    } else {
                        break;
                    }
                } else {
                    state = 3;
                    break;
                }
            }
        }
        
        return null;
    }
}
