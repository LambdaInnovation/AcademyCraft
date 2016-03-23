package cn.academy.ability.client.ui

import cn.academy.ability.api.Skill
import cn.academy.ability.block.TileDeveloper
import cn.academy.core.AcademyCraft
import cn.academy.core.client.Resources
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.{CGuiScreen, Widget}
import cn.lambdalib.cgui.xml.CGUIDocument
import net.minecraft.client.Minecraft
import cn.lambdalib.cgui.ScalaCGUI._
import cn.lambdalib.cgui.gui.event.FrameEvent
import cn.lambdalib.util.client.{HudUtils, RenderUtils}
import cn.lambdalib.util.key.{KeyHandler, KeyManager}
import org.lwjgl.input.Keyboard
import cn.lambdalib.util.generic.MathUtils._

object DeveloperUI {

  def apply(tile: TileDeveloper): CGuiScreen = {
    val ret = new CGuiScreen

    ret.getGui.addWidget(Common.initialize())

    ret
  }

}

object SkillTreeAppUI {
  def apply(): CGuiScreen = {
    val ret = new CGuiScreen

    ret.getGui.addWidget(Common.initialize())

    ret
  }
}

@Registrant
object SkillPosEditorUI {

  @RegInitCallback
  def __init() = {
    if (AcademyCraft.DEBUG_MODE) {
      KeyManager.dynamic.addKeyHandler("skill_tree_pos_editor", Keyboard.KEY_RMENU, new KeyHandler {
        override def onKeyDown() = {
          Minecraft.getMinecraft.displayGuiScreen(SkillPosEditorUI())
        }
      })
    }
  }

  def apply(): CGuiScreen = {
    val ret = new CGuiScreen

    ret.getGui.addWidget(Common.initialize())

    ret
  }

}

private object Common {

  private val template = CGUIDocument.panicRead(Resources.getGui("rework/page_developer")).getWidget("main")

  private val texSkillBack = Resources.getTexture("guis/effect/effect_developer_background")

  def player = Minecraft.getMinecraft.thePlayer

  def initialize(): Widget = {
    val ret = template.copy()

    { // Initialize the skill area
      val area = ret.child("parent_right/area")
      val back_scale = 1.02
      val back_scale_inv = 1 / back_scale
      val max_du = (back_scale - 1)

      area.listens((evt: FrameEvent) => {
        val gui = area.getGui

        // Update delta
        def scale(x: Double) = (x - 0.5) * back_scale_inv + 0.5

        val dx = (clampd(0, 1, gui.mouseX / gui.getWidth) - 0.5) * max_du
        val dy = (clampd(0, 1, gui.mouseY / gui.getHeight) - 0.5) * max_du

        // Update skill companion widgets

        // Draw background
        RenderUtils.loadTexture(texSkillBack)
        HudUtils.rawRect(0, 0, scale(dx), scale(dy), area.transform.width, area.transform.height, back_scale_inv, back_scale_inv)

        // Draw skill
      })
    }

    ret
  }

  private def normalize(x: Double, absmax: Double) = math.min(math.abs(x), absmax) * math.signum(x)

}
