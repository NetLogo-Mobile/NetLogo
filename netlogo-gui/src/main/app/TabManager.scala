import org.nlogo.app.common.TabsInterface
//import javax.swing.JTabbedPane
import org.nlogo.app.codetab.{ CodeTab, MainCodeTab }

class TabManager( appTabs: TabsInterface, mainCodeTabPanel: TabsInterface) {
  def getAppTabs = appTabs
  def getMainCodeTabPanel = mainCodeTabPanel
  def getMainCodeTabOwner = mainCodeTabPanel

  def getCodeTabOwner(tab: CodeTab): TabsInterface = {
    if (tab.isInstanceOf[MainCodeTab]) getMainCodeTabOwner else getAppTabs
  }

  def getTabOwner(tab: Component): TabsInterface = {
    if (tab.isInstanceOf[MainCodeTab]) mainCodeTabOwner else getAppTabs
  }
}
