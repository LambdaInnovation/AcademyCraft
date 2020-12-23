/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
package cn.lambdalib2.util.markdown

import cn.lambdalib2.util.markdown.MarkdownParser.Attribute

import scala.collection._

/**
  * Simple markdown dialect parser that uses recursion and regex heavily :-) Current supported syntax as follows:
  *
  *   * Header: 1~6 `#`s followed by text
  *   * List element: `*` followed by a ` ` followed by text
  *   * Emphasize: `**` followed by text followed by `**`
  *   * Strong: `__` followed by text followed by `__`
  *
  * Above are rules that works on texts. Anything that are not text are treated as **tags**. Currently you can specify
  * tag in two ways:
  *
  *   * Inline tag: `![tag_name attr1=val1 attr2=val2 ...]`
  *     - You can wrap the value name with quotes to avoid parse interrupting.
  *   * Image tag: `![hover](url)`
  *     - Which is directly translated to `![image src=<url> hover=<hover>`.
  *
  * After parsing, the elements are passed to the `MarkdownRenderer` instructively.
  */
object MarkdownParser {
  trait Attribute
  case class ListElement() extends Attribute
  case class Header(level: Int) extends Attribute
  case class Emphasize() extends Attribute
  case class Strong() extends Attribute
  case class Reference() extends Attribute

  trait Instruction
  case class Tag(name: String, attrs: Map[String, String]) extends Instruction
  case class Text(text: String, attrs: Set[Attribute]) extends Instruction

  def accept(content: String, target: MarkdownRenderer) = {
    content.lines foreach (ln => parseLine(ln)(target))
  }

  private def parseLine(ln : String)(implicit target: MarkdownRenderer) = {
    implicit val attributes = mutable.Set[Attribute]()
    if (ln.startsWith("#")) {
      val sharps = ln.takeWhile(_ == '#').length
      val level = math.min(6, sharps)
      attributes += Header(level)
      processSpan(ln.substring(sharps).dropWhile(_ == ' '))
      target.onNewline()
    } else if (ln.startsWith("* ")) {
      attributes += ListElement()
      processSpan(ln.substring(2))
      target.onNewline()
    } else if (ln.startsWith("> ")) {
      attributes += Reference()
      processSpan(ln.substring(2))
      // target.onNewline()
    } else {
      processSpan(ln)
    }
  }

  val star = raw"(.*)\*\*(.*)".r
  val underscore = raw"(.*)__(.*)".r
  val image = raw"(.*)!\[([^\[\]]*)\]\(([^\(\)]+)\)(.*)".r
  val inlineTag = raw"(.*)!\[([^\[\]]+)\](.*)".r

  private def processSpan(ln: String)
                         (implicit attrs: Set[Attribute],
                          target: MarkdownRenderer) = parseSpan(ln)(attrs) match {
    case Nil => target.onNewline()
    case list => list foreach {
      case Text(text, attrs) => target.onTextContent(text, attrs)
      case Tag(name, attrs) => target.onTag(name, attrs)
    }
  }

  val stringProperty = raw""" *([^=]+)= *"([^"]+)"(.*)""".r
  val property=raw" *([^=]+)= *([^ =]+)(.*)".r

  private def parseTag(content: String) = {
    def parseProperties(rest: String, prev: Map[String, String]): Map[String, String] = {
      rest match {
        case stringProperty(key, value, rest2) => parseProperties(rest2, prev + (key.trim -> value.trim))
        case property(key, value, rest2) => parseProperties(rest2, prev + (key.trim -> value.trim))
        case _ => prev
      }
    }
    val rwind = content.indexOf(' ')
    val ind = if (rwind == -1) content.length else rwind

    new Tag(content.substring(0, ind), parseProperties(content.substring(ind), Map()))
  }

  private def parseSpan(line: String)(implicit baseattr: Set[Attribute]): List[Instruction] = line match {
    case image(prev, hover, url, rest) =>
      parseSpan(prev) ::: Tag("img", Map("hover" -> hover, "src" -> url)) :: parseSpan(rest)
    case inlineTag(prev, content, rest) =>
      parseSpan(prev) ::: parseTag(content) :: parseSpan(rest)
    case star(star(prev, content), rest) =>
      parseSpan(prev) ::: Text(content, baseattr + Emphasize()) :: parseSpan(rest)
    case underscore(underscore(prev, content), rest) =>
      parseSpan(prev) ::: Text(content, baseattr + Strong()) :: parseSpan(rest)
    case full if !full.isEmpty => Text(full, baseattr) :: Nil
    case _ => Nil
  }

}

/**
  * Abstract markdown renderer. Accepts a series of instruction in sequence and behaves afterwards according to
  *  them.
  */
trait MarkdownRenderer {

  def onTextContent(text: String, attr: Set[Attribute])
  def onNewline()
  def onTag(name: String, attr: Map[String, String])

}