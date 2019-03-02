package cn.academy.crafting.client.ui

import cn.academy.Resources
import cn.academy.block.AbilityInterf
import cn.academy.block.container.ContainAbilityInterferer
import cn.academy.core.client.ui.TechUI.{ContainerUI, Page}
import cn.lambdalib2.cgui.Widget
import cn.lambdalib2.cgui.component.{Component, DrawTexture, ElementList, TextBox}
import cn.lambdalib2.cgui.component.TextBox.ConfirmInputEvent
import cn.lambdalib2.cgui.event.{FrameEvent, LeftClickEvent, LostFocusEvent}
import cn.lambdalib2.cgui.loader.CGUIDocument
import cn.lambdalib2.util.{Colors, MathUtils}
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import org.lwjgl.util.Color


@SideOnly(Side.CLIENT)
object GuiAbilityInterferer {

  import cn.lambdalib2.cgui.ScalaCGUI._
  import cn.academy.core.client.ui._
  import AbilityInterf._

  lazy val template = CGUIDocument.read(Resources.getGui("rework/page_interfere")).getWidget("main")

  val buttonOn = Resources.getTexture("guis/button/button_switch_on")
  val buttonOff = Resources.getTexture("guis/button/button_switch_off")

  def apply(container: ContainAbilityInterferer) = {
    val window = template.copy()
    val tile = container.tile

    {
      case class Element(playerName: String) extends Component("Element")
      class Area(var focus: Option[Widget]) extends Component("Area")

      val listPanel = window.child("panel_whitelist")
      val listArea = listPanel.child("zone_whitelist")

      val element = listArea.child("element")
      val area = new Area(None)

      listArea.removeWidget("element")
      listArea :+ area

      def update(whitelist: Iterable[String]) = {
        listArea.removeComponent(classOf[ElementList])
        area.focus = None

        val elist = new ElementList

        whitelist.foreach(name => {
          val instance = element.copy()
          val dt = instance.component[DrawTexture]
          dt.color.setAlpha(Colors.f2i(0.7f))

          instance.child("element_name").component[TextBox].content = name
          instance.listens[FrameEvent](() => dt.color.setAlpha(Colors.f2i(area.focus match {
            case Some(f) if f == instance => 1.0f
            case _ => 0.7f
          })))
          instance.listens[LeftClickEvent](() => area.focus = Some(instance))
          instance :+ new Element(name)

          elist.addWidget(instance)
        })

        listArea :+ elist
      }

      def sendUpdate(whitelist: Iterable[String]) = {
        tile.setWhitelistClient(whitelist, () => update(whitelist))
      }

      listPanel.child("btn_up").listens[LeftClickEvent](() => listArea.component[ElementList].progressLast())
      listPanel.child("btn_down").listens[LeftClickEvent](() => listArea.component[ElementList].progressNext())
      listPanel.child("btn_add").listens[LeftClickEvent](() => {
        val box = new Widget().size(40, 10).pos(50, 5)
          .addComponent(new DrawTexture(null).setColor(new Color(255, 255, 255, 50)))
          .addComponent(Resources.newTextBox().allowEdit())

        box.listens[ConfirmInputEvent](() => {
          box.component[TextBox].content match {
            case "" =>
            case str => sendUpdate(tile.whitelist + str)
          }
          box.dispose()
        })
        box.listens[LostFocusEvent](() => box.dispose())
        listPanel :+ box
        box.gainFocus()
      })
      listPanel.child("btn_remove").listens((w, e: LeftClickEvent) => {
        listArea.component[Area].focus match {
          case Some(widget) => {
            val name = widget.component[Element].playerName
            sendUpdate(tile.whitelist - name)
          }
          case None =>
        }
      })

      update(tile.whitelist)
    }

    {
      val button = window.child("panel_config/element_switch/element_btn_switch")
      val texture = button.component[DrawTexture]
      val color = texture.color
      var state = tile.enabled

      def setState(state2: Boolean) = {
        state = state2

        val lum = Colors.f2i(if (state) 1 else 0.6f)
        color.setRed(lum)
        color.setGreen(lum)
        color.setBlue(lum)

        texture.texture = if (state) buttonOn else buttonOff
      }

      setState(state)

      button.listens[LeftClickEvent](() => {
        tile.setEnabledClient(!state, () => setState(!state))
      })
      button.listens[FrameEvent](() => tile.enabled)
    }

    {
      val elemRange = window.child("panel_config/element_range")

      def updateRange(value: Double) = elemRange.child("element_text_range").component[TextBox].content = value.toString

      def handle(delta: Int) = () => {
        val newValue = MathUtils.clampd(minRange, maxRange, tile.range + delta)
        tile.setRangeClient(newValue, () => updateRange(newValue))
      }

      updateRange(tile.range)

      elemRange.child("element_btn_left").listens[LeftClickEvent](handle(-10))
      elemRange.child("element_btn_right").listens[LeftClickEvent](handle(10))
    }

    val invPage = InventoryPage(window)
    val wirelessPage: Page = WirelessPage.userPage(tile)
    val ret = new ContainerUI(container, invPage, wirelessPage)
    ret.infoPage.histogram(TechUI.histEnergy(() => tile.getEnergy, tile.getMaxEnergy))
    ret

  }
}