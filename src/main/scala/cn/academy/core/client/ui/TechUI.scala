package cn.academy.core.client.ui

import cn.academy.core.client.Resources
import cn.lambdalib.cgui.gui.Widget
import cn.lambdalib.cgui.gui.component.ProgressBar.Direction
import cn.lambdalib.cgui.gui.component.{ProgressBar, TextBox, ElementList, DrawTexture}
import cn.lambdalib.cgui.gui.component.Transform.{HeightAlign, WidthAlign}
import cn.lambdalib.cgui.gui.event.{FrameEvent, LeftClickEvent}
import cn.lambdalib.cgui.xml.CGUIDocument
import cn.lambdalib.util.client.font.IFont.FontOption
import cn.lambdalib.util.helper.Color
import net.minecraft.util.{StatCollector, ResourceLocation}
import cn.lambdalib.cgui.ScalaCGUI._
import scala.collection.JavaConversions._

object TechUI {

  private def readxml(loc: String) = CGUIDocument.panicRead(new ResourceLocation(s"academy:guis/rework/$loc.xml"))

  private val pageButtonTemplate = readxml("pageselect").getWidget("main")

  private val configPageTemplate = readxml("page_config").getWidget("main")

  private val wirelessPageTemplate = readxml("page_wireless").getWidget("main")

  case class Page(id: String, window: Widget)

  case class HistoElement(id: String, color: Color, progressProvider: () => Double)

  def textProperty(content: String, color: Color = Color.white()) = {
    val ret = new Widget().size(142, 12)
    ret :+ new TextBox(new FontOption(10, color)).setContent(content)
    ret
  }

  def createConfigPage(properties: Seq[Widget], histo: Seq[HistoElement]) = {
    val widget = configPageTemplate.copy()

    {
      val elist = new ElementList
      elist.spacing = 3.0
      properties.foreach(elist.addWidget)

      val panelConfig = widget.getWidget("panel_config")

      val area = panelConfig.getWidget("zone_elementlist")
      area :+ elist

      panelConfig.getWidget("btn_arrow_up").listens[LeftClickEvent](() => elist.progressLast())
      panelConfig.getWidget("btn_arrow_down").listens[LeftClickEvent](() => elist.progressNext())
    }

    {
      val panelDiagram = widget.getWidget("panel_diagram")
      val histZone = panelDiagram.getWidget("zone_histogram")
      val elemList = panelDiagram.getWidget("zone_elementlist")

      histo.zipWithIndex.foreach { case (elem, idx) =>
          val barX = 10 + idx * 15
          val bar = new Widget().halign(HeightAlign.BOTTOM).pos(barX, 0).size(10, 60)

          val progress = new ProgressBar().setDirection(Direction.UP).setFluctRegion(0)
          progress.color.from(elem.color)
          bar :+ progress
          bar.listens[FrameEvent](() => {
            progress.progress = elem.progressProvider()
          })

          histZone :+ bar

          val disp = elemList.getWidget("element").copy()
          disp.transform.y += 10 + idx * 15
          disp.transform.doesDraw = true

          disp.getWidget("element_mark").getComponent(classOf[DrawTexture]).color.from(elem.color)
          disp.getWidget("element_name").getComponent(classOf[TextBox])
            .setContent(StatCollector.translateToLocal("ac.gui.histogram." + elem.id))

          elemList :+ disp
      }
    }

    Page("config_2", widget)
  }

  def createWirelessPage() = {
    val widget = wirelessPageTemplate.copy()

    Page("wireless", widget)
  }

  /**
    * Creates a tech UI with specified pages.
    *
    * @param pages The pages of this UI. Must not be empty.
    */
  def create(pages: Page*): Widget = {
    val ret = new Widget
    ret.size(172, 187).centered()

    var currentPage = pages.head

    pages.zipWithIndex.foreach { case (page, idx) =>
      val button = pageButtonTemplate.copy()
      button.walign(WidthAlign.LEFT).halign(HeightAlign.TOP)

      val buttonTex = button.getComponent(classOf[DrawTexture])
      buttonTex.setTex(Resources.getTexture("guis/icons/icon_" + page.id))
      button.scale(0.7)
      button.pos(-20, idx * 22)
      button.listens[LeftClickEvent](() => {
        pages.foreach(_.window.transform.doesDraw = false)
        page.window.transform.doesDraw = true
        currentPage = page
      })
      button.listens((evt: FrameEvent) => {
        val a1 = if (evt.hovering || currentPage == page) 1.0 else 0.8
        val a2 = if (currentPage == page) 1.0 else 0.8
        buttonTex.color.a = a1
        buttonTex.color.r = a2
        buttonTex.color.g = a2
        buttonTex.color.b = a2
      })

      page.window.transform.doesDraw = false

      ret :+ button
      ret :+ page.window
    }

    pages.head.window.transform.doesDraw = true

    ret
  }



}
