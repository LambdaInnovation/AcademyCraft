package cn.academy.vanilla.vecmanip.skills

import cn.academy.core.config.ACConfig
import net.minecraft.entity.{Entity, EntityList}

/**
  * Handles entity affection of VecDeviation and VecReflection.
  */
object EntityAffection {

  sealed trait AffectInfo
  case class Excluded(difficulty: Float) extends AffectInfo
  case class Affected(difficulty: Float) extends AffectInfo

  private val (entityData, excluded) = {
    import collection.JavaConversions._

    val cfg = ACConfig.instance.getConfig(
      "ac.ability.category.vecmanip.common.affected_entities")

    val nameMapping = EntityList.stringToClassMapping.asInstanceOf[java.util.Map[String, Class[_<:Entity]]].toMap

    val entityData = {
      cfg.getConfigList("difficulties")
        .map(elem => (elem.getString("name"), elem.getDouble("difficulty").toFloat))
        .flatMap { case (name, difficulty) => nameMapping.get(name).map((_, difficulty)) }
        .toList
    }

    val excluded = cfg.getStringList("excluded")

    (entityData, excluded.toSet.flatMap(nameMapping.get))
  }

  def getAffectInfo(entity: Entity): AffectInfo = {
    if (excluded.exists(_.isInstance(entity))) {
      entityData.find { case (klass, _) => klass.isInstance(entity) }
        .map { case (_, difficulty) => Excluded(difficulty) }
        .getOrElse(Excluded(1))
    } else {
      entityData.find { case (klass, _) => klass.isInstance(entity) }
        .map { case (_, difficulty) => Affected(difficulty) }
        .getOrElse(Affected(1))
    }
  }

  def mark(targ: Entity) = targ.getEntityData.setBoolean("ac_vm_deviated", true)
  def isMarked(targ: Entity) = targ.getEntityData.getBoolean("ac_vm_deviated")

}
