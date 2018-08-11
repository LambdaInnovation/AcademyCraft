package cn.lambdalib2.cgui

/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of LambdaLib modding library.
* https://github.com/LambdaInnovation/LambdaLib
* Licensed under MIT, see project root for more information.
*/
import cn.lambdalib2.cgui.component.Component
import cn.lambdalib2.cgui.event.{GuiEvent, IGuiEventHandler}

import scala.reflect.ClassTag

class RichWidget(val w: Widget) extends AnyVal {

  def listens[T <: GuiEvent](handler: (Widget, T) => Any, priority: Int = 0)(implicit evidence: ClassTag[T]): Widget = {
    w.listen[T](evidence.runtimeClass.asInstanceOf[Class[T]], priority, new IGuiEventHandler[T] {
      override def handleEvent(w: Widget, event: T) = {
        handler(w, event)
      }
    })
    w
  }

  def listens[T <: GuiEvent](handler: T => Any)(implicit evidence: ClassTag[T]): Widget = {
    listens((_, e: T) => handler(e))
    w
  }

  def listens[T <: GuiEvent](handler: () => Any)(implicit evidence: ClassTag[T]): Widget = {
    listens((_, _: T) => handler())
    w
  }

  def :+(add: Widget): Unit = w.addWidget(add)

  def :+(pair: (String, Widget)): Unit = w.addWidget(pair._1, pair._2)

  def :+(c: Component): Unit = w.addComponent(c)

  def component[T <: Component](implicit evidence: ClassTag[T]) = {
    w.getComponent(evidence.runtimeClass.asInstanceOf[Class[T]])
  }

  def child(name: String) = w.getWidget(name)

  def child(idx: Int) = w.getWidget(idx)

}

class RichComponent(val c: Component) extends AnyVal {
  def listens[T <: GuiEvent](handler: (Widget, T) => Any)(implicit tag: ClassTag[T]): Unit = {
    c.listen[T](tag.runtimeClass.asInstanceOf[Class[T]], new IGuiEventHandler[T] {
      override def handleEvent(w: Widget, e: T) = handler(w, e)
    })
  }

  def listens[T <: GuiEvent](handler: T => Any)(implicit tag: ClassTag[T]): Unit = {
    listens((_, e:T) => handler(e))
  }

  def listens[T <: GuiEvent](handler: () => Any)(implicit tag: ClassTag[T]): Unit = {
    listens((_, _:T) => handler())
  }
}

/**
  * CGUI scala extensions to reduce syntax burden.
  */
object ScalaCGUI {

  implicit def toWrapper(w: Widget): RichWidget = new RichWidget(w)

  implicit def toComponentWrapper(c: Component): RichComponent = new RichComponent(c)

}