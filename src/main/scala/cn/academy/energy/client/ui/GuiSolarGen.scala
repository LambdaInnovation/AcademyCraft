package cn.academy.energy.client.ui

import cn.academy.Resources
import cn.academy.block.container.ContainerSolarGen
import cn.academy.core.client.ui.{InventoryPage, TechUI, WirelessPage}
import cn.academy.block.tileentity.TileSolarGen.SolarStatus
import cn.lambdalib2.cgui.event.FrameEvent
import cn.lambdalib2.cgui.loader.CGUIDocument
import cn.lambdalib2.util.{HudUtils, RenderUtils}
import net.minecraft.client.gui.inventory.GuiContainer

object GuiSolarGen {
  import cn.academy.core.client.ui.TechUI._
  import cn.lambdalib2.cgui.ScalaCGUI._

  private lazy val template = CGUIDocument.read(Resources.getGui("rework/page_solar")).getWidget("main")
  private val texture = Resources.getTexture("guis/effect/effect_solar")

  def apply(container: ContainerSolarGen): GuiContainer = {
    val tile = container.tile

    val main = template.copy()

    val animFrame = main.child("ui_block/anim_frame")
    animFrame.listens[FrameEvent](() => {
      val v = tile.getStatus match {
        case SolarStatus.STOPPED => 1.0 / 3.0
        case SolarStatus.STRONG => 0.0
        case SolarStatus.WEAK => 2.0 / 3.0
      }
      RenderUtils.loadTexture(texture)
      HudUtils.rawRect(0, 0, 0, v,
        animFrame.transform.width,
        animFrame.transform.height,
        1, 1.0 / 3.0)
    })

    val invPage = InventoryPage(main)
    val wirelessPage = WirelessPage.userPage(tile)

    val ret = new ContainerUI(container, invPage, wirelessPage)

    ret.infoPage
      .histogram(TechUI.histBuffer(() => tile.getEnergy, tile.bufferSize))
      .seplineInfo()
      .property("gen_speed",f"${tile.getGeneration(1024)}%.2fIF/T")

    ret
  }

}