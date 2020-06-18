// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app
import java.awt.{ Component }
//import org.nlogo.app.common.TabsInterface
//import javax.swing.JTabbedPane
import org.nlogo.app.codetab.{ CodeTab, MainCodeTab }

class AppTabManager( val appTabs: AbstractTabs, val mainCodeTabPanel: AbstractTabs) {
  def getAppTabs = appTabs
  def getMainCodeTabPanel = mainCodeTabPanel
  def getMainCodeTabOwner = mainCodeTabPanel

  def getCodeTabOwner(tab: CodeTab): AbstractTabs = {
    if (tab.isInstanceOf[MainCodeTab]) mainCodeTabPanel else appTabs
  }

  def getTabOwner(tab: Component): AbstractTabs = {
    if (tab.isInstanceOf[MainCodeTab]) mainCodeTabPanel else appTabs
  }
  def setSelectedCodeTab(tab: CodeTab): Unit = {
    getCodeTabOwner(tab).setSelectedComponent(tab)
  }
}
