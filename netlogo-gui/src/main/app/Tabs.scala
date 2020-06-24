// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app
import java.awt.{ Component }
// CodeTab,
import org.nlogo.app.codetab.{ ExternalFileManager }
import org.nlogo.core.I18N
import org.nlogo.swing.{ TabsMenu }
import org.nlogo.window.{ GUIWorkspace }
import org.nlogo.app.interfacetab.InterfaceTab

class Tabs(workspace:       GUIWorkspace,
           interfaceTab:    InterfaceTab,
           menu:    MenuBar,
           externalFileManager: ExternalFileManager)
  extends AbstractTabs(workspace,
      interfaceTab, menu, externalFileManager) {
  // println(">Tabs")
  def init(manager: FileManager, monitor: DirtyMonitor, moreTabs: (String, Component) *) {
    println(" >Tabs.init")
    addTab(I18N.gui.get("tabs.run"), interfaceTab)
    addTab(I18N.gui.get("tabs.info"), infoTab)
    addAdditionalTabs(moreTabs: _*)
    tabActions = TabsMenu.tabActions(this)
    initManagerMonitor(manager, monitor)
    saveModelActions foreach menu.offerAction
    println(" <Tabs.init")
  }
}
