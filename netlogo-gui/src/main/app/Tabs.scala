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
  println(">Tabs")

  override def init(manager: FileManager, monitor: DirtyMonitor, moreTabs: (String, Component) *) {
    println("   >Tabs.init")
        //  println("      Thread.currentThread: " +  Thread.currentThread)
    //  println("      add interface tab")
    addTab(I18N.gui.get("tabs.run"), interfaceTab)
    //  println("      Tabs info")
    addTab(I18N.gui.get("tabs.info"), infoTab)
    // aab println("      Tabs code")
    // aab addTab(I18N.gui.get("tabs.code"), codeTab)
    //  println("      Tabs add more")
    for((name, tab) <- moreTabs)
      addTab(name, tab)
      println("      Tabs have been added")
    tabActions = TabsMenu.tabActions(this)
    fileManager = manager
    dirtyMonitor = monitor
    assert(fileManager != null && dirtyMonitor != null)

    saveModelActions foreach menu.offerAction
    println("   <Tabs.init")

  }


  println("<Tabs")
}
