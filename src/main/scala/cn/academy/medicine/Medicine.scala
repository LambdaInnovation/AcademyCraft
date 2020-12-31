package cn.academy.medicine

import cn.academy.Resources

import java.util
import cn.academy.medicine.MedSynth.MedicineApplyInfo
import cn.lambdalib2.render.legacy.LegacyShaderProgram
import cn.lambdalib2.util.RenderUtils
import net.minecraft.util.{ActionResult, EnumActionResult, EnumHand}
import net.minecraftforge.common.model.IModelState
//import codechicken.lib.render.RenderUtils
import codechicken.lib.render.item.IItemRenderer
import codechicken.lib.render.shader.ShaderProgram
import net.minecraft.client.renderer.block.model.ItemCameraTransforms
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.text.TextFormatting
import net.minecraft.world.World
import org.lwjgl.opengl.GL11
import org.lwjgl.util.Color

class ItemMedicineBase() extends Item {

  setMaxStackSize(1)

  def create(info: MedicineApplyInfo): ItemStack = {
    val stack = new ItemStack(this)
    MedSynth.writeApplyInfo(stack, info)

    stack
  }

  def getInfo(stack: ItemStack): MedicineApplyInfo = {
    MedSynth.readApplyInfo(stack)
  }

//  override def addInformation(stack: ItemStack, player: EntityPlayer, list2: util.List[_], wtf: Boolean): Unit = {
//    val list = list2.asInstanceOf[util.List[String]]
//    val info = getInfo(stack)
//
//    if (info.target != Properties.Targ_Disposed) {
//      list.add(info.target.displayDesc + " " + info.method.displayDesc)
//      list.add(info.strengthType.displayDesc)
//    } else {
//      list.add(TextFormatting.RED + Properties.Targ_Disposed.displayDesc)
//    }
//  }

  override def onItemRightClick(worldIn: World, playerIn: EntityPlayer, handIn: EnumHand): ActionResult[ItemStack] = {
    var stack = playerIn.getHeldItem(handIn)
    val info = getInfo(stack)

    if (!worldIn.isRemote) {
      info.target.apply(playerIn, info)
    }

    if (!playerIn.capabilities.isCreativeMode) {
      stack.setCount(stack.getCount - 1)
    }

//    stack
    new ActionResult[ItemStack](EnumActionResult.PASS, stack)
  }
}

object ItemMedicineBottle extends ItemMedicineBase/*("medicine_bottle")*/ {

//  override def getSubItems(item: Item, cct: CreativeTabs, list2: util.List[_]): Unit = {
//    val list = list2.asInstanceOf[util.List[ItemStack]]
//
//    // For debug
//    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Mild, 1.0f, Properties.Apply_Instant_Incr, 0.5f)))
//    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Weak, 1.0f, Properties.Apply_Instant_Incr, 0.5f)))
//    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Normal, 1.0f, Properties.Apply_Instant_Incr, 0.5f)))
//    list.add(create(MedicineApplyInfo(Properties.Targ_Life, Properties.Str_Strong, 2.0f, Properties.Apply_Instant_Decr, 0.5f)))
//  }



}

object RenderMedicineBottle {
  def apply(fn: java.util.function.Function[ItemStack, Color]) = new RenderMedicineBottle(fn.apply)
}

class RenderMedicineBottle(val colorProvider: ItemStack=>Color) extends IItemRenderer {
  import org.lwjgl.opengl.GL20._
  import GL11._

  val texture = Resources.getTexture("items/med_bottle")

  private val shaderProg = new LegacyShaderProgram
  shaderProg.linkShader(Resources.getShader("med_bottle.vert"), GL_FRAGMENT_SHADER)
  shaderProg.linkShader(Resources.getShader("med_bottle.frag"), GL_VERTEX_SHADER)
  shaderProg.compile()
//  val program = ShaderProgram.load(
//    Resources.getShader("med_bottle.vert"),
//    Resources.getShader("med_bottle.frag")
//  )
  val locColor = glGetUniformLocation(shaderProg.getProgramID, "u_color")

//  override def handleRenderType(item: ItemStack, renderType: ItemRenderType): Boolean = true
//
//  override def shouldUseRenderHelper(renderType : ItemRenderType, item: ItemStack, helper: ItemRendererHelper) = {
//    renderType == ItemRenderType.ENTITY
//  }

  override def renderItem(stack: ItemStack, renderType: ItemCameraTransforms.TransformType): Unit = {
    import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType._
    RenderUtils.loadTexture(texture)
    glUseProgram(shaderProg.getProgramID)

    {
      val color = colorProvider(stack)
      glUniform4f(locColor, color.getRed.toFloat, color.getGreen.toFloat, color.getBlue.toFloat, color.getAlpha.toFloat)
    }

    glEnable(GL_BLEND)
    glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
    renderType match {
//      case EQUIPPED | EQUIPPED_FIRST_PERSON | ENTITY =>
      case FIRST_PERSON_RIGHT_HAND =>
//        if (renderType == ENTITY) {
//          glTranslated(-.5, -0.1, 0)
//        }
        RenderUtils.drawEquippedItem(.04f, texture)
//      case INVENTORY =>
//        HudUtils.rect(0, 0, 16, 16)
    }
    glUseProgram(0)
  }

  override def getTransforms: IModelState = null

  override def isAmbientOcclusion: Boolean = false

  override def isGui3d: Boolean = false
}
