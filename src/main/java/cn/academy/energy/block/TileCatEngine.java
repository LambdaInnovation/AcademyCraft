/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.energy.block;

import cn.academy.core.block.TileGeneratorBase;
import cn.academy.energy.client.render.block.RenderCatEngine;
import cn.lambdalib2.annoreg.core.Registrant;
import cn.lambdalib2.annoreg.mc.RegTileEntity;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.helper.TickScheduler;
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

    private final TickScheduler scheduler = new TickScheduler();

    // Sync
    int syncTicker;
    
    // Intrusive render parameters
    public double thisTickGen;
    public double rotation;
    public long lastRender;

    {
        scheduler.every(20).atOnly(Side.SERVER).run(() -> {
            NetworkMessage.sendToAllAround(TargetPoints.convert(this, 20), this, "sync_genspeed", thisTickGen);
        });
    }

    public TileCatEngine() {
        super("infinite_generator", 0, 2000, 200);
    }

    @Override
    public void update() {
        super.update();
        scheduler.runTick();
    }
    
    @Override
    public double getGeneration(double required) {
        return (thisTickGen = Math.min(required, 500));
    }

    @Listener(channel="sync_genspeed", side=Side.CLIENT)
    private void hSync(double genSpeed) {
        this.thisTickGen = genSpeed;
    }

}