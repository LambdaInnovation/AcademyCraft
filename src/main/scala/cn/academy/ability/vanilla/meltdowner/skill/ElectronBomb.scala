package cn.academy.ability.vanilla.meltdowner.skill

import cn.academy.ability.Skill
import cn.academy.ability.context.{ClientContext, ClientRuntime, Context, RegClientContext}
import cn.academy.advancements.ACAdvancements
import cn.academy.client.render.util.ACRenderingHelper
import cn.academy.entity.{EntityMdBall, EntityMdRaySmall}
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.util.entityx.EntityCallback
import cn.lambdalib2.util.{EntitySelectors, Raytrace, VecUtils}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.math.{RayTraceResult, Vec3d}

/**
  * @author WeAthFolD, KSkun
  */
object ElectronBomb extends Skill("electron_bomb", 1) {

  @SideOnly(Side.CLIENT)
  override def activate(rt: ClientRuntime, keyID: Int) = activateSingleKey(rt, keyID, p => new EBContext(p))

}

object EBContext {

  final val MSG_EFFECT = "effect"

}

import cn.lambdalib2.util.MathUtils._
import cn.academy.ability.api.AbilityAPIExt._
import EBContext._

class EBContext(p: EntityPlayer) extends Context(p, ElectronBomb) {

  private val LIFE: Int = 20
  private val LIFE_IMPROVED: Int = 5
  private val DISTANCE: Double = 15

  private def consume() = {
    val exp: Float = ctx.getSkillExp
    val overload: Float = lerpf(16, 13, exp)
    val cp: Float = lerpf(35, 80, exp)

    ctx.consume(overload, cp)
  }

  @Listener(channel=MSG_MADEALIVE, side=Array(Side.SERVER))
  private def s_execute() = {
    if(consume()) {
      val exp: Float = ctx.getSkillExp
//      if (ctx.getSkillExp >= 0.8f) ACAdvancements.trigger(player, ACAdvancements.ac_milestone.ID)
      val ball: EntityMdBall = new EntityMdBall(player, if (ctx.getSkillExp >= 0.8f) LIFE_IMPROVED else LIFE,
        new EntityCallback[EntityMdBall]() {
          def execute(ball: EntityMdBall) {
            val trace: RayTraceResult = Raytrace.perform(player.world, new Vec3d(ball.posX, ball.posY, ball.posZ),
              getDest(player), EntitySelectors.exclude(player).and(EntitySelectors.of(classOf[EntityMdBall]).negate))
            if (trace != null && trace.entityHit != null) MDDamageHelper.attack(ctx, trace.entityHit, getDamage(exp))
            sendToClient(MSG_EFFECT, ball)
            terminate()
          }
        })
      player.world.spawnEntity(ball)

      ctx.addSkillExp(.005f)
      ctx.setCooldown(lerpf(20, 10, exp).toInt)
    }
  }

  private def getDest(player: EntityPlayer): Vec3d = Raytrace.getLookingPos(player, DISTANCE).getLeft

  private def getDamage(exp: Float): Float = lerpf(6, 12, exp)

}

@SideOnly(Side.CLIENT)
@RegClientContext(classOf[EBContext])
class EBContextC(par: EBContext) extends ClientContext(par) {

  private val DISTANCE: Double = 15
  
  @Listener(channel=MSG_EFFECT, side=Array(Side.CLIENT))
  private def c_spawnEffect(ball: EntityMdBall) = {
    val dest: Vec3d = getDest(player)
    val raySmall: EntityMdRaySmall = new EntityMdRaySmall(player.world)
    raySmall.setFromTo(ball.posX, ball.posY + player.eyeHeight, ball.posZ, dest.x, dest.y, dest.z)
    raySmall.viewOptimize = false
    player.world.spawnEntity(raySmall)
  }

  private def getDest(player: EntityPlayer): Vec3d = Raytrace.getLookingPos(player, DISTANCE).getLeft
  
}