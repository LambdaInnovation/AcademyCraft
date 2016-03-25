package cn.academy.ability.client.ui

import cn.academy.ability.api.Skill
import cn.academy.ability.api.data.{AbilityData, CPData}
import cn.academy.ability.block.TileDeveloper
import cn.academy.ability.develop.DevelopData.DevState
import cn.academy.ability.develop.action.DevelopActionSkill
import cn.academy.ability.develop.condition.IDevCondition
import cn.academy.ability.develop.{DevelopData, IDeveloper, LearningHelper}
import cn.academy.core.AcademyCraft
import cn.academy.core.client.Resources
import cn.academy.core.client.ui.{TechUI, WirelessPage}
import cn.academy.energy.api.WirelessHelper
import cn.lambdalib.annoreg.core.Registrant
import cn.lambdalib.annoreg.mc.RegInitCallback
import cn.lambdalib.cgui.gui.{CGui, CGuiScreen, Widget}
import cn.lambdalib.cgui.xml.CGUIDocument
import net.minecraft.client.Minecraft
import cn.lambdalib.cgui.ScalaCGUI._
import cn.lambdalib.cgui.gui.component.Transform.{HeightAlign, WidthAlign}
import cn.lambdalib.cgui.gui.component._
import cn.lambdalib.cgui.gui.event.{FrameEvent, LeftClickEvent}
import cn.lambdalib.s11n.network.NetworkMessage.Listener
import cn.lambdalib.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib.util.client.font.IFont.{FontAlign, FontOption}
import cn.lambdalib.util.client.shader.{ShaderMono, ShaderProgram}
import cn.lambdalib.util.client.{HudUtils, RenderUtils}
import cn.lambdalib.util.key.{KeyHandler, KeyManager}
import org.lwjgl.input.Keyboard
import cn.lambdalib.util.generic.MathUtils._
import cn.lambdalib.util.helper.{Color, GameTimer}
import cpw.mods.fml.relauncher.Side
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL20._

import scala.collection.JavaConversions._

object DeveloperUI {

  def apply(tile: IDeveloper): CGuiScreen = {
    val ret = Common.newScreen()
    implicit val gui = ret.gui()

    ret.getGui.addWidget(Common.initialize(tile))

    ret
  }

}

object SkillTreeAppUI {
  def apply(): CGuiScreen = {
    val ret = Common.newScreen()
    implicit val gui = ret.gui()

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
    implicit val gui = ret.gui()

    ret.getGui.addWidget(Common.initialize())

    ret
  }

}

private object Common {

  private val template = CGUIDocument.panicRead(Resources.getGui("rework/page_developer")).getWidget("main")

  private val texAreaBack = Resources.preloadTexture("guis/effect/effect_developer_background")
  private val texSkillBack = Resources.preloadMipmapTexture("guis/developer/skill_back")
  private val texSkillMask = Resources.preloadMipmapTexture("guis/developer/skill_radial_mask")
  private val texSkillOutline = Resources.preloadMipmapTexture("guis/developer/skill_outline")
  private val texLine = Resources.preloadMipmapTexture("guis/developer/line")
  private val texViewOutline = Resources.preloadMipmapTexture("guis/developer/skill_view_outline")
  private val texViewOutlineGlow = Resources.preloadMipmapTexture("guis/developer/skill_view_outline_glow")
  private val texButtonLearn = Resources.getTexture("guis/button/button_learn")
  private val texButtonReset = Resources.getTexture("guis/button/button_reset")
  private val texButton      = Resources.getTexture("guis/developer/button")

  private val foSkillTitle = new FontOption(12, FontAlign.CENTER)
  private val foSkillDesc = new FontOption(9, FontAlign.CENTER)
  private val foSkillProg = new FontOption(8, FontAlign.CENTER, new Color(0xffa1e1ff))
  private val foSkillUnlearned = new FontOption(10, FontAlign.CENTER, new Color(0xffff5555))
  private val foSkillUnlearned2 = new FontOption(10, FontAlign.CENTER, new Color(0xaaffffff))
  private val foSkillReq = new FontOption(9, FontAlign.RIGHT, new Color(0xaaffffff))
  private val foSkillReqDetail = new FontOption(9, FontAlign.LEFT, new Color(0xeeffffff))
  private val foSkillReqDetail2 = new FontOption(9, FontAlign.LEFT, new Color(0xffee5858))

  private val Font = Resources.font()
  private val FontBold = Resources.fontBold()

  private val shaderProg = new ShaderProgram
  shaderProg.linkShader(Resources.getShader("skill_progbar.frag"), GL_FRAGMENT_SHADER)
  shaderProg.linkShader(Resources.getShader("skill_progbar.vert"), GL_VERTEX_SHADER)
  shaderProg.compile()

  private val shaderMono = ShaderMono.instance()

  private val posProgress = shaderProg.getUniformLocation("progress")

  shaderProg.useProgram()

  {
    glUniform1i(shaderProg.getUniformLocation("texCircle"), 0)
    glUniform1i(shaderProg.getUniformLocation("texGradient"), 1)
    glUniform1f(posProgress, 0.7f)
  }
  glUseProgram(0)

  def player = Minecraft.getMinecraft.thePlayer

  def initialize(developer: IDeveloper = null)(implicit gui: CGui): Widget = {
    val ret = template.copy()

    implicit val aData = AbilityData.get(player)
    implicit val developer_ = developer

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
        val skills = aData.getCategory.getSkillList.toList
          .filter(skill => LearningHelper.canBePotentiallyLearned(aData, skill))

        skills.zipWithIndex.foreach { case (skill, idx) =>
          val StateIdle = 0
          val StateHover = 1
          val TransitTime = 100.0

          val WidgetSize = 16.0
          val ProgSize = 31.0
          val TotalSize = 23.0
          val IconSize = 14.0
          val ProgAlign = (TotalSize - ProgSize) / 2
          val Align = (TotalSize - IconSize) / 2
          val DrawAlign = (WidgetSize - TotalSize) / 2

          val learned = aData.isSkillLearned(skill)

          val widget = new Widget
          val (sx, sy) = (skill.guiX, skill.guiY)

          var lastTransit = GameTimer.getTime - 2000
          var state = StateIdle
          val creationTime = GameTimer.getTime
          val blendOffset = idx * 80 + 100

          val lineDrawer = Option(skill.getParent).map(parent => {
            def center(x: Double, y: Double) = (x + WidgetSize / 2, y + WidgetSize / 2)

            val (cx, cy) = center(skill.guiX, skill.guiY)
            val (pcx, pcy) = center(parent.guiX, parent.guiY)
            val (px, py) = (pcx - cx, pcy - cy)
            val norm = math.sqrt(px * px + py * py)
            val (dx, dy) = (px/norm*12.2, py/norm*12.2)

            drawLine(px + WidgetSize / 2 - dx, py + WidgetSize / 2 - dy,
              WidgetSize / 2 + dx, WidgetSize / 2 + dy, 5.5)
          })

          widget.pos(sx, sy).size(WidgetSize, WidgetSize)
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

            val dt = math.max(0, (time - creationTime - blendOffset) / 1000.0)
            val backAlpha = math.max(0, dt * 10.0)
            val iconAlpha = math.max(0, (dt - 0.08) * 10.0)
            val progressBlend = clampd(0, 1, (dt - 0.12) * 2.0).toFloat
            val lineBlend = clampd(0, 1, dt * 5.0)

            glEnable(GL_DEPTH_TEST)
            glPushMatrix()

            glTranslated(DrawAlign, DrawAlign, 10)

            glTranslated(TotalSize/2, TotalSize/2, 0)
            glScaled(scale, scale, 1)
            glTranslated(-TotalSize/2, -TotalSize/2, 0)

            // Draw back without depth writing
            glColor4d(1, 1, 1, backAlpha)
            glDepthMask(false)
            RenderUtils.loadTexture(texSkillBack)
            HudUtils.rect(0, 0, TotalSize, TotalSize)

            // Draw outline back
            RenderUtils.loadTexture(texSkillOutline)
            glColor4f(0.2f, 0.2f, 0.2f, 0.6f)
            HudUtils.rect(ProgAlign, ProgAlign, ProgSize, ProgSize)
            glColor4f(1, 1, 1, 1)

            // Draw back as a depth mask
            glDepthMask(true)
            glEnable(GL_ALPHA_TEST)
            glColorMask(false, false, false, false)
            glAlphaFunc(GL_GREATER, 0.3f)

            RenderUtils.loadTexture(texSkillBack)
            HudUtils.rect(0, 0, TotalSize, TotalSize)

            glPushMatrix()
            glTranslated(0, 0, 1)
            RenderUtils.loadTexture(texSkillOutline)
            glAlphaFunc(GL_GREATER, 0.5f)
            HudUtils.rect(ProgAlign, ProgAlign, ProgSize, ProgSize)
            glPopMatrix()

            glDisable(GL_ALPHA_TEST)
            glColorMask(true, true, true, true)
            glDepthMask(false)

            // Draw skill
            glColor4d(1, 1, 1, iconAlpha)
            glDepthFunc(GL_EQUAL)
            RenderUtils.loadTexture(skill.getHintIcon)
            HudUtils.rect(Align, Align, IconSize, IconSize)
            glDepthFunc(GL_LEQUAL)

            // Progress bar (if learned)
            glColor4d(1, 1, 1, 1)
            if (learned) {
              glDisable(GL_DEPTH_TEST)

              shaderProg.useProgram()
              glUniform1f(posProgress, progressBlend * aData.getSkillExp(skill))

              glActiveTexture(GL_TEXTURE0)
              RenderUtils.loadTexture(texSkillOutline)

              glActiveTexture(GL_TEXTURE1)
              RenderUtils.loadTexture(texSkillMask)

              glActiveTexture(GL_TEXTURE0)
              HudUtils.rect(ProgAlign, ProgAlign, ProgSize, ProgSize)

              glUseProgram(0)
              glEnable(GL_DEPTH_TEST)
            }

            glPopMatrix()

            glDepthFunc(GL_NOTEQUAL)
            glPushMatrix()
            glTranslated(0, 0, 11)
            lineDrawer match {
              case Some(drawer) => drawer(lineBlend)
              case _ =>
            }
            glPopMatrix()

            glDepthFunc(GL_LEQUAL)
            glDisable(GL_DEPTH_TEST)
          })

          widget.listens[LeftClickEvent](() => {
            val cover = skillViewArea(skill)

            widget.getGui.addWidget(cover)
          })


          area :+ widget
        }
      }
    }

    { // Initialize left ability panel
      val panel = ret.child("parent_left/panel_ability")

      val (icon, name, prog, lvltext) = Option(aData.getCategory) match {
        case Some(cat) => (cat.getDeveloperIcon, cat.getDisplayName, math.max(0.02, CPData.get(player).getLevelProgress), "Level " + aData.getLevel)
        case None => (Resources.getTexture("guis/icons/icon_nonecat"), "No Category", 0.0f, "")
      }

      panel.child("logo_ability").component[DrawTexture].setTex(icon)
      panel.child("text_abilityname").component[TextBox].setContent(name)
      panel.child("logo_progress").component[ProgressBar].progress = prog
      panel.child("text_level").component[TextBox].setContent(lvltext)
    }

    { // Initialize machine panel
      val panel = ret.child("parent_left")

      val wProgPower = panel.child("progress_power")
      val progPower = wProgPower.component[ProgressBar]

      val wProgRate = panel.child("progress_syncrate")
      val progRate = wProgRate.component[ProgressBar]

      val wirelessButton = panel.child("button_wireless")

      if (developer != null) {
        wProgPower.listens[FrameEvent](() => {
          progPower.progress = developer.getEnergy / developer.getMaxEnergy
        })
        progRate.progress = developer.getType.syncRate
        developer match {
          case tile: TileDeveloper =>
            send(NetDelegate.MSG_GET_NODE, tile, Future.create((result: String) => {
              panel.child("button_wireless/text_nodename").component[TextBox].content = if (result != null) result else "N/A"
            }))
            panel.child("button_wireless").listens[LeftClickEvent](() => {
              val wirelessPage = WirelessPage.userPage(tile).window.centered()
              val cover = blackCover(gui)
              cover :+ wirelessPage

              cover.listens[LeftClickEvent](() => cover.dispose())

              gui.addWidget(cover)
            })
          case _ =>
            panel.child("button_wireless").transform.doesDraw = false
            panel.child("text_wireless").transform.doesDraw = false
        }

      } else { // TODO
        // wProgPower.transform.doesDraw = false
      }
    }

    ret
  }

  private def drawLine(x0: Double, y0: Double, x1: Double, y1: Double, width: Double): (Double)=>Any = {
    val (dx, dy) = (x1 - x0, y1 - y0)
    val norm = math.sqrt(dx * dx + dy * dy)
    val (nx, ny) = (-dy/norm/2*width, dx/norm/2*width)

    (progress) => {
      val (xx, yy) = (lerp(x0, x1, progress), lerp(y0, y1, progress))

      RenderUtils.loadTexture(texLine)
      glColor4f(1, 1, 1, 1)

      glBegin(GL_QUADS)

      glTexCoord2d(0, 0)
      glVertex2d(x0 - nx, y0 - ny)

      glTexCoord2d(0, 1)
      glVertex2d(x0 + nx, y0 + ny)

      glTexCoord2d(1, 1)
      glVertex2d(xx + nx, yy + ny)

      glTexCoord2d(1, 0)
      glVertex2d(xx - nx, yy - ny)

      glEnd()
    }
  }

  private def normalize(x: Double, absmax: Double) = math.min(math.abs(x), absmax) * math.signum(x)

  private def blackCover(gui: CGui): Widget = {
    val ret = new Widget
    ret :+ new Cover
    ret.size(gui.getWidth, gui.getHeight)

    ret
  }

  private def skillViewArea(skill: Skill)
                           (implicit data: AbilityData, gui: CGui, developer: IDeveloper=null): Widget = {
    val ret = blackCover(gui)

    {
      val skillWid = new Widget
      skillWid.centered().size(50, 50)

      val learned = data.isSkillLearned(skill)
      var canClose = true

      val textArea = new Widget().size(0, 10).centered().pos(0, 25)
      if (learned) {
        skillWid.listens[FrameEvent](() => {
          drawActionIcon(skill.getHintIcon, 0, glow=false)
        })
        textArea.listens[FrameEvent](() => {
          FontBold.draw(skill.getDisplayName, 0, 3, foSkillTitle)
          Font.draw("Skill Experience: %.0f%%".format(data.getSkillExp(skill) * 100), 0, 15, foSkillProg)
          Font.drawSeperated(skill.getDescription, 0, 24, 200, foSkillDesc)
        })
      } else {
        var progress: Double = 0
        var learning = false
        var message: Option[String] = None

        skillWid.listens[FrameEvent](() => {
          drawActionIcon(skill.getHintIcon, progress, glow=progress == 1)
        })

        textArea.listens[FrameEvent](() => {
          FontBold.draw(skill.getDisplayName, 0, 3, foSkillTitle)
          Font.draw("Skill Not Learned", 0, 15, foSkillUnlearned)
        })

        if (developer != null) {
          val action = new DevelopActionSkill(skill)
          val estmCons = LearningHelper.getEstimatedConsumption(player, developer.getType, action)

          val conditions = skill.getDevConditions.toList.filter(_.shouldDisplay)
          val CondIconSize = 14
          val CondIconStep = 16
          val len = CondIconStep * conditions.size

          println(conditions)

          textArea.listens[FrameEvent](() => {
            Font.draw("Req.", -len/2 - 2, 26, foSkillReq)
          })

          case class CondTag(cond: IDevCondition, accepted: Boolean) extends Component("CondTag")

          conditions.zipWithIndex foreach { case (cond, idx) =>
            val widget = new Widget().size(CondIconSize, CondIconSize)
              .pos(-len/2 + CondIconStep * idx, 25).size(CondIconSize, CondIconSize)

            val tex = new DrawTexture(cond.getIcon)
            val accepted = cond.accepts(data, developer, skill)

            if (!accepted) {
              tex.setShaderId(shaderMono.getProgramID)
            }

            widget :+ tex

            widget :+ CondTag(cond, accepted)
            textArea :+ widget
          }

          textArea.listens[FrameEvent](() => {
            Option(gui.getHoveringWidget) match {
              case Some(w) =>
                val tag = Option(w.component[CondTag])
                tag match {
                  case Some(CondTag(cond, accepted)) =>
                    Font.draw(s"(${cond.getHintText})", len/2 + 3, 27, if (accepted) foSkillReqDetail else foSkillReqDetail2)
                  case _ =>
                }
              case _ =>
            }
          })

          textArea.listens[FrameEvent](() => message match {
            case Some(str) =>
              Font.draw(str, 0, 40, foSkillUnlearned2)
            case None =>
              Font.draw("Learn? (Estm. Consumption: " + estmCons + ")",
                0, 40, foSkillUnlearned2)
          })

          val button = new Widget()
            .size(64, 32).scale(.5).centered().pos(0, 55)
            .addComponent(new DrawTexture(texButton))
            .addComponent(new Tint(Color.monoBlend(1, .6), Color.monoBlend(1, 1), true))

          button.listens[LeftClickEvent](() => {
            if (developer.getEnergy < estmCons) {
              message = Some("Not enough energy.")
            } else if (!action.validate(player, developer)) {
              message = Some("Develop condition not satisfied.")
            } else {
              // start developing
              val devData = DevelopData.get(player)
              devData.reset()

              send(NetDelegate.MSG_START_SKILL, devData, developer, skill)
              ret.listens[FrameEvent](() => {
                devData.getState match {
                  case DevState.IDLE =>
                  case DevState.DEVELOPING =>
                    message = Some("Progress %.0f%%".format(devData.getDevelopProgress * 100))
                    progress = devData.getDevelopProgress
                  case DevState.FAILED =>
                    message = Some("Develop failed.")
                }
              })
            }

            button.dispose()
          })

          textArea :+ button
        }
      }

      ret :+ textArea
      ret :+ skillWid

      ret.listens[LeftClickEvent](() => if (canClose) {
        ret.component[Cover].end()
      })
    }

    ret
  }

  private def drawActionIcon(icon: ResourceLocation, progress: Double, glow: Boolean) = {
    val BackSize = 50
    val IconSize = 30
    val IconAlign = (BackSize - IconSize) / 2

    glPushMatrix()
    glTranslated(0, 0, 11)
    glColor4f(1, 1, 1, 1)

    RenderUtils.loadTexture(texSkillBack)
    HudUtils.rect(0, 0, BackSize, BackSize)

    RenderUtils.loadTexture(icon)
    HudUtils.rect(IconAlign, IconAlign, IconSize, IconSize)

    glUseProgram(shaderProg.getProgramID)

    glActiveTexture(GL_TEXTURE1)
    RenderUtils.loadTexture(texSkillMask)

    glActiveTexture(GL_TEXTURE0)
    RenderUtils.loadTexture(if (glow) texViewOutlineGlow else texViewOutline)

    glUniform1f(posProgress, progress.toFloat)
    HudUtils.rect(0, 0, BackSize, BackSize)

    glUseProgram(0)

    glPopMatrix()
  }


  class Cover extends Component("cover") {

    private var lastTransit = GameTimer.getTime
    private var ended: Boolean = false

    this.listens[FrameEvent](() => {
      val time = GameTimer.getTime
      val dt = time - lastTransit

      widget.transform.width = widget.getGui.getWidth
      widget.transform.height = widget.getGui.getHeight

      val src = clampd(0, 1, dt / 200.0)
      val alpha = if (ended) 1 - src else src

      glColor4d(0, 0, 0, alpha * 0.7)
      HudUtils.colorRect(0, 0, widget.transform.width, widget.transform.height)

      if (ended && alpha == 0) {
        widget.dispose()
      }

      widget.dirty = true
    })

    def end() = {
      ended = true
      lastTransit = GameTimer.getTime
    }

  }

  def newScreen(): CGuiScreen = new TreeScreen()

  private class TreeScreen extends CGuiScreen {
    // getGui.setDebug()

    override def doesGuiPauseGame = false
  }

  private def send(channel: String, pars: Any*) = NetworkMessage.sendToServer(NetDelegate, channel, pars.map(_.asInstanceOf[AnyRef]): _*)

}

@Registrant
private object NetDelegate {

  final val MSG_START_SKILL = "start_skill"
  final val MSG_GET_NODE = "get_node"

  @RegInitCallback
  def __init() = {
    NetworkS11n.addDirectInstance(NetDelegate)
  }

  @Listener(channel=MSG_START_SKILL, side=Array(Side.SERVER))
  private def hStartDevelop(data: DevelopData, developer: IDeveloper, skill: Skill) = {
    data.startDeveloping(developer, new DevelopActionSkill(skill))
  }

  @Listener(channel=MSG_GET_NODE, side=Array(Side.SERVER))
  private def hGetLinkNodeName(tile: TileDeveloper, future: Future[String]) = {
    future.sendResult(WirelessHelper.getNodeConn(tile) match {
      case null => null
      case conn => conn.getNode.getNodeName
    })
  }

}
