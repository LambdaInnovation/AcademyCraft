package cn.academy.ability.client.ui

import java.util
import java.util.function.Consumer

import cn.academy.{ACItems, AcademyCraft, Resources}
import cn.academy.ability.{AbilityLocalization, Skill}
import cn.lambdalib2.registry.StateEventCallback
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import cn.academy.ability.client.ui.Common.{Cover, RebuildEvent, TreeScreen}
import cn.academy.ability.develop.DevelopData.DevState
import cn.academy.ability.develop.action.{DevelopActionLevel, DevelopActionReset, DevelopActionSkill}
import cn.academy.ability.develop.condition.IDevCondition
import cn.academy.ability.develop.{DevelopData, DeveloperType, IDeveloper, LearningHelper}
import cn.academy.core.client.ui.{TechUI, WirelessPage}
import cn.academy.energy.api.WirelessHelper
import cn.academy.block.tileentity.TileDeveloper
import cn.academy.datapart.{AbilityData, CPData}
import cn.academy.util.LocalHelper
import cn.lambdalib2.cgui.{CGui, CGuiScreen, Widget}
import cn.lambdalib2.cgui.loader.CGUIDocument
import net.minecraft.client.Minecraft
import cn.lambdalib2.cgui.ScalaCGUI._
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent
import cn.lambdalib2.cgui.component.Transform.{HeightAlign, WidthAlign}
import cn.lambdalib2.cgui.component._
import cn.lambdalib2.cgui.event._
import cn.lambdalib2.s11n.network.NetworkMessage.Listener
import cn.lambdalib2.s11n.network.{Future, NetworkMessage, NetworkS11n}
import cn.lambdalib2.render.font.IFont.{FontAlign, FontOption}
import net.minecraft.util.EnumHand
import cn.lambdalib2.util.{HudUtils, RenderUtils}
import cn.lambdalib2.input.{KeyHandler, KeyManager}
import cn.lambdalib2.render.legacy.{LegacyShaderProgram, ShaderMono}
import org.lwjgl.input.Keyboard
import cn.lambdalib2.util.MathUtils._
import cn.lambdalib2.util.RandUtils
import cn.lambdalib2.util.{Colors, GameTimer}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import net.minecraft.util.{ChatAllowedCharacters, ResourceLocation}
import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.GL13._
import org.lwjgl.opengl.GL20._

import scala.collection.JavaConversions._
import scala.collection.mutable.ArrayBuffer

@SideOnly(Side.CLIENT)
object DeveloperUI {

  def apply(tile: IDeveloper): CGuiScreen = {
    val ret = new TreeScreen {
      override def onGuiClosed() = tile.onGuiClosed()
      // Close the link page if we are opening that, otherwise delegate down
      override def keyTyped(ch: Char, key: Int) = {
        if (key == Keyboard.KEY_ESCAPE) {
          Option(gui.getWidget("link_page")) match {
            case Some(page) => page.component[Cover].end()
            case None => super.keyTyped(ch, key)
          }
        } else {
          super.keyTyped(ch, key)
        }
      }
    }
    implicit val gui = ret.getGui

    def build() = {
      ret.getGui.clear()
      ret.getGui.addWidget("main", Common.initialize(tile))
    }

    gui.listen(classOf[RebuildEvent], new IGuiEventHandler[RebuildEvent] {
      override def handleEvent(w: Widget, event: RebuildEvent): Unit = build()
    })

    build()

    ret
  }

}

@SideOnly(Side.CLIENT)
object SkillTreeAppUI {
  def apply(): CGuiScreen = {
    val ret = Common.newScreen()
    implicit val gui = ret.getGui

    ret.getGui.addWidget(Common.initialize())

    ret
  }
}

@SideOnly(Side.CLIENT)
object SkillPosEditorUI {

  @StateEventCallback
  def __init(ev: FMLInitializationEvent) = {
    if (AcademyCraft.DEBUG_MODE) KeyManager.dynamic.addKeyHandler("skill_tree_pos_editor", Keyboard.KEY_RMENU, new KeyHandler {
      override def onKeyDown() = {
        Minecraft.getMinecraft.displayGuiScreen(SkillPosEditorUI())
      }
    })
  }

  def apply(): CGuiScreen = {
    val ret = Common.newScreen()
    implicit val gui = ret.getGui

    def build() = {
      gui.clear()

      val main = Common.initialize()

      ret.getGui.addWidget(main)

      main.removeWidget("parent_left")

      val aData = AbilityData.get(Minecraft.getMinecraft.player)
      if (aData.hasCategory) aData.getCategory.getSkillList.zipWithIndex foreach { case (skill, idx) =>
        val y = 5 + idx * 12
        val box0 = new Widget().size(40, 10).pos(20, y)
          .addComponent(Resources.newTextBox(new FontOption(8)).setContent(skill.getName))

        def box(init: Double, callback: Double => Any) = {
          val text = Resources.newTextBox(new FontOption(8)).setContent(init.toString)
          text.allowEdit()

          val ret = new Widget().size(20, 10)
            .addComponent(new DrawTexture().setTex(null).setColor(Colors.fromFloat(.3f, .3f, .3f, .3f)))
            .addComponent(text)
            .listens((evt: ConfirmInputEvent) => {
              try {
                val num = text.content.toDouble
                callback(num)
                gui.postEvent(new RebuildEvent)
              } catch {
                case _: NumberFormatException =>
              }
            })

          ret
        }

        val box1 = box(skill.guiX, newX => skill.guiX = newX.toFloat).pos(70, y)
        val box2 = box(skill.guiY, newY => skill.guiY = newY.toFloat).pos(93, y)

        gui.addWidget(box0)
        gui.addWidget(box1)
        gui.addWidget(box2)
      }
    }

    build()
    gui.listen(classOf[RebuildEvent], new IGuiEventHandler[RebuildEvent] {
      override def handleEvent(w: Widget, event: RebuildEvent): Unit = build()
    })

    ret
  }

}

private object Common {

  private lazy val template = CGUIDocument.read(Resources.getGui("rework/page_developer")).getWidget("main")

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
  private val foSkillProg = new FontOption(8, FontAlign.CENTER, Colors.fromHexColor(0xffa1e1ff))
  private val foSkillUnlearned = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xffff5555))
  private val foSkillUnlearned2 = new FontOption(10, FontAlign.CENTER, Colors.fromHexColor(0xaaffffff))
  private val foSkillReq = new FontOption(9, FontAlign.RIGHT, Colors.fromHexColor(0xaaffffff))
  private val foSkillReqDetail = new FontOption(9, FontAlign.LEFT, Colors.fromHexColor(0xeeffffff))
  private val foSkillReqDetail2 = new FontOption(9, FontAlign.LEFT, Colors.fromHexColor(0xffee5858))
  private val foLevelTitle = new FontOption(12, FontAlign.CENTER)
  private val foLevelReq = new FontOption(9, FontAlign.CENTER)

  private val Font = Resources.font()
  private val FontBold = Resources.fontBold()

  private val shaderProg = new LegacyShaderProgram
  shaderProg.linkShader(Resources.getShader("skill_progbar.frag"), GL_FRAGMENT_SHADER)
  shaderProg.linkShader(Resources.getShader("skill_progbar.vert"), GL_VERTEX_SHADER)
  shaderProg.compile()

  private val shaderMono = ShaderMono.instance()

  private val posProgress = shaderProg.getUniformLocation("progress")

  private val local = LocalHelper.at("ac.skill_tree")

  shaderProg.useProgram()

  {
    glUniform1i(shaderProg.getUniformLocation("texCircle"), 0)
    glUniform1i(shaderProg.getUniformLocation("texGradient"), 1)
    glUniform1f(posProgress, 0.7f)
  }
  glUseProgram(0)

  // This event is posted on global GuiEventBus to query for widget reload. Each gui instance must by itself respond to it.
  class RebuildEvent extends GuiEvent

  def player = Minecraft.getMinecraft.player

  def initialize(developer: IDeveloper = null)(implicit gui: CGui): Widget = {
    val ret = template.copy()

    implicit val aData = AbilityData.get(player)
    implicit val developer_ = developer
    implicit val devData = DevelopData.get(player)

    val area = ret.child("parent_right/area")

    if (!aData.hasCategory) {
      initConsole(area)
    } else if (Option(player.getHeldItem(EnumHand.MAIN_HAND)).exists(_.getItem == ACItems.magnetic_coil)) {
      initReset(area)
    } else { // Initialize skill area
      val back_scale = 1.01
      val back_scale_inv = 1 / back_scale
      val max_du = back_scale - 1
      val max_du_skills = 10

      var (dx, dy) = (0.0f, 0.0f)

      area.listens((evt: FrameEvent) => {
        val gui = area.getGui

        // Update delta
        def scale(x: Double) = (x - 0.5) * back_scale_inv + 0.5

        dx = clampf(0, 1, gui.getMouseX / gui.getWidth) - 0.5f
        dy = clampf(0, 1, gui.getMouseY / gui.getHeight) - 0.5f

        // Draw background
        RenderUtils.loadTexture(texAreaBack)
        HudUtils.rawRect(0, 0, scale(dx * max_du), scale(dy * max_du),
          area.transform.width, area.transform.height,
          back_scale_inv, back_scale_inv)
      })

      if (aData.hasCategory) {
        val skills = aData.getCategory.getSkillList.toList
          .filter(skill => LearningHelper.canBePotentiallyLearned(aData, skill))
          .filter(_.isEnabled)

        skills.zipWithIndex.foreach { case (skill, idx) =>
          val StateIdle = 0
          val StateHover = 1
          val TransitTime = 0.1

          val WidgetSize = 16.0f
          val ProgSize = 31.0f
          val TotalSize = 23.0f
          val IconSize = 14.0f
          val ProgAlign = (TotalSize - ProgSize) / 2
          val Align = (TotalSize - IconSize) / 2
          val DrawAlign = (WidgetSize - TotalSize) / 2

          val learned = aData.isSkillLearned(skill)

          val widget = new Widget
          val (sx, sy) = (skill.guiX, skill.guiY)

          var lastTransit = GameTimer.getTime - 2
          var state = StateIdle
          val creationTime = GameTimer.getTime
          val blendOffset = idx * 0.08 + 0.1

          val mAlpha = (learned, if (skill.getParent == null) true else aData.isSkillLearned(skill.getParent)) match {
            case (true, _)  => 1.0
            case (_, true)  => 0.7
            case (_, false) => 0.25
          }

          val lineDrawer = Option(skill.getParent).map(parent => {
            def center(x: Double, y: Double) = (x + WidgetSize / 2, y + WidgetSize / 2)

            val (cx, cy) = center(skill.guiX, skill.guiY)
            val (pcx, pcy) = center(parent.guiX, parent.guiY)
            val (px, py) = (pcx - cx, pcy - cy)
            val norm = math.sqrt(px * px + py * py)
            val (dx, dy) = (px/norm*12.2, py/norm*12.2)

            drawLine(px + WidgetSize / 2 - dx, py + WidgetSize / 2 - dy,
              WidgetSize / 2 + dx, WidgetSize / 2 + dy, 5.5, mAlpha * (if (learned) 1.0 else 0.4))
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

            val dt = math.max(0, time - creationTime - blendOffset)
            val backAlpha = mAlpha * clampd(0, 1, dt * 10.0)
            val iconAlpha = mAlpha * clampd(0, 1, (dt - 0.08) * 10.0)
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
            glColor4d(0.2, 0.2, 0.2, backAlpha * 0.6)
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
            if (!learned) {
              glUseProgram(shaderMono.getProgramID)
            }
            HudUtils.rect(Align, Align, IconSize, IconSize)
            glUseProgram(0)
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
              val texture1Binding = glGetInteger(GL_TEXTURE_BINDING_2D)
              RenderUtils.loadTexture(texSkillMask)

              HudUtils.rect(ProgAlign, ProgAlign, ProgSize, ProgSize)

              glBindTexture(GL_TEXTURE_2D, texture1Binding)
              glActiveTexture(GL_TEXTURE0)

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

      val (icon, name, prog) = Option(aData.getCategoryNullable) match {
        case Some(cat) => (cat.getDeveloperIcon, cat.getDisplayName, math.max(0.02f, aData.getLevelProgress))
        case None => (Resources.getTexture("guis/icons/icon_nocategory"), "N/A", 0.0f)
      }

      panel.child("logo_ability").component[DrawTexture].setTex(icon)
      panel.child("text_abilityname").component[TextBox].setContent(name)
      panel.child("logo_progress").component[ProgressBar].progress = prog
      panel.child("text_level").component[TextBox].setContent(AbilityLocalization.instance.levelDesc(aData.getLevel))

      {
        val cpData = CPData.get(player)
        panel.child("text_exp").component[TextBox].setContent("EXP " + (aData.getLevelProgress * 100).toInt+"%")

      }

      if (developer != null && aData.hasCategory && LearningHelper.canLevelUp(developer.getType, aData)) {
        val btn = panel.child("btn_upgrade")
        btn.transform.doesDraw = true
        btn.listens[LeftClickEvent](() => {
          val cover = levelUpArea

          gui.addWidget(cover)
        })
        panel.removeWidget("text_level")
      }
    }

    { // Initialize machine panel
      val panel = ret.child("parent_left/panel_machine")

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
            send(NetDelegate.MSG_GET_NODE, tile, Future.create(new Consumer[String]{
              override def accept(result: String): Unit = {panel.child("button_wireless/text_nodename").component[TextBox].content = if (result != null) result else "N/A"}
            }))
            panel.child("button_wireless").listens[LeftClickEvent](() => {
              val wirelessPage = WirelessPage.userPage(tile).window.centered()
              val cover = blackCover(gui)
              cover :+ wirelessPage

              cover.listens[LeftClickEvent](() => cover.component[Cover].end())
              cover.listens[CloseEvent](() => gui.postEvent(new RebuildEvent))

              gui.addWidget("link_page", cover)
            })
          case _ =>
            panel.child("button_wireless").transform.doesDraw = false
            panel.child("text_wireless").transform.doesDraw = false
        }

      } else {
        ret.child("parent_left/ui_left").component[DrawTexture].setTex(Resources.getTexture("guis/ui/ui_developerleft_skilltree"))
        panel.transform.doesDraw = false
      }
    }

    ret
  }

  private def drawLine(x0: Double, y0: Double, x1: Double, y1: Double,
                       width: Double, alpha: Double): (Double)=>Any = {
    val (dx, dy) = (x1 - x0, y1 - y0)
    val norm = math.sqrt(dx * dx + dy * dy)
    val (nx, ny) = (-dy/norm/2*width, dx/norm/2*width)

    (progress) => {
      val (xx, yy) = (lerp(x0, x1, progress), lerp(y0, y1, progress))

      RenderUtils.loadTexture(texLine)
      glColor4d(1, 1, 1, alpha)

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

  private def levelUpArea(implicit data: AbilityData, gui: CGui, developer: IDeveloper): Widget = {
    val ret = blackCover(gui)

    {
      val wid = new Widget
      wid.centered().size(50, 50)

      val action = new DevelopActionLevel()
      val estmCons = LearningHelper.getEstimatedConsumption(player, developer.getType, action)

      val textArea = new Widget().size(0, 10).centered().pos(0, 25)

      var hint = local.get("level_question")
      var progress: Double = 0
      var canClose: Boolean = true
      var shouldRebuild = false

      val icon = Resources.getTexture("abilities/condition/any" + (data.getLevel+1))

      wid.listens[FrameEvent](() => {
        drawActionIcon(icon, progress, glow = progress == 1)
      })

      val lvltext = local.getFormatted("uplevel", AbilityLocalization.instance.levelDesc(data.getLevel+1))
      val reqtext = local.get("req") + " %.0f".format(estmCons)
      textArea.listens[FrameEvent](() => {
        Font.draw(lvltext, 0, 3, foLevelTitle)
        Font.draw(reqtext, 0, 16, foLevelReq)
        Font.draw(hint, 0, 26, foLevelReq)
      })

      val button = newButton().centered().pos(0, 40)
      button.listens[LeftClickEvent](() => {
        if (developer.getEnergy < estmCons) {
          hint = local.get("noenergy")
        } else {
          val devData = DevelopData.get(player)
          devData.reset()
          canClose = false

          send(NetDelegate.MSG_START_LEVEL, devData, developer)
          ret.listens[FrameEvent](() => devData.getState match {
            case DevState.IDLE =>

            case DevState.DEVELOPING =>
              hint = local.get("dev_developing")
              progress = devData.getDevelopProgress

            case DevState.DONE =>
              hint = local.get("dev_successful")
              progress = 1
              canClose = true
              shouldRebuild = true

            case DevState.FAILED =>
              hint = local.get("dev_failed")
              canClose = true
          })
        }

        button.dispose()
      })

      textArea :+ button
      ret :+ textArea
      ret.listens[LeftClickEvent](() => {
        if (canClose) {
          if (shouldRebuild) {
            gui.postEvent(new RebuildEvent)
          } else {
            ret.component[Cover].end()
          }
        }
      })
      ret :+ wid
    }

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
      var shouldRebuild = false

      val textArea = new Widget().size(0, 10).centered().pos(0, 25)
      if (learned) {
        skillWid.listens[FrameEvent](() => {
          drawActionIcon(skill.getHintIcon, 0, glow=false)
        })
        textArea.listens[FrameEvent](() => {
          FontBold.draw(skill.getDisplayName, 0, 3, foSkillTitle)
          Font.draw(local.get("skill_exp") + (data.getSkillExp(skill) * 100).toInt + "%", 0, 15, foSkillProg)
          Font.drawSeperated(skill.getDescription, 0, 24, 200, foSkillDesc)
        })
      } else {
        var progress: Double = 0
        var message: Option[String] = None

        skillWid.listens[FrameEvent](() => {
          drawActionIcon(skill.getHintIcon, progress, glow=progress == 1)
        })

        val skillNameText = skill.getDisplayName + s" (LV ${skill.getLevel})"
        textArea.listens[FrameEvent](() => {
          FontBold.draw(skillNameText, 0, 3, foSkillTitle)
          Font.draw(local.get("skill_not_learned"), 0, 15, foSkillUnlearned)
        })

        if (developer != null) {
          val action = new DevelopActionSkill(skill)
          val estmCons = LearningHelper.getEstimatedConsumption(player, developer.getType, action)

          val conditions = skill.getDevConditions.toList.filter(_.shouldDisplay)
          val CondIconSize = 14
          val CondIconStep = 16
          val len = CondIconStep * conditions.size

          textArea.listens[FrameEvent](() => {
            Font.draw(local.get("req"), -len/2 - 2, 28, foSkillReq)
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
              Font.draw(local.get("learn_question").format("%.0f".format(estmCons)),
                0, 40, foSkillUnlearned2)
          })

          val button = newButton().centered().pos(0, 55)

          button.listens[LeftClickEvent](() => {
            if (developer.getEnergy < estmCons) {
              message = Some(local.get("noenergy"))
            } else if (skill.getLevel > data.getLevel) {
              message = Some(local.getFormatted("level_fail", skill.getLevel.asInstanceOf[AnyRef]))
            } else if (!action.validate(player, developer)) {
              message = Some(local.get("condition_fail"))
            } else {
              // start developing
              val devData = DevelopData.get(player)
              devData.reset()

              send(NetDelegate.MSG_START_SKILL, devData, developer, skill)
              canClose = false
              ret.listens[FrameEvent](() => {
                devData.getState match {
                  case DevState.IDLE =>
                  case DevState.DEVELOPING =>
                    message = Some(local.get("progress") + " %.0f%%".format(devData.getDevelopProgress * 100))
                    progress = devData.getDevelopProgress
                  case DevState.DONE =>
                    message = Some(local.get("dev_successful"))
                    shouldRebuild = true
                    progress = 1.0
                    canClose = true

                  case DevState.FAILED =>
                    canClose = true
                    message = Some(local.get("dev_failed"))
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
        if (shouldRebuild) {
          gui.postEvent(new RebuildEvent)
        } else {
          ret.component[Cover].end()
        }
      })
    }

    ret
  }

  private def newButton() = new Widget()
    .size(64, 32).scale(.5f)
    .addComponent(new DrawTexture(texButton))
    .addComponent(new Tint(Colors.monoBlend(1, .6f), Colors.monoBlend(1, 1), true))

  private def drawActionIcon(icon: ResourceLocation, progress: Double, glow: Boolean) = {
    val BackSize = 50
    val IconSize = 27
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
    val texture1Binding = glGetInteger(GL_TEXTURE_BINDING_2D)
    RenderUtils.loadTexture(texSkillMask)

    glActiveTexture(GL_TEXTURE0)
    RenderUtils.loadTexture(if (glow) texViewOutlineGlow else texViewOutline)

    glUniform1f(posProgress, progress.toFloat)
    HudUtils.rect(0, 0, BackSize, BackSize)

    glActiveTexture(GL_TEXTURE1)
    glBindTexture(GL_TEXTURE_2D, texture1Binding)
    glActiveTexture(GL_TEXTURE0)

    glUseProgram(0)

    glPopMatrix()
  }

  private def fmt(x: Int): String = if (x < 10) "0" + x else x.toString

  private def initConsole(area: Widget)(implicit data: DevelopData, developer: IDeveloper) = {
    implicit val console = new Console(false, developer != null)

    if (developer != null) {
      console += Command("learn", () => {

        console.enqueue(printTask(Console.localized("dev_begin")))
        console.enqueue(printTask(Console.localized("progress", fmt(0))))
        send(NetDelegate.MSG_START_LEVEL, data, developer)
        data.reset()

        console.enqueue(new Task {
          override def isFinished: Boolean = data.getState == DevState.FAILED || data.getState == DevState.DONE
          override def update() = {
            console.output("\b\b\b" + fmt((data.getDevelopProgress*100).toInt) + "%")
          }
          override def finish() = {
            console.outputln()
            if (data.getState == DevState.DONE) {
              console.output(Console.localized("dev_succ"))
            } else {
              console.output(Console.localized("dev_fail"))
            }

            console.pause(0.5)
            console.enqueueRebuild()
          }
        })
      })
    }

    area :+ console
  }

  private def initReset(area: Widget)(implicit data: DevelopData, developer: IDeveloper) = {
    implicit val console = new Console(true, true)

    console += Command("reset", () => {
      if (DevelopActionReset.canReset(data.getEntity, developer)) {
        console.enqueue(printTask(Console.localized("reset_begin")))
        console.enqueue(printTask(Console.localized("progress", fmt(0))))
        send(NetDelegate.MSG_RESET, data, developer)
        data.reset()

        console.enqueue(new Task {
          override def isFinished: Boolean = data.getState == DevState.FAILED || data.getState == DevState.DONE
          override def update() = {
            console.output("\b\b\b" + fmt((data.getDevelopProgress*100).toInt) + "%")
          }
          override def finish() = {
            console.outputln()
            if (data.getState == DevState.DONE) {
              console.output(Console.localized("reset_succ"))
            } else {
              console.output(Console.localized("reset_fail"))
            }

            console.pause(0.5)
            console.enqueueRebuild()
          }
        })
      } else {
        if (developer.getType != DeveloperType.ADVANCED) {
          console.enqueue(printTask(Console.localized("reset_fail_dev")))
        } else {
          console.enqueue(printTask(Console.localized("reset_fail_other")))
        }
      }
    })

    area :+ console
  }

  class CloseEvent extends GuiEvent

  class Cover extends Component("cover") {

    private var lastTransit = GameTimer.getTime
    private var ended: Boolean = false

    this.listens[FrameEvent](() => {
      val time = GameTimer.getTime
      val dt = time - lastTransit

      widget.transform.width = widget.getGui.getWidth
      widget.transform.height = widget.getGui.getHeight

      val src = clampd(0, 1, dt / 0.2)
      val alpha = if (ended) 1 - src else src

      glColor4d(0, 0, 0, alpha * 0.7)
      HudUtils.colorRect(0, 0, widget.transform.width, widget.transform.height)

      if (ended && alpha == 0) {
        widget.post(new CloseEvent)
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

  class TreeScreen extends CGuiScreen {
    // getGui.setDebug()

    override def doesGuiPauseGame = false
  }

  object Console {
    private val MaxLines = 10
    private val FO = new FontOption(8)

    private val ConsoleHead = "OS >"

    private val consoleLocal = local.subPath("console")

    def localized(id: String, args: AnyRef*) = consoleLocal.getFormatted(id, args: _*).replace("\\n", "\n")
  }

  class Console(val emergency: Boolean, val hasDeveloper: Boolean)
    extends Component("Console") {
    import Console._

    private implicit val _self = this

    private val inputTask = new Task {
      override def finish(): Unit = {}
      override def update(): Unit = {}
      override def isFinished: Boolean = false
      override def begin(): Unit = {
        output(ConsoleHead)
      }
    }

    private val commands = new ArrayBuffer[Command]()
    private val outputs: util.Deque[String] = new util.LinkedList[String]()
    private val taskQueue: util.Queue[Task] = new util.LinkedList[Task]()
    private var currentTask: Task = null
    private var input: String = ""

    enqueue(slowPrintTask(localized("init", player.getName)))
    pause(0.4)

    val numSeq =  (1 to 6).map(_ * 10 + RandUtils.nextInt(6) - 3).map(_ + "%").toList :::
      ((64 + RandUtils.nextInt(4)) + "%") :: localized("boot_failed") :: Nil

    animSequence(0.3, numSeq: _*)

    {
      val startupText: String = (emergency, hasDeveloper) match {
        case (true, _)  => localized("override")
        case (_, true)  => localized("invalid_cat") + localized("learn_hint")
        case (_, false) => localized("invalid_cat")
      }
      enqueue(slowPrintTask(startupText))
    }

    this.listens[FrameEvent](() => {
      if (currentTask != null && currentTask.isFinished) {
        currentTask.finish()
        currentTask = null
      }

      if (currentTask != null) {
        currentTask.update()
      }

      if (currentTask == null) {
        if (taskQueue.isEmpty) {
          currentTask = inputTask
        } else {
          currentTask = taskQueue.remove()
        }
        currentTask.begin()
      }

      val x = 5
      val font = Resources.font()
      var y = 5

      outputs.zipWithIndex foreach { case (line, idx) =>
        if (idx == outputs.size() - 1 && currentTask == inputTask) {
          font.draw(line + input + (if (((GameTimer.getTime*1000).toInt % 1000) < 500) "_" else ""), x, y, FO)
        } else {
          font.draw(line, x, y, FO)
        }

        y += 10
      }

      if (this.widget.getGui.getWidget("link_page") == null) {
        widget.gainFocus()
      }
    })

    this.listens((evt: KeyEvent) => {
      if (evt.keyCode == Keyboard.KEY_BACK) {
        if (input.length > 0) {
          input = input.substring(0, input.length - 1)
        }
      } else if (evt.keyCode == Keyboard.KEY_RETURN || evt.keyCode == Keyboard.KEY_NUMPADENTER) {
        parseCommand(input)
        input = ""
      } else if (ChatAllowedCharacters.isAllowedCharacter(evt.inputChar)) {
        input += evt.inputChar
      }
    })

    def enqueue(task: Task) = {
      taskQueue.offer(task)
      if (currentTask == inputTask) {
        outputln(input)
        currentTask = null
      }
    }

    def output(content: String) = {
      def refresh() = new StringBuilder(if (outputs.isEmpty) "" else outputs.removeLast())

      var current = refresh()

      def flush() = {
        outputs.addLast(current.toString())
      }

      for (ch <- content) ch match {
        case '\b' => current.setLength(math.max(0, current.length - 1))
        case '\n' =>
          flush()
          outputs.addLast("")
          current = refresh()
        case _ => current += ch
      }

      flush()

      while (outputs.size > MaxLines) outputs.removeFirst()
    }

    def outputln(content: String) = {
      output(content + '\n')
    }

    def outputln() = output("\n")

    def animSequence(time: Double, strs: String*) = {
      for ((s, idx) <- strs.zipWithIndex) {
        enqueue(new TimedTask {
          override def life = time

          override def finish(): Unit = if (idx != strs.length - 1) {
            output("\b" * s.length)
          }

          override def update(): Unit = {}

          override def begin(): Unit = {
            super.begin()
            output(s)
          }
        })
      }
    }

    def pause(time: Double) = enqueue(new TimedTask {
      override def life: Double = time
    })

    def enqueueRebuild() = enqueue(new Task {
      override def isFinished: Boolean = true
      override def begin(): Unit = widget.getGui.postEvent(new RebuildEvent)
    })

    def += (command: Command) = {
      commands += command
    }

    private def parseCommand(cmd: String) = {
      commands.filter(_.name == cmd).toList match {
        case Nil => enqueue(printTask(localized("invalid_command")))
        case command :: _ => command.callback()
      }
    }

  }

  case class Command(name: String, callback: ()=>Any)
  trait Task {
    def begin(): Unit = {}
    def update(): Unit = {}
    def finish(): Unit = {}

    def isFinished: Boolean
  }
  trait TimedTask extends Task {
    def life: Double

    private var creationTime: Double = -1

    def getCreationTime = creationTime

    override def begin() = creationTime = GameTimer.getTime

    override def isFinished = (GameTimer.getTime - creationTime) >= life
  }

  def printTask(str: String)(implicit console: Console): Task = new Task {
    override def begin(): Unit = {
      console.output(str)
    }
    override def isFinished = true
  }

  def slowPrintTask(str: String)(implicit console: Console): Task = new Task {
    val PerCharTime = 0.01

    private var idx = 0
    private var last :Double = -1

    override def finish(): Unit = {}

    override def begin(): Unit = {
      last = GameTimer.getTime
    }

    override def update(): Unit = {
      val time = GameTimer.getTime
      val n = ((time - last) / PerCharTime).toInt
      if (n > 0) {
        val end = math.min(str.length, idx + n)
        console.output(str.substring(idx, end))

        last += n * PerCharTime
        idx = end
      }
    }

    override def isFinished: Boolean = idx == str.length
  }

  private def send(channel: String, pars: Any*) = NetworkMessage.sendToServer(NetDelegate, channel, pars.map(_.asInstanceOf[AnyRef]): _*)

}

private object NetDelegate {

  final val MSG_START_SKILL = "start_skill"
  final val MSG_GET_NODE = "get_node"
  final val MSG_RESET = "reset"
  final val MSG_START_LEVEL = "start_level"

  @StateEventCallback
  def __init(ev: FMLInitializationEvent) = {
    NetworkS11n.addDirectInstance(NetDelegate)
  }

  @Listener(channel=MSG_START_SKILL, side=Array(Side.SERVER))
  private def hStartSkill(data: DevelopData, developer: IDeveloper, skill: Skill) = {
    data.startDeveloping(developer, new DevelopActionSkill(skill))
  }

  @Listener(channel=MSG_START_LEVEL, side=Array(Side.SERVER))
  private def hStartLevel(data: DevelopData, developer: IDeveloper) = {
    data.startDeveloping(developer, new DevelopActionLevel())
  }

  @Listener(channel=MSG_GET_NODE, side=Array(Side.SERVER))
  private def hGetLinkNodeName(tile: TileDeveloper, future: Future[String]) = {
    future.sendResult(WirelessHelper.getNodeConn(tile) match {
      case null => null
      case conn => conn.getNode.getNodeName
    })
  }

  @Listener(channel=MSG_RESET, side=Array(Side.SERVER))
  private def hStartReset(data: DevelopData, developer: IDeveloper) = {
    data.startDeveloping(developer, new DevelopActionReset)
  }

}