/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.client.effect;

import cn.academy.core.client.Resources;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.particle.ParticleFactory;
import cn.lambdalib.util.entityx.MotionHandler;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import java.util.Random;

@SideOnly(Side.CLIENT)
public class IronSandParticles {
    
    ResourceLocation[] textures = Resources.getEffectSeq("ironsand", 5);
    
    Random rand = new Random();
    ParticleFactory factory;
    
    final EntityPlayer player;
    
    public IronSandParticles(EntityPlayer _player) {
        player = _player;
        
        Particle template = new Particle();
        template.size = 0.6f;
        template.needRigidbody = false;
        
        factory = new ParticleFactory(template) {
            @Override
            public Particle next(World world) {
                Particle ret = super.next(world);
                final long time = RandUtils.rangei(3000, 4000);
                
                ret.fadeAfter(RandUtils.rangei(40, 60), 20);
                ret.fadeInTime = 10;
                ret.ignoreFrustumCheck = true;
                
                double theta = rand.nextDouble() * Math.PI * 2;
                double r = 1.7 + RandUtils.ranged(-0.1, 0.1);
                double dy = RandUtils.ranged(-1.6, -1.5);
                
                double dvR = RandUtils.ranged(-0.01, 0.01), dvY = RandUtils.ranged(0.01, 0.02);
                double spin = 3;
                
                ret.texture = textures[RandUtils.rangei(0, 5)];
                ret.setPosition(
                    player.posX + r * Math.sin(theta), 
                    player.posY + dy, 
                    player.posZ + r * Math.cos(theta));
                
                ret.addMotionHandler(new MotionHandler<Particle>() {
                    
                    double rad = r;

                    @Override
                    public String getID() {
                        return "surround";
                    }

                    @Override
                    public void onStart() { onUpdate(); }

                    @Override
                    public void onUpdate() {
                        Particle target = getTarget();
                        double time = (target.getParticleLife() / 1000.0) % (Math.PI * 2);
                        target.posX = player.posX + rad * Math.sin(theta + spin * time);
                        target.posY += dvY;
                        target.posZ = player.posZ + rad * Math.cos(theta + spin * time);
                        rad += dvR;
                    }
                    
                });
                return ret;
            }
        };
    }
    
    public void tick() {
        int n = RandUtils.rangei(2, 3);
        for(int i = 0; i < n; ++i) {
            player.worldObj.spawnEntityInWorld(factory.next(player.worldObj));
        }
    }
}
