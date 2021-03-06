// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.fileformat

import org.nlogo.api.{ ModelFormat, WorldDimensions3D }
import org.nlogo.core.{ View, UpdateMode, Model }
import scala.util.{ Failure, Success }
import org.nlogo.core.model.WidgetReader

class NLogoThreeDFormat
  extends ModelFormat[Array[String], NLogoThreeDFormat]
  with AbstractNLogoFormat[NLogoThreeDFormat] {
  val is3DFormat = true
  def name: String = "nlogo3d"
  override def widgetReaders =
    Map[String, WidgetReader]("GRAPHICS-WINDOW" -> ThreeDViewReader)

  override def isCompatible(location: java.net.URI): Boolean =
    sections(location) match {
      case Success(sections) =>
        sections("org.nlogo.modelsection.version")
          .find(_.contains("NetLogo 3D"))
          .flatMap(_ => Some(true)).getOrElse(false)
      case Failure(ex) => false
    }
  override def isCompatible(source: String): Boolean =
    sectionsFromSource(source) match {
      case Success(sections) =>
        sections("org.nlogo.modelsection.version")
          .find(_.contains("NetLogo 3D"))
          .flatMap(_ => Some(true)).getOrElse(false)
      case Failure(ex) => false
    }
  override def isCompatible(model: Model): Boolean =
    model.version.contains("3D")
  override lazy val defaultView: View = View(left = 210, top = 10, right = 649, bottom = 470,
    dimensions = new WorldDimensions3D(-16, 16, -16, 16, -16, 16, 13.0), fontSize = 10, updateMode = UpdateMode.Continuous,
    showTickCounter = true, frameRate = 30)
  }
