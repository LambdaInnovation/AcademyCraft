/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.util.markdown

import java.util

import cn.lambdalib2.render.font.IFont.FontOption
import cn.lambdalib2.render.font.{IFont, TrueTypeFont}
import cn.lambdalib2.util.Fragmentor.IFontSizeProvider
import cn.lambdalib2.util.{Colors, Fragmentor, HudUtils, RenderUtils}
import cn.lambdalib2.util.markdown.MarkdownParser._
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

import scala.collection.JavaConversions._
import scala.collection._

/**
  * Renders markdown to legacy opengl.
  */
class GLMarkdownRenderer extends MarkdownRenderer {

  // Render parameters

  val headerPrefixes = List(1.0f, 1.2f, 1.4f, 1.6f, 1.8f).reverse.toArray

  var font: IFont = TrueTypeFont.defaultFont
  var boldFont: IFont = TrueTypeFont.defaultFont
  var italicFont: IFont  = TrueTypeFont.defaultFont

  var fontSize = 10.0f
  var lineSpacing = 4
  var widthLimit = Float.MaxValue

  var textColor = Colors.white()
  var refTextColor = Colors.fromHexColor(0xffe1c385)
  var refBackgroundColor = Colors.fromFloat(0.5f, 0.5f, 0.5f, 0.4f)

  def setFonts(_font: IFont, _boldFont: IFont, _italicFont: IFont) = {
    font = _font
    boldFont = _boldFont
    italicFont = _italicFont
  }

  //

  protected class Context {
    var x = 0.0f
    var y = 0.0f
    var lastSize = 0.0f
    var lineBegin = true

    def lineHead = x == 0.0f
  }

  protected val rc: Context = new Context

  //
  private val instructions = new util.ArrayList[() => Any]()
  private class TextInsr extends (() => Any) {

    var x: Float = 0
    var y: Float = 0
    var font: IFont = null
    var option: FontOption = null
    var txt: String = null

    def this(_txt: String, _x: Float, _y: Float, _font: IFont, _option: FontOption) = { this
      x = _x
      y = _y
      font = _font
      option = _option
      txt = _txt
    }

    override def apply() = font.draw(txt, x, y, option)
  }

  private def insr(ins: () => Any) = instructions.add(ins)
  //

  override def onTextContent(text: String, attr: Set[Attribute]) = {
    var usedFont = font
    var usedSize = fontSize
    var listElement = false
    var usedColor = textColor

    attr foreach {
      case ListElement() =>
        if (rc.lineBegin) {
          listElement = true
        }
      case Header(level) =>
        usedSize = fontSize * headerPrefixes(level)
        if (level <= 3) {
          usedFont = boldFont
        }
      case Emphasize() => usedFont = italicFont
      case Strong() => usedFont = boldFont
      case Reference() =>
        usedColor = refTextColor
        if (rc.lineHead) rc.x = usedSize
      case _ => // Ignore everything else
    }

    val option = new FontOption(usedSize, usedColor)

    val lines = Fragmentor.toMultiline(text, new IFontSizeProvider {
      override def getCharWidth(chr: Int): Double = usedFont.getCharWidth(chr, option)
      override def getTextWidth(str: String): Double = usedFont.getTextWidth(str, option)
    }, rc.x, widthLimit)

    if (listElement) {
      val dotSize = usedSize * 0.2f
      val indent = usedSize * 1.2f
      rc.x = indent
      val x = rc.x
      val y = rc.y
      insr(() => {
        GL11.glColor4f(1, 1, 1, 1)
        HudUtils.colorRect(x, y + usedSize / 2 - dotSize / 2, dotSize, dotSize)
      })
      rc.x += dotSize * 2
    }

    for (i <- 0 until lines.length) {
      val ln = lines(i)

      val width = usedFont.getTextWidth(ln, option)

      // Note: widthLimit * 1.2 is a magic number and this is a temporary hack.
      // should find a better way to handle line seperation.
      if (i != 0 || width + rc.x > widthLimit * 1.2) {
        newline(true)
      }

      rc.lastSize = option.fontSize

      insr(new TextInsr(ln, rc.x, rc.y, usedFont, option))
      rc.x += width
    }
  }

  private def newline(continue: Boolean = false) = {
    rc.x = 0.0f
    if (continue) {
      rc.y += rc.lastSize
    } else {
      rc.y += rc.lastSize + lineSpacing
      rc.lastSize = 0
    }

    rc.lineBegin = !continue
  }

  override def onNewline() = newline()

  override def onTag(name: String, attr: Map[String, String]) = {
    name match {
      case "img" =>

        val src = new ResourceLocation(attr("src"))
        val hover = attr.get("hover")
        var scale = if (attr.contains("scale")) attr("scale").toFloat else 1

        var size = if (attr.contains("width") && attr.contains("height")) {
          (attr("width").toFloat * scale, attr("height").toFloat * scale)
        } else {
          RenderUtils.loadTexture(src)
          val twidth: Float = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_WIDTH)
          val theight: Float = GL11.glGetTexLevelParameteri(GL11.GL_TEXTURE_2D, 0, GL11.GL_TEXTURE_HEIGHT)

          (twidth * scale, theight * scale)
        }

        if (size._1 + rc.x > widthLimit) {
          scale = (widthLimit - rc.x) / size._1
          size = (size._1 * scale, size._2 * scale)
        }

        val x = rc.x
        val y = if (size._2 >= rc.lastSize) rc.y else rc.y + rc.lastSize - size._2
        insr(() => {
          RenderUtils.loadTexture(src)
          GL11.glColor4f(1, 1, 1, 1)
          HudUtils.rect(x, y, size._1, size._2)
        })

        val newY = rc.y + math.max(0f, size._2 - fontSize)
        // Take all contents rendered in same line previously and alter their position
        // Note that this will be buggy when multiple images are introduced.
        // Maybe we should introduce line number to make this easy to proccess?
        instructions.reverse.drop(1).takeWhile(_.isInstanceOf[TextInsr]).foreach {
          case insr : TextInsr if insr.y == rc.y => insr.y = newY
          case _ =>
        }

        rc.x += size._1
        rc.y = newY
        rc.lastSize = fontSize

      case _ => // Ignore
    }
  }

  def render() = instructions foreach (_.apply())

  def getMaxHeight = rc.y + fontSize

}