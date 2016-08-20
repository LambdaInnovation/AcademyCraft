package cn.academy.medicine

import java.util

import cn.academy.core.Resources
import cn.academy.core.item.ACItem
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.lambdalib.pipeline.api.ShaderProgram
import cn.lambdalib.util.client.{HudUtils, RenderUtils}
import cn.lambdalib.util.helper.Color
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.EnumChatFormatting
import net.minecraft.world.World
import net.minecraftforge.client.IItemRenderer
import net.minecraftforge.client.IItemRenderer.{ItemRenderType, ItemRendererHelper}
import org.lwjgl.opengl.GL11

class ItemMedicineBase(name: String) extends ACItem(name) {

  setMaxStackSize(1)

  def create(info: MedicineApplyInfo): ItemStack = {
    val stack = new ItemStack(this)
    MedSynth.writeApplyInfo(stack, info)

    stack
  }

  def getInfo(stack: ItemStack): MedicineApplyInfo = {
    MedSynth.readApplyInfo(stack)
  }

  override def addInformation(stack: ItemStack, player: EntityPlayer, list2: util.List[_], wtf: Boolean): Unit = {
    val list = list2.asInstanceOf[util.List[String]]
    val info = getInfo(stack)

    if (info.target != Properties.Targ_Disposed) {
      list.add(info.target.displayDesc + " " + info.method.displayDesc)
      list.add(info.strengthType.displayDesc)
    } else {
      list.add(EnumChatFormatting.RED + Properties.Targ_Disposed.displayDesc)
    }
  }

  override def onItemRightClick(stack: ItemStack, world: World, player: EntityPlayer): ItemStack = {
    val info = getInfo(stack)

    if (!world.isRemote) {
      info.target.apply(player, info)
    }

    if (!player.capabilities.isCreativeMode) {
      stack.stackSize -= 1
    }

    stack
  }
}

object ItemMedicineBottle extends ItemMedicineBase("medicine_bottle") {

  override def getSubItems(item: Item, cct: CreativeTabs, list2: util.List[_]): Unit = {
    val list = list2.asInstanceOf[util.List[ItemStack]]

    // For debug
    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Mild, 1.0f, Properties.Apply_Instant_Incr, 0.5f)))
    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Weak, 1.0f, Properties.Apply_Instant_Incr, 0.5f)))
    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Normal, 1.0f, Properties.Apply_Instant_Incr, 0.5f)))
    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Strong, 2.0f, Properties.Apply_Instant_Decr, 0.5f)))
  }



}

object RenderMedicineBottle {
  def apply(fn: java.util.function.Function[ItemStack, Color]) = new RenderMedicineBottle(fn.apply)
}

class RenderMedicineBottle(val colorProvider: ItemStack=>Color) extends IItemRenderer {
  import org.lwjgl.opengl.GL20._
  import GL11._

  val texture = Resources.getTexture("items/med_bottle")
  val program = ShaderProgram.load(
    Resources.getShader("med_bottle.vert"),
    Resources.getShader("med_bottle.frag")
  )
  val locColor = glGetUniformLocation(program.getProgramID, "u_color")

  override def handleRenderType(item: ItemStack, renderType: ItemRenderType): Boolean = true

  override def shouldUseRenderHelper(renderType : ItemRenderType, item: ItemStack, helper: ItemRendererHelper) = {
    renderType == ItemRenderType.ENTITY
  }

  override def renderItem(renderType: ItemRenderType, stack: ItemStack, data: AnyRef*): Unit = {
    import ItemRenderType._
    RenderUtils.loadTexture(texture)
    glUseProgram(program.getProgramID)

    {
      val color = colorProvider(stack)
      glUniform4f(locColor, color.r.toFloat, color.g.toFloat, color.b.toFloat, color.a.toFloat)
    }

    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    renderType match {
      case EQUIPPED | EQUIPPED_FIRST_PERSON | ENTITY =>
        if (renderType == ENTITY) {
          glTranslated(-.5, -0.1, 0)
        }
        RenderUtils.drawEquippedItem(.04f, texture)
      case INVENTORY =>
        HudUtils.rect(0, 0, 16, 16)
    }
    glUseProgram(0)
  }
}
