package cn.academy.tutorial.client

import cn.academy.AcademyCraft
import cn.academy.tutorial.TutorialData
import cn.academy.util.LocalHelper
import cn.lambdalib2.input.KeyManager
import cn.lambdalib2.util.SideUtils
import cn.lambdalib2.util.markdown.{GLMarkdownRenderer, MarkdownParser}

import scala.collection.Map

/**
  * This class renderers markdown with keyid display support.
  */
class ACMarkdownRenderer extends GLMarkdownRenderer {

  private val tutLocal = LocalHelper.at("ac.tutorial")

  override def onTag(name: String, attr: Map[String, String]) = {
    super.onTag(name, attr)
    def render(keyName: String) = onTextContent(keyName, Set(MarkdownParser.Reference()))

    if (name == "key") {
      val keyid = attr("id")
      val cfg = AcademyCraft.config
      if (cfg.hasKey("keys", keyid)) {
        render(KeyManager.getKeyName(cfg.get("keys", keyid, -1).getInt))
      } else {
        render("???")
      }
    }

    if (name == "misakaname") {
      val name = tutLocal.getFormatted("misaka",
        TutorialData.get(SideUtils.getThePlayer).getMisakaID.asInstanceOf[AnyRef])
      onTextContent(name, Set(MarkdownParser.Strong()))
    }
  }

}