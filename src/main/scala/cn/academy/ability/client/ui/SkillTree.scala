package cn.academy.ability.client.ui

import cn.academy.ability.api.data.AbilityData
import cn.academy.ability.block.TileDeveloper
import cn.academy.core.AcademyCraft
import cn.academy.core.client.Resources
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.{CGuiScreen, Widget}
import cn.lambdalib.cgui.xml.CGUIDocument
import net.minecraft.client.Minecraft
import cn.lambdalib.cgui.ScalaCGUI._
import cn.lambdalib.cgui.gui.component.DrawTexture
import cn.lambdalib.cgui.gui.event.FrameEvent
import cn.lambdalib.util.client.shader.ShaderProgram
import cn.lambdalib.util.client.{HudUtils, RenderUtils}
import cn.lambdalib.util.key.{KeyHandler, KeyManager}
import org.lwjgl.input.Keyboard
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.helper.GameTimer
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL20._

import scala.collection.JavaConversions._

object DeveloperUI {

  def apply(tile: TileDeveloper): CGuiScreen = {
    val ret = Common.newScreen()

    ret.getGui.addWidget(Common.initialize())

    ret
  }

}

object SkillTreeAppUI {
  def apply(): CGuiScreen = {
    val ret = Common.newScreen()

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
    val ret = Common.newScreen()

    ret.getGui.addWidget(Common.initialize())

    ret
  }

}

private object Common {

  private val template = CGUIDocument.panicRead(Resources.getGui("rework/page_developer")).getWidget("main")

  private val texAreaBack = Resources.getTexture("guis/effect/effect_developer_background")
  private val texSkillBack = Resources.getTexture("guis/developer/skill_back")
  private val texSkillMask = Resources.getTexture("guis/developer/skill_radial_mask")
  private val texSkillOutline = Resources.getTexture("guis/developer/skill_outline")

  private val shader = new ShaderProgram
  shader.linkShader(Resources.getShader("skill_progbar.frag"), GL_FRAGMENT_SHADER)
  shader.linkShader(Resources.getShader("skill_progbar.vert"), GL_VERTEX_SHADER)
  shader.compile()

  shader.useProgram()

  {
    glUniform1i(shader.getUniformLocation("texCircle"), 0)
    glUniform1i(shader.getUniformLocation("texGradient"), 1)
    glUniform1f(shader.getUniformLocation("progress"), 0.7f)
  }
  glUseProgram(0)

  def player = Minecraft.getMinecraft.thePlayer

  def initialize(): Widget = {
    val ret = template.copy()

    val aData = AbilityData.get(player)

    { // Initialize the skill area
      val area = ret.child("parent_right/area")
      val back_scale = 1.01
      val back_scale_inv = 1 / back_scale
      val max_du = back_scale - 1
      val max_du_skills = 10

      var (dx, dy) = (0.0, 0.0)

      area.listens((evt: FrameEvent) => {
        val gui = area.getGui

        // Update delta
        def scale(x: Double) = (x - 0.5) * back_scale_inv + 0.5

        dx = clampd(0, 1, gui.mouseX / gui.getWidth) - 0.5
        dy = clampd(0, 1, gui.mouseY / gui.getHeight) - 0.5

        // Draw background
        RenderUtils.loadTexture(texAreaBack)
        HudUtils.rawRect(0, 0, scale(dx * max_du), scale(dy * max_du),
          area.transform.width, area.transform.height,
          back_scale_inv, back_scale_inv)
      })

      if (aData.hasCategory) {
        val skills = aData.getLearnedSkillList

        val comp = skills.foreach(skill => {
          val StateIdle = 0
          val StateHover = 1
          val TransitTime = 100.0

          val WidgetSize = 16.0
          val ProgSize = 31.0
          val TotalSize = 23.0
          val IconSize = 16.0
          val ProgAlign = (TotalSize - ProgSize) / 2
          val Align = (TotalSize - IconSize) / 2
          val DrawAlign = (WidgetSize - TotalSize) / 2

          val learned = aData.isSkillLearned(skill)

          val widget = new Widget
          val (sx, sy) = (skill.guiX * .3 - 10, skill.guiY * .2)

          var lastTransit = GameTimer.getTime
          var state = StateIdle

          widget.pos(sx, sy).size(WidgetSize, WidgetSize).addComponent(new DrawTexture(null).setColor4d(1, 0, 0, 0.1))
          widget.listens((evt: FrameEvent) => {
            val time = GameTimer.getTime

            widget.pos(sx - dx * max_du_skills, sy - dy * max_du_skills)
            widget.dirty = true

            val transitProgress = clampd(0, 1, (time - lastTransit) / TransitTime)
            val scale = state match {
              case StateIdle => lerp(1.2, 1, clampd(0, 1, transitProgress))
              case StateHover => lerp(1, 1.2, clampd(0, 1, transitProgress))
            }

            // Transit state
            if (transitProgress == 1) {
              if (state == StateIdle && evt.hovering) {
                state = StateHover
                lastTransit = GameTimer.getTime
              } else if (state == StateHover && !evt.hovering) {
                state = StateIdle
                lastTransit = GameTimer.getTime
              }
            }

            glEnable(GL_DEPTH_TEST)
            glPushMatrix()
            glColor4f(1, 1, 1, 1)

            glTranslated(DrawAlign, DrawAlign, 10)

            glTranslated(TotalSize/2, TotalSize/2, 0)
            glScaled(scale, scale, 1)
            glTranslated(-TotalSize/2, -TotalSize/2, 0)

            // Draw back without depth writing
            glDepthMask(false)
            RenderUtils.loadTexture(texSkillBack)
            HudUtils.rect(0, 0, TotalSize, TotalSize)

            // Draw back as a depth mask
            glDepthMask(true)
            glEnable(GL_ALPHA_TEST)
            glColorMask(false, false, false, false)
            glAlphaFunc(GL_GREATER, 0.06f)
            HudUtils.rect(0, 0, TotalSize, TotalSize)
            glDisable(GL_ALPHA_TEST)
            glColorMask(true, true, true, true)

            // Draw skill
            glDepthFunc(GL_EQUAL)
            RenderUtils.loadTexture(skill.getHintIcon)
            HudUtils.rect(Align, Align, IconSize, IconSize)
            glDepthFunc(GL_LEQUAL)

            // Progress bar (if learned)
            if (learned) {
              shader.useProgram()

              glActiveTexture(GL_TEXTURE0)
              RenderUtils.loadTexture(texSkillOutline)

              glActiveTexture(GL_TEXTURE1)
              RenderUtils.loadTexture(texSkillMask)

              glActiveTexture(GL_TEXTURE0)
              HudUtils.rect(ProgAlign, ProgAlign, ProgSize, ProgSize)

              glUseProgram(0)
            }

            glPopMatrix()
            glDisable(GL_DEPTH_TEST)
          })


          area :+ widget
        })


      }
    }

    ret
  }

  private def normalize(x: Double, absmax: Double) = math.min(math.abs(x), absmax) * math.signum(x)

  def newScreen(): CGuiScreen = new CGuiScreen() {
    override def doesGuiPauseGame = false
  }

}
