// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app
import java.awt.{ Component }
//import org.nlogo.app.common.TabsInterface
//import javax.swing.JTabbedPane
import org.nlogo.app.codetab.{ CodeTab, MainCodeTab }

class AppTabManager( val appTabs: AbstractTabs, val mainCodeTabPanel: AbstractTabs) {

  def printComponent(cmp: Component, description: String): Unit = {
    val pattern = """(^.*)\[(.*$)""".r
    val pattern(name, _) = cmp.toString
    val shortName = name.split("\\.").last
    println(description + System.identityHashCode(cmp) +
     ", " + shortName)
  }

printComponent(appTabs, "appTabs: ")
printComponent(mainCodeTabPanel, "mainCodeTabPanel: ")
  def getAppTabs = appTabs
  def getMainCodeTabPanel = mainCodeTabPanel
  def getMainCodeTabOwner = mainCodeTabPanel

  def getCodeTabOwner(tab: CodeTab): AbstractTabs = {
    printComponent(tab, "getCodeTabOwner: ")
    if (tab.isInstanceOf[MainCodeTab]) mainCodeTabPanel else appTabs
  }

  def getTabOwner(tab: Component): AbstractTabs = {
    printComponent(tab, "getTabOwner: ")
    if (tab.isInstanceOf[MainCodeTab]) mainCodeTabPanel else appTabs
  }
  def setSelectedCodeTab(tab: CodeTab): Unit = {
    printComponent(tab, "setSelectedCodeTab: ")
    getCodeTabOwner(tab).setSelectedComponent(tab)
  }
}
