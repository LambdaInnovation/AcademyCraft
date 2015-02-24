/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.energy.block.tile.impl;

import cn.academy.api.energy.IWirelessNode;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegTileEntity;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.World;

/**
 * 风能发电机TileEntity
 * 
 * @author WeAthFolD
 *
 */
@RegistrationClass
@RegTileEntity
public class TileWindGenerator extends TileEntity implements IWirelessNode {
    
    /* Const Declaration for This General Generator. */
    private double currentEU = 0;
    private int until = 0;
    
    private final double MAX_EU = 25000.0;
    private final double LATENCY = 200.0;
    private final double MAX_DISTANCE = 6; /* Unit: Block */
    private final int RATE = 128; /* Ticks */
    
    /**
     * Those API stuff.
     */
    public TileWindGenerator() {
        super();
    }
    
    @Override
    public void setEnergy(double value) {
        this.currentEU = value;
        
    }
    
    @Override
    public double getMaxEnergy() {
        return MAX_EU;
    }
    
    @Override
    public double getEnergy() {
        return currentEU;
    }
    
    @Override
    public double getLatency() {
        return LATENCY;
    }
    
    @Override
    public double getTransDistance() {
        return MAX_DISTANCE;
    }
    
    public void addEnergy(double toAdd) {
        if(this.currentEU + toAdd < MAX_EU)
            this.currentEU += toAdd;
        else
            this.currentEU = MAX_EU;
    }
    
    @Override
	@SideOnly(Side.CLIENT)
    public AxisAlignedBB getRenderBoundingBox()
    {
        return INFINITE_EXTENT_AABB;
    }
    
    private static float getWeather(World world) {
        if (world.isThundering()) {
            return 1.5F;
        }
        if (world.isRaining()) {
            return 1.2F;
        }
        return 1.0F;
    }
    
    @Override
    public void updateEntity() {
        super.updateEntity();
        
        if(--until != 0)
            return;
        else
            until = RATE;
        
        World theWorld = this.getWorldObj();
        int h = theWorld.getHeight();
        int totalFree = 0;
        for(int i=-3; i<=3; i++)
            for(int j=-3; j<=3; j++)
                for(int k=-3; k<=3; k++) {
                    boolean hasBlock = theWorld.blockExists(xCoord+i, yCoord+j, zCoord+k);
                    if(!hasBlock) totalFree++;
                }
        double EUToAdd = 0;
        /* Free blocks ranged from 0 ~ 216 */
        if(totalFree <= 30) // Lots of blocks nearby
            EUToAdd = 40;
        if(totalFree <= 60) // Many blocks nearby
            EUToAdd = 80;
        if(totalFree <= 120) // Much free blocks nearby
            EUToAdd = 100;
        if(totalFree <= 216) // Free blocks
            EUToAdd = 180;
        
        float weatherPower = TileWindGenerator.getWeather(theWorld);
        this.addEnergy(EUToAdd * weatherPower);
    				
        
    }
    

}
