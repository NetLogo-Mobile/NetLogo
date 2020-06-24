// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app

import java.awt.{ BorderLayout, Color, Component, Dimension, Window }
import javax.swing.{ JDialog, JFrame, JTabbedPane, WindowConstants }
import org.nlogo.app.codetab.{ ExternalFileManager, MainCodeTab }
import org.nlogo.core.I18N
import org.nlogo.swing.{ TabsMenu }
import org.nlogo.window.Events._
import org.nlogo.window.{ ExternalFileInterface, GUIWorkspace, JobWidget }
import org.nlogo.app.interfacetab.InterfaceTab
class MainCodeTabPanel(workspace:       GUIWorkspace,
           interfaceTab:    InterfaceTab,
           menu:    MenuBar,
           externalFileManager: ExternalFileManager)
  extends AbstractTabs(workspace,
      interfaceTab, menu, externalFileManager)
 {
  // Frame is the main app frame, which is the container for the
  // JDialog that contains the code tab and its JTabbedPane
  // val frame = (javax.swing.JFrame) workspace.getFrame
  val frame = workspace.getFrame.asInstanceOf[JFrame]
  //val codeTabbedPane = this

  val codeTabContainer = initCodeContainer(frame, this)

  override val codeTab = new MainCodeTab(workspace, this, menu)

  currentTab = codeTab

  def initCodeContainer(frame: JFrame, codeTabbedPane: JTabbedPane): Window = {
    val codeTabContainer = new JDialog(frame,"Code Tab Container")
    // aab2 val codeTabContainer = new JDialog(frame, I18N.gui.get("tabs.code"))
    //codeTabContainer.setModalityType(Dialog.ModalityType.MODELESS)
    codeTabContainer.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    codeTabContainer.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE)
    //codeTabContainer.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE)
    codeTabContainer.add(codeTabbedPane, BorderLayout.CENTER)
    codeTabContainer.setSize(new Dimension(600, 400))
    codeTabContainer.setLocationRelativeTo(null)
    codeTabContainer.setVisible(true)
    codeTabContainer
  }

  override def init(manager: FileManager, monitor: DirtyMonitor, moreTabs: (String, Component) *) {
    println("   >MainCodeTabPanel.init")
    addTab(I18N.gui.get("tabs.code"), codeTab)
    tabActions = TabsMenu.tabActions(this)
    fileManager = manager
    dirtyMonitor = monitor
    assert(fileManager != null && dirtyMonitor != null)
    saveModelActions foreach menu.offerAction
    println("   <MainCodeTabPanel.init")
  }

  def printHandleCompiledEvent(e: CompiledEvent, inClass: String): Unit = {
    println("   >" + inClass + " handle CompiledEvent")
    println("     error: " + java.util.Objects.toString(e.error, "<null>"))
    println("     sourceOwner: " + e.sourceOwner)
    // println("   program: " + e.program) //seems to always be the same
    println("     procedure: " + e.procedure)
  }

  override def handle(e: CompiledEvent) = {
    printHandleCompiledEvent(e, "MainCodeTabPanel")

    val errorColor = Color.RED
    def clearErrors() = forAllCodeTabs(tab =>
      tabManager.getTabOwner(tab).setForegroundAt(
     tabManager.getTabOwner(tab).indexOfComponent(tab), null))
    def recolorTab(component: Component, hasError: Boolean): Unit =
      tabManager.getTabOwner(component).setForegroundAt(
        tabManager.getTabOwner(component).indexOfComponent(component),
        if(hasError) errorColor else null)

    def recolorInterfaceTab() = {
      if (e.error != null) setSelectedIndex(0)
      recolorTab(interfaceTab, e.error != null)
    }

    // recolor tabs
    e.sourceOwner match {
      case `codeTab` =>
        // on null error, clear all errors, as we only get one event for all the files
        println("     MainCodeTabPanel CompiledEvent case code tab")
        if (e.error == null) {
          println("     MainCodeTabPanel CompiledEvent case null error")
          clearErrors()
        }
        else {
          println("     MainCodeTabPanel CompiledEvent case not null error")
          tabManager.setSelectedCodeTab(codeTab)     // aab replacement
          // setSelectedComponent(codeTab)  // aab orig
          recolorTab(codeTab, true)
        }
        // I don't really know why this is necessary when you delete a slider (by using the menu
        // item *not* the button) which causes an error in the Code tab the focus gets lost,
        // so request the focus by a known component 7/18/07
        requestFocus()
      case file: ExternalFileInterface =>
        println("     MainCodeTabPanel CompiledEvent case ExternalFileInterface")
        val filename = file.getFileName
        var tab = getTabWithFilename(Right(filename))
        if (!tab.isDefined && e.error != null) {
          openExternalFile(filename)
          tab = getTabWithFilename(Right(filename))
          tab.get.handle(e) // it was late to the party, let it handle the event too
        }
        // if (e.error != null) tabManager.setSelectedCodeTab(tab.get) // aab replacement
        if (e.error != null) setSelectedComponent(tab.get) // aab orig
        recolorTab(tab.get, e.error != null)
        requestFocus()
      case null => // i'm assuming this is only true when we've deleted that last widget. not a great sol'n - AZS 5/16/05
        recolorInterfaceTab()
      case jobWidget: JobWidget if !jobWidget.isCommandCenter =>
        recolorInterfaceTab()
      case _ =>
    }
    println("   <MainCodeTabPanel handle CompiledEvent")
  }
}
