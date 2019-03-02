package cn.academy.entity;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.vanilla.electromaster.skill.MagManip;
import cn.lambdalib2.registry.mc.RegEntity;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import cn.lambdalib2.util.entityx.MotionHandler;
import cn.lambdalib2.util.entityx.event.CollideEvent;
import cn.lambdalib2.util.entityx.handlers.Rigidbody;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@RegEntity(freq = 10)
public class MagManipEntityBlock extends EntityBlock
{
    public static final int ActNothing=0, ActMoveTo=1;
    public float damage;
    public boolean placeWhenCollide = false;
    EntityPlayer player2 = null;
    float yawSpeed = RandUtils.rangef(1, 3);
    float pitchSpeed = RandUtils.rangef(1, 3);
    private int tick =0;
    private final int MAXTICK = 5;

    public int actionType;
    float tx, ty, tz;

    public MagManipEntityBlock(EntityPlayer _player, float damage)
    {
        super(_player);
        this.damage = damage;
        this.player2 = _player;
    }

    public MagManipEntityBlock(World world)
    {
        super(world);
    }

    public void setMoveTo(double x, double y, double z)
    {
        actionType = ActMoveTo;
        tx = (float) x;
        ty = (float) y;
        tz = (float) z;
    }

    public void stopMoveTo()
    {
        actionType = ActNothing;
    }

    @Override
    public void entityInit()
    {
        super.entityInit();
    }

    @Override
    @SuppressWarnings("sideonly")
    public void onFirstUpdate()
    {
        super.onFirstUpdate();
        Rigidbody rb = getMotionHandler(Rigidbody.class);
        rb.entitySel = entity -> entity != player2;

        regEventHandler(new CollideEvent.CollideHandler()
        {
            @Override
            public void onEvent(CollideEvent event)
            {
                if(!getEntityWorld().isRemote && event.result!=null && event.result.entityHit!=null)
                {
                    AbilityContext.of(player2, MagManip.INSTANCE).attack(event.result.entityHit, damage);
                }
            }
        });

        if( world.isRemote)
        {
            startClient();
        }
        else
        {
            NetworkMessage.sendToAllAround(new NetworkRegistry.TargetPoint(world.provider.getDimension(), posX, posY, posZ, 40),
                    this, "MSG_ENT_SYNC", player2.getEntityId(), actionType, tx, ty, tz);
        }
    }

    @SideOnly(value=Side.CLIENT)
    @Listener(channel = "MSG_ENT_SYNC", side= Side.CLIENT)
    public void onClientSyncEntity(int id, int actType, float _tx, float _ty, float _tz)
    {
        player2 = (EntityPlayer) world.getEntityByID(id);
        actionType = actType;
        tx = _tx;
        ty = _ty;
        tz = _tz;
    }

    @Override
    public void onUpdate()
    {
        if(!world.isRemote)
        {
            tick++;
            if(tick>MAXTICK)
            {
                tick=0;
                NetworkMessage.sendToAllAround(new NetworkRegistry.TargetPoint(world.provider.getDimension(), posX, posY, posZ, 40),
                        this, "MSG_ENT_SYNC", player2.getEntityId(), actionType, tx, ty, tz);
            }
        }
        super.onUpdate();

        yaw+=yawSpeed;
        pitch += pitchSpeed;
        switch(actionType)
        {
            case ActMoveTo:
                double dist = this.getDistanceSq(tx, ty, tz);
                Vec3d delta = new Vec3d(tx - posX, ty - posY, tz - posZ).normalize();
                Vec3d mo = delta.scale(0.2).scale(dist<4?dist/4:1.0);
                VecUtils.setMotion(this, mo);
                break;
            case ActNothing:
                motionY -=0.04;
        }
        posX +=motionX;
        posY +=motionY;
        posZ +=motionZ;
    }

    private double move(double fr, double to, double max)
    {
        double delta = to-fr;
        return fr + Math.min(Math.abs(delta), max) * Math.signum(delta);
    }

    @SideOnly(Side.CLIENT)
    private void startClient()
    {
        EntitySurroundArc surrounder = new EntitySurroundArc(this);
        surrounder.life = 233333;
        surrounder.addMotionHandler(new MotionHandler()
        {
            @Override
            public String getID() { return "killer"; }

            @Override
            public void onStart() { }

            @Override
            public void onUpdate()
            {
                if (MagManipEntityBlock.this.isDead) getTarget().setDead();
            }
        });
        surrounder.setArcType(EntitySurroundArc.ArcType.THIN);
        world.spawnEntity(surrounder);
    }
}
