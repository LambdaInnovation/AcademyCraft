package cn.academy.ability.vanilla.vecmanip.skill

import cn.academy.ACConfig
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.{Entity, EntityList, EntityLivingBase}
import net.minecraft.util.ResourceLocation

/**
  * Handles entity affection of VecDeviation and VecReflection.
  */
object EntityAffection {

  sealed trait AffectInfo
  case class Excluded() extends AffectInfo
  case class Affected(difficulty: Float) extends AffectInfo

  private val (entityData, excluded) = {
    import collection.JavaConversions._

    val cfg = ACConfig.instance.getConfig(
      "ac.ability.category.vecmanip.common.affected_entities")


    val entityData = {
      cfg.getConfigList("difficulties")
        .map(elem => (elem.getString("name"), elem.getDouble("difficulty").toFloat))
        .map { case (name, difficulty) => (EntityList.getClass(new ResourceLocation(name)), difficulty) }
        .find{case (klass, _)=>klass!=null}.toList
    }

    val excluded = cfg.getStringList("excluded")

    (entityData, excluded.toSet.map((key:String)=>{
      if(key.equalsIgnoreCase("living")){
        classOf[EntityLivingBase]
      }
      else if (key.equalsIgnoreCase("mob")){
        classOf[EntityMob]
      }
      else
        EntityList.getClass(new ResourceLocation(key))
    }).filter(_ != null))
  }

  def getAffectInfo(entity: Entity): AffectInfo = {
    if (excluded.exists(_.isInstance(entity))) {
      Excluded()
    } else {
      entityData.find { case (klass, _) => klass.isInstance(entity) }
        .map { case (_, difficulty) => Affected(difficulty) }
        .getOrElse(Affected(1))
    }
  }

  def mark(targ: Entity) = targ.getEntityData.setBoolean("ac_vm_deviated", true)
  def isMarked(targ: Entity) = targ.getEntityData.getBoolean("ac_vm_deviated")

}