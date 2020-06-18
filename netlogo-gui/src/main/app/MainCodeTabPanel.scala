// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app

import java.awt.{ BorderLayout, Component, Dimension, Window }
import javax.swing.{ JDialog, JFrame, JTabbedPane, WindowConstants }
//CodeTab,
import org.nlogo.app.codetab.{ ExternalFileManager, MainCodeTab }
import org.nlogo.core.I18N
import org.nlogo.swing.{ TabsMenu }
import org.nlogo.window.{ GUIWorkspace }
import org.nlogo.app.interfacetab.InterfaceTab
class MainCodeTabPanel(workspace:       GUIWorkspace,
           interfaceTab:    InterfaceTab,
           menu:    MenuBar,
           externalFileManager: ExternalFileManager)
  extends AbstractTabs(workspace,
      interfaceTab, menu, externalFileManager)
 {
  println("MainCodeTabPanel create class")

  // Frame is the main app frame, which is the container for the
  // JDialog that contains the code tab and its JTabbedPane
  // val frame = (javax.swing.JFrame) workspace.getFrame
  val frame = workspace.getFrame.asInstanceOf[JFrame]
  val codeTabContainer = initCodeContainer(frame)
  val codeTabbedPane = new (JTabbedPane)
  codeTabContainer.add(codeTabbedPane, BorderLayout.CENTER)

  // println("  =MainCodeTabPanel about to codeTabbedPane.add" )
  println("   MainCodeTabPanel create MainCodeTab")
  override val codeTab = new MainCodeTab(workspace, this, menu)
  println("   MainCodeTabPanel done MainCodeTab")
  this.add(codeTab)
  currentTab = codeTab

  def initCodeContainer(frame: JFrame): Window = {
    println("initCodeContainer")
    val codeTabContainer = new JDialog(frame, I18N.gui.get("tabs.code"))
    //codeTabContainer.setModalityType(Dialog.ModalityType.MODELESS)
    codeTabContainer.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    codeTabContainer.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    //codeTabContainer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    codeTabContainer.setSize(new Dimension(600, 400))
    codeTabContainer.setLocationRelativeTo(null)
    codeTabContainer.setVisible(true)
    codeTabContainer
  }

  override def init(manager: FileManager, monitor: DirtyMonitor, moreTabs: (String, Component) *) {
    println("   MainCodeTabPanel init begins")
    println("      MainCodeTabPanel code")
    addTab(I18N.gui.get("tabs.code"), codeTab)
    println("      MainCodeTabPanel add more")
    // for((name, tab) <- moreTabs)
    //   addTab(name, tab)
    //   println("      MainCodeTabPanel have been added")
    tabActions = TabsMenu.tabActions(this)
    fileManager = manager
    dirtyMonitor = monitor
    assert(fileManager != null && dirtyMonitor != null)

    saveModelActions foreach menu.offerAction
    println("   MainCodeTabPanel init end")

  }

  println("MainCodeTabPanel end")
}
