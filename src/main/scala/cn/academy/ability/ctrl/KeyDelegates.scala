package cn.academy.ability.ctrl

import cn.academy.ability.Skill
import cn.academy.ability.context.{Context, ContextManager, DelegateState, KeyDelegate}
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.ResourceLocation

import scala.reflect.ClassTag

object KeyDelegates {

  /**
    * Creates a context that activates the given context on key down,
    * @param skill Skill that this context belongs to
    * @tparam T type of context
    */
  def contextActivate[S <: Skill, T <: Context[S]](skill: S, contextProvider: EntityPlayer => T)
                                   (implicit tag: ClassTag[T]): KeyDelegate = {
    val klass = tag.runtimeClass.asInstanceOf[Class[T]]
    new KeyDelegate {
      override def onKeyDown(): Unit = {
        findContext() match {
          case Some(ctx) => ctx.terminate()
          case _ => ContextManager.instance.activate(contextProvider(getPlayer))
        }
      }

      override def createID(): Int = 0

      override def getIcon: ResourceLocation = skill.getHintIcon

      override def getSkill: Skill = skill

      override def getState: DelegateState = findContext() match {
        case Some(_) => DelegateState.ACTIVE
        case _ => DelegateState.IDLE
      }

      private def findContext(): Option[T] = {
        val opt = ContextManager.instance.findLocal(klass)
        if (opt.isPresent) Some(opt.get) else None
      }
    }
  }

}