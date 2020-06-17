// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app
import java.awt.{ Component }
import org.nlogo.app.common.TabsInterface
//import javax.swing.JTabbedPane
import org.nlogo.app.codetab.{ CodeTab, MainCodeTab }

class AppTabManager( appTabs: TabsInterface, mainCodeTabPanel: TabsInterface) {
  def getAppTabs = appTabs
  def getMainCodeTabPanel = mainCodeTabPanel
  def getMainCodeTabOwner = mainCodeTabPanel

  def getCodeTabOwner(tab: CodeTab): TabsInterface = {
    if (tab.isInstanceOf[MainCodeTab]) mainCodeTabPanel else appTabs
  }

  def getTabOwner(tab: Component): TabsInterface = {
    if (tab.isInstanceOf[MainCodeTab]) mainCodeTabPanel else appTabs
  }
}
