// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app
// import java.awt.{ Color } // aab
// import org.nlogo.window.{ JobWidget } // aab
import java.awt.{ Component }
import java.awt.event.{ ActionEvent, MouseEvent }
import java.awt.print.PrinterAbortException
import javax.swing.{ AbstractAction, Action, JTabbedPane, SwingConstants }
import javax.swing.event.{ ChangeEvent, ChangeListener }
import javax.swing.plaf.ComponentUI
import javax.swing.{ AbstractAction, Action, JTabbedPane, SwingConstants }
import org.nlogo.api.Exceptions
import org.nlogo.app.codetab.{ CodeTab, ExternalFileManager, TemporaryCodeTab }
import org.nlogo.app.common.{ ExceptionCatchingAction, MenuTab, TabsInterface, Events => AppEvents },
  TabsInterface.Filename
import org.nlogo.app.infotab.InfoTab
import org.nlogo.app.interfacetab.InterfaceTab
import org.nlogo.awt.{ EventQueue, UserCancelException }
import org.nlogo.core.I18N
import org.nlogo.swing.{ Printable, PrinterManager, TabsMenu, UserAction }, UserAction.MenuAction
import org.nlogo.window.Event.LinkParent
import org.nlogo.window.Events._
import org.nlogo.window.{ Event, ExternalFileInterface, GUIWorkspace, MonitorWidget }
import org.nlogo.app.interfacetab.InterfaceTab
abstract class AbstractTabs(val workspace:       GUIWorkspace,
           val interfaceTab:    InterfaceTab,
           private var menu:    MenuBar,
           externalFileManager: ExternalFileManager)
  extends JTabbedPane(SwingConstants.TOP)
  with TabsInterface with ChangeListener with LinkParent
  with org.nlogo.window.LinkRoot
  with AboutToCloseFilesEvent.Handler
  with LoadBeginEvent.Handler
  with RuntimeErrorEvent.Handler
  with CompiledEvent.Handler
  with AfterLoadEvent.Handler
  with ExternalFileSavedEvent.Handler {
  // println("  <AbstractTabs")
  locally {
    setOpaque(false)
    setFocusable(false)
    addChangeListener(this)
    if (System.getProperty("os.name").startsWith("Mac")) {
      try {
        val ui = Class.forName("org.nlogo.app.MacTabbedPaneUI").newInstance.asInstanceOf[ComponentUI]
        setUI(ui)
      } catch {
        case e: ClassNotFoundException =>
      }
    }
  }

  var tabManager: AppTabManager = null

  def setTabManager( myTabManager: AppTabManager ) {
    tabManager = myTabManager
  }

  def getTabManager() = tabManager

  def setMenu(newMenu: MenuBar): Unit = {
    //println("    >setMenu")
    val menuItems = permanentMenuActions ++ (currentTab match {
      case mt: MenuTab => mt.activeMenuActions
      case _ => Seq()
    })

    //println("menuItems: " +  menuItems.map(_.getValue(javax.swing.Action.NAME)).mkString(" "))
    menuItems.foreach(action => menu.revokeAction(action))
    //println("menu: " + menu.getSubElements.map(_.getClass.getSimpleName).mkString(" "))
    menuItems.foreach(newMenu.offerAction)
    //println("newMenu: " + newMenu.getSubElements.map(_.getClass.getSimpleName).mkString(" "))
    menu = newMenu
    //println("    >setMenu")
  }

  def permanentMenuActions =
    tabActions ++ interfaceTab.permanentMenuActions :+ PrintAction
// aab check
    //tabActions ++ codeTab.permanentMenuActions ++ interfaceTab.permanentMenuActions :+ PrintAction


  var tabActions: Seq[Action] = TabsMenu.tabActions(this)
  lazy val saveModelActions = fileManager.saveModelActions(this)

  var fileManager: FileManager = null
  var dirtyMonitor: DirtyMonitor = null

  val infoTab = new InfoTab(workspace.attachModelDir(_))
  var externalFileTabs = Set.empty[TemporaryCodeTab]
  var currentTab: Component = interfaceTab

  val codeTab = new CodeTab(workspace, this) {}

  def initManagerMonitor(manager: FileManager, monitor: DirtyMonitor) {
    fileManager = manager
    dirtyMonitor = monitor
    assert(fileManager != null && dirtyMonitor != null)
  }

  def addAdditionalTabs(moreTabs: (String, Component) *) {
    for((name, tab) <- moreTabs)
      addTab(name, tab)
  }

  def getComponentString(cmp: Component): String = {
    if (cmp == null) {
        "<null>"
    } else {
      val pattern = """(^.*)\[(.*$)""".r
      val pattern(name, _) = cmp.toString
      val shortName = name.split("\\.").last
      shortName + " " + System.identityHashCode(cmp)
    }
  }

  def stateChanged(e: ChangeEvent) = {
    //println("   the tab: " + this)
    println("   AbstractTabs proc stateChanged: " )
    val previousTab = currentTab
    currentTab = getSelectedComponent // aab fix
    println("      old tab: " + getComponentString(previousTab))
    println("      new tab: " + getComponentString(currentTab))
    previousTab match {
      case mt: MenuTab => mt.activeMenuActions foreach menu.revokeAction
      case _ =>
    }
    currentTab match {
      case mt: MenuTab => mt.activeMenuActions foreach menu.offerAction
      case _ =>
    }
    (previousTab.isInstanceOf[TemporaryCodeTab], currentTab.isInstanceOf[TemporaryCodeTab]) match {
      case (true, false) => saveModelActions foreach menu.offerAction
      //case (true, true) => saveModelActions foreach menu.offerAction
      case (false, true) => saveModelActions foreach menu.revokeAction
      case _             =>
    }
    // println("       current tab request focus ")
    currentTab.requestFocus()
    // println("       SwitchedTabs event")
    new AppEvents.SwitchedTabsEvent(previousTab, currentTab).raise(this)
  }

  override def requestFocus() = currentTab.requestFocus()

  def handle(e: AboutToCloseFilesEvent) =
    println("   AbstractTabs handle AboutToCloseFilesEvent")
    OfferSaveExternalsDialog.offer(externalFileTabs filter (_.saveNeeded), this)

  def handle(e: LoadBeginEvent) = {
    //println("   AbstractTabs handle LoadBeginEvent")
    setSelectedComponent(interfaceTab)
    externalFileTabs foreach { tab =>
      externalFileManager.remove(tab)
      closeExternalFile(tab.filename)
    }
  }

  def handle(e: RuntimeErrorEvent) {
    println("   AbstractTabs handle RuntimeErrorEvent")
     if(!e.jobOwner.isInstanceOf[MonitorWidget])
        e.sourceOwner match {
          case `codeTab` =>
            highlightRuntimeError(codeTab, e)
          case file: ExternalFileInterface =>
            val filename = file.getFileName
            val tab = getTabWithFilename(Right(filename)).getOrElse {
              openExternalFile(filename)
              getTabWithFilename(Right(filename)).get
            }
            highlightRuntimeError(tab, e)
          case _ =>
        }
      }

  def highlightRuntimeError(tab: CodeTab, e: RuntimeErrorEvent) {
    println("   highlightRuntimeError tab: " + tab)
    println("   RTE Jobowner: " + e.jobOwner + " sourceOwner:" + e.sourceOwner)
    println("   pos: " + e.pos + " length: " + e.length)
    tabManager.setSelectedCodeTab(tab)     // aab replacement
    //setSelectedComponent(tab)  // aab orig
    // the use of invokeLater here is a desperate attempt to work around the Mac bug where sometimes
    // the selection happens and sometime it doesn't - ST 8/28/04
    EventQueue.invokeLater(() => tab.select(e.pos, e.pos + e.length) )
  }
// aab removed handle compiledEvent

  def handle(e: ExternalFileSavedEvent) {
    // println("   AbstractTabs handle ExternalFileSavedEvent")
    getTabWithFilename(Right(e.path)) foreach { tab =>
      val index = indexOfComponent(tab)
      setTitleAt(index, tab.filenameForDisplay)
      tabActions(index).putValue(Action.NAME, e.path)
    }
  }

  def getSource(filename: String): String = getTabWithFilename(Right(filename)).map(_.innerSource).orNull

  def getTabWithFilename(filename: Filename): Option[TemporaryCodeTab] =
    externalFileTabs find (_.filename == filename)

  private var _externalFileNum = 1
  private def externalFileNum() = {
    _externalFileNum += 1
    _externalFileNum - 1
  }
  def newExternalFile() = addNewTab(Left(I18N.gui.getN("tabs.external.new", externalFileNum(): Integer)))

  def openExternalFile(filename: String) =
    getTabWithFilename(Right(filename)) match {
      case Some(tab) =>tabManager.setSelectedCodeTab(tab)   // aab replacement
      //case Some(tab) => setSelectedComponent(tab) // aab orig
      case _ => addNewTab(Right(filename))
    }

  def addNewTab(name: Filename) = {
    val tab = new TemporaryCodeTab(workspace,
      this,
      name,
      externalFileManager,
      fileManager.convertTabAction _,
      false)
  //aab    codeTab.smartTabbingEnabled)
    if (externalFileTabs.isEmpty) menu.offerAction(SaveAllAction)
    externalFileTabs += tab
    addTab(tab.filenameForDisplay, tab)
    addMenuItem(getTabCount - 1, tab.filenameForDisplay)
    Event.rehash()
  tabManager.setSelectedCodeTab(tab)     // aab replacement
    //setSelectedComponent(tab)  // aab orig
    // if I just call requestFocus the tab never gets the focus request because it's not yet
    // visible.  There might be a more swing appropriate way to do this but I can't figure it out
    // (if you know it feel free to fix) ev 7/24/07
    EventQueue.invokeLater( () => requestFocus() )
  }

  def getIndexOfComponent(tab: CodeTab): Int =
    (0 until getTabCount).find(n => getComponentAt(n) == tab).get

  def closeExternalFile(filename: Filename): Unit =
    getTabWithFilename(filename) foreach { tab =>
      val index = getIndexOfComponent(tab)  // aab
      remove(tab)
      removeMenuItem(index)
      externalFileTabs -= tab
      if (externalFileTabs.isEmpty) menu.revokeAction(SaveAllAction)
    }

  def forAllCodeTabs(fn: CodeTab => Unit) =
    (externalFileTabs.asInstanceOf[Set[CodeTab]] + codeTab) foreach fn

  def lineNumbersVisible = codeTab.lineNumbersVisible
  def lineNumbersVisible_=(visible: Boolean) = forAllCodeTabs(_.lineNumbersVisible = visible)

  def removeMenuItem(index: Int) {
    tabActions.foreach(action => menu.revokeAction(action))
    tabActions = TabsMenu.tabActions(this)
    tabActions.foreach(action => menu.offerAction(action))
  }

  def addMenuItem(i: Int, name: String) {
    val newAction = TabsMenu.tabAction(this, i)
    tabActions = tabActions :+ newAction
    menu.offerAction(newAction)
  }

  override def processMouseMotionEvent(e: MouseEvent) {
    // do nothing.  mouse moves are for some reason causing doLayout to be called in the tabbed
    // components on windows and linux (but not Mac) in java 6 it never did this before and I don't
    // see any reason why it needs to. It's causing flickering in the info tabs on the affected
    // platforms ev 2/2/09
  }

  def handle(e: AfterLoadEvent) {
    // println("   AbstractTabs handle AfterLoadEvent")
    requestFocus()
  }

  object SaveAllAction extends ExceptionCatchingAction(I18N.gui.get("menu.file.saveAll"), this)
  with MenuAction {
    category    = UserAction.FileCategory
    group       = UserAction.FileSaveGroup
    rank        = 1
    accelerator = UserAction.KeyBindings.keystroke('S', withMenu = true, withAlt = true)

    @throws(classOf[UserCancelException])
    override def action(): Unit = {
      fileManager.saveModel(false)
      externalFileTabs foreach (_.save(false))
    }
  }

  object PrintAction extends AbstractAction(I18N.gui.get("menu.file.print")) with UserAction.MenuAction {
    category = UserAction.FileCategory
    group = "org.nlogo.app.Tabs.Print"
    accelerator = UserAction.KeyBindings.keystroke('P', withMenu = true)

    def actionPerformed(e: ActionEvent) = currentTab match {
      case printable: Printable =>
        try PrinterManager.print(printable, workspace.modelNameForDisplay)
        catch {
          case abortEx: PrinterAbortException => Exceptions.ignore(abortEx)
        }
    }
  }


  def printHandleCompiledEvent(e: CompiledEvent, inClass: String): Unit = {
    println("   >" + inClass + " handle CompiledEvent")
    println("     error: " + java.util.Objects.toString(e.error, "<null>"))
    println("     sourceOwner: " + e.sourceOwner)
    // println("   program: " + e.program) //seems to always be the same
    println("     procedure: " + e.procedure)
  }
  
  // override def handle(e: CompiledEvent) = {
  //   println( "AbstractTabs.handle.CompiledEvent")
  //
  //   // try {
  //   //   throw new Exception("my exception")
  //   // }
  //   // catch {
  //   //   case e: Exception =>
  //   //   e.printStackTrace()
  //   // }
  //   printHandleCompiledEvent(e, "AbstractTabs")
  //   val errorColor = Color.RED
  //   def clearErrors() = forAllCodeTabs(tab =>
  //     tabManager.getTabOwner(tab).setForegroundAt(
  //    tabManager.getTabOwner(tab).indexOfComponent(tab), null))
  //   def recolorTab(component: Component, hasError: Boolean): Unit =
  //     tabManager.getTabOwner(component).setForegroundAt(
  //       tabManager.getTabOwner(component).indexOfComponent(component),
  //       if(hasError) errorColor else null)
  //
  //   def recolorInterfaceTab() = {
  //     if (e.error != null) setSelectedIndex(0)
  //     recolorTab(interfaceTab, e.error != null)
  //   }
  //
  //   // recolor tabs
  //   e.sourceOwner match {
  //     case `codeTab` =>
  //       // on null error, clear all errors, as we only get one event for all the files
  //       println("     AbstractTabs CompiledEvent case code tab")
  //       if (e.error == null) {
  //         println("     AbstractTabs CompiledEvent case null error")
  //         clearErrors()
  //       }
  //       else {
  //         println("     AbstractTabs CompiledEvent case not null error")
  //         tabManager.setSelectedCodeTab(codeTab)     // aab replacement
  //         // setSelectedComponent(codeTab)  // aab orig
  //         recolorTab(codeTab, true)
  //       }
  //       // I don't really know why this is necessary when you delete a slider (by using the menu
  //       // item *not* the button) which causes an error in the Code tab the focus gets lost,
  //       // so request the focus by a known component 7/18/07
  //       requestFocus()
  //     case file: ExternalFileInterface =>
  //       println("     AbstractTabs CompiledEvent case ExternalFileInterface")
  //       val filename = file.getFileName
  //       var tab = getTabWithFilename(Right(filename))
  //       if (!tab.isDefined && e.error != null) {
  //         openExternalFile(filename)
  //         tab = getTabWithFilename(Right(filename))
  //         tab.get.handle(e) // it was late to the party, let it handle the event too
  //       }
  //       // if (e.error != null) tabManager.setSelectedCodeTab(tab.get) // aab replacement
  //       if (e.error != null) setSelectedComponent(tab.get) // aab orig
  //       recolorTab(tab.get, e.error != null)
  //       requestFocus()
  //     case null => // i'm assuming this is only true when we've deleted that last widget. not a great sol'n - AZS 5/16/05
  //       recolorInterfaceTab()
  //     case jobWidget: JobWidget if !jobWidget.isCommandCenter =>
  //       recolorInterfaceTab()
  //     case _ =>
  //   }
  //   println("   <AbstractTabs handle CompiledEvent")
  // }
  //println("  <AbstractTabs")
}
