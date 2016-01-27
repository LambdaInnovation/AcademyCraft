/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.block.TileGeneratorBase;
import cn.academy.energy.client.render.block.RenderCatEngine;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegTileEntity;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.RangedTarget;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Cat Engine!
 * @author WeAthFolD
 */
@Registrant
@RegTileEntity
@RegTileEntity.HasRender
public class TileCatEngine extends TileGeneratorBase {
    
    @SideOnly(Side.CLIENT)
    @RegTileEntity.Render
    public static RenderCatEngine renderer;
    
    // Sync
    int syncTicker;
    
    // Intrusive render parameters
    public double thisTickGen;
    public double rotation;
    public long lastRender;

    public TileCatEngine() {
        super("infinite_generator", 0, 2000, 200);
    }

    @Override
    public void updateEntity() {
        super.updateEntity();
        
        if(!getWorldObj().isRemote) {
            if(++syncTicker == 20) {
                syncTicker = 0;
                syncGen(this, thisTickGen);
            }
        }
    }
    
    @Override
    public double getGeneration(double required) {
        return (thisTickGen = Math.min(required, 500));
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    private static void syncGen(@RangedTarget(range = 10) TileCatEngine te, @Data Double amt) {
        te.thisTickGen = amt;
    }

}