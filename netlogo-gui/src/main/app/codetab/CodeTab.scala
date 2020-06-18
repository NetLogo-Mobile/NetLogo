// (C) Uri Wilensky. https://github.com/NetLogo/NetLogo

package org.nlogo.app.codetab

import java.awt.event.{ActionEvent, TextEvent, TextListener}
import java.awt.print.PageFormat
import java.awt.{BorderLayout, Component, Dimension, Graphics, Insets}
import java.io.IOException
import java.net.MalformedURLException
import javax.swing.{AbstractAction, Action, JComponent, JPanel}

import org.nlogo.agent.Observer
import org.nlogo.app.common.{CodeToHtml, EditorFactory, FindDialog, MenuTab, TabsInterface, Events => AppEvents}
import org.nlogo.core.{AgentKind, I18N}
import org.nlogo.editor.DumbIndenter
import org.nlogo.ide.FocusedOnlyAction
import org.nlogo.swing.Utils.icon
import org.nlogo.swing.{PrinterManager, ToolBar, ToolBarActionButton, UserAction, WrappedAction, Printable => NlogoPrintable}
import org.nlogo.window.{CommentableError, ProceduresInterface, Zoomable, Events => WindowEvents}
import org.nlogo.workspace.AbstractWorkspace

abstract class CodeTab(val workspace: AbstractWorkspace, tabs: TabsInterface) extends JPanel
with ProceduresInterface
with ProceduresMenuTarget
with AppEvents.SwitchedTabsEvent.Handler
with WindowEvents.CompiledEvent.Handler
with Zoomable
with NlogoPrintable
with MenuTab {

  println("CodeTab create ")
  private var _dirty = false
  def dirty = _dirty

//  def smartTabbingEnabled = false  // aab check this

  protected def dirty_=(b: Boolean) = {
    CompileAction.setDirty(b)
    _dirty = b
  }

  private lazy val listener = new TextListener {
    override def textValueChanged(e: TextEvent) = dirty = true
  }

  lazy val editorFactory = new EditorFactory(workspace, workspace.getExtensionManager)

  def editorConfiguration =
    editorFactory.defaultConfiguration(100, 80)
      .withCurrentLineHighlighted(true)
      .withListener(listener)

  val text = {
    println("   creating an editor in CodeTab")
    val editor = editorFactory.newEditor(editorConfiguration, true)
    editor.setMargin(new Insets(4, 7, 4, 7))
    editor
  }

  lazy val undoAction: Action = {
    new WrappedAction(text.undoAction,
      UserAction.EditCategory,
      UserAction.EditUndoGroup,
      UserAction.KeyBindings.keystroke('Z', withMenu = true))
  }

  lazy val redoAction: Action = {
    new WrappedAction(text.redoAction,
      UserAction.EditCategory,
      UserAction.EditUndoGroup,
      UserAction.KeyBindings.keystroke('Y', withMenu = true))
  }

  override def zoomTarget = text

  val errorLabel = new CommentableError(text)
  println("   >CodeTab, about to getToolBar")
  val toolBar = getToolBar
  println("   <CodeTab, getToolBar done")
  val scrollableEditor = editorFactory.scrollPane(text)
  def compiler = workspace
  def program = workspace.world.program

  locally {
    setIndenter(false)
    setLayout(new BorderLayout)
    println("   CodeTab, add toolBar to layout")
    add(toolBar, BorderLayout.NORTH)
    val codePanel = new JPanel(new BorderLayout) {
      println("   codetab, create codepanel")
      add(scrollableEditor, BorderLayout.CENTER)
      add(errorLabel.component, BorderLayout.NORTH)
    }
    add(codePanel, BorderLayout.CENTER)
  }

// getToolBar is a method that creates an instantiation of the
// abstract class ToolBar, by providing an implemention of addControls
  def getToolBar = new ToolBar {
    println("     >codetab, gettoolbar new ToolBar")
    override def addControls() {
      println("     >codetab, gettoolbar addControls")
      val proceduresMenu = new ProceduresMenu(CodeTab.this)
      this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        .put(UserAction.KeyBindings.keystroke('G', withMenu = true), "procmenu")
      this.getActionMap.put("procmenu", proceduresMenu.getAction)

      add(new ToolBarActionButton(FindDialog.FIND_ACTION))
      println("       =codetab, add ToolBarActionButton(CompileAction)")
      add(new ToolBarActionButton(CompileAction))
      add(new ToolBar.Separator)
      println("       =codetab, add proceduresMenu addControls")
      add(proceduresMenu)
      println("       =codetab, add IncludedFilesMenu addControls")
      add(new IncludedFilesMenu(getIncludesTable, tabs))
      println("       =codetab,before additionalComps")
      val additionalComps = getAdditionalToolBarComponents
      println("       =codetab, after additionalComps")
      if (additionalComps.nonEmpty) {
        add(new ToolBar.Separator)
        additionalComps foreach add
      }
      println("     <codetab, gettoolbar addControls done")
    }
    println("     <codetab, getToolbar done")
  }

  protected def getAdditionalToolBarComponents: Seq[Component] = {
      println("   codetab, getAdditionalToolBarComponents")
       Seq.empty[Component]
  }

  override val permanentMenuActions = {
    println("   codetab, permanentMenuActions")
    Seq(new CodeToHtml.Action(workspace, this, () => getText)) ++ editorConfiguration.permanentActions
  }
  override val activeMenuActions = {
    println("   codetab, activeMenuActions")
    editorConfiguration.contextActions.filter(_.isInstanceOf[FocusedOnlyAction]) ++ Seq(undoAction, redoAction)
  }
  // don't let the editor influence the preferred size,
  // since the editor tends to want to be huge - ST
  override def getPreferredSize: Dimension = toolBar.getPreferredSize

  def getIncludesTable: Option[Map[String, String]] = {

    println("          >codetab getIncludesTable")
    try {
      //throw new Exception("my exception")
    }
    catch {
      case e: Exception =>
      e.printStackTrace()
    }
    val path = Option(workspace.getModelPath).getOrElse{
      // we create an arbitrary model name for checking include paths when we don't have an actual
      // modelPath or directory
      try workspace.attachModelDir("foo.nlogo")
      catch {
        case ex: MalformedURLException =>
          // if we can't even figure out where we are, we certainly can't have includes
          return None
      }
    }
    println("            =getIncludesTable, path: " + path)

    val result = workspace.compiler.findIncludes(path, getText, workspace.getCompilationEnvironment)
    println("          <codetab getIncludesTable")
    result
  }

  def agentClass = classOf[Observer]

  def kind = AgentKind.Observer

  def printComponent(cmp: Component, description: String): Unit = {
    val pattern = """(^.*)\[(.*$)""".r
    val pattern(name, _) = cmp.toString
    val shortName = name.split("\\.").last
    println(description + System.identityHashCode(cmp) +
     ", " + shortName)
  }

  def handle(e: AppEvents.SwitchedTabsEvent) = {
    println("   CodeTab handle SwitchedTabsEvent")
    printComponent(e.oldTab, "      old tab: ")
    printComponent(e.newTab, "      new tab: ")
    println("      dirty: " + dirty)
    if (dirty && e.oldTab == this) compile()
  }

  private var originalFontSize = -1
  override def handle(e: WindowEvents.ZoomedEvent) {
    super.handle(e)
    if (originalFontSize == -1)
      originalFontSize = text.getFont.getSize
    text.setFont(text.getFont.deriveFont(StrictMath.ceil(originalFontSize * zoomFactor).toFloat))
    scrollableEditor.setFont(text.getFont)
    errorLabel.zoom(zoomFactor)
  }

  def printHandleCompiledEvent(e: org.nlogo.window.Events.CompiledEvent, inClass: String): Unit = {
    println("   >" + inClass + " handle CompiledEvent")
    println("     error: " + java.util.Objects.toString(e.error, "<null>"))
    // println("   program: " + e.program) //seems to always be the same
    println("     procedure: " + e.procedure)
  }

  def handle(e: WindowEvents.CompiledEvent) = {
    printHandleCompiledEvent(e, "CodeTab")
    dirty = false
    if (e.sourceOwner == this) errorLabel.setError(e.error, headerSource.length)
    // this was needed to get extension colorization showing up reliably in the editor area - RG 23/3/16
    text.revalidate()
    println("   <CodeTab handle CompiledEvent")
  }

  protected def compile(): Unit = new WindowEvents.CompileAllEvent().raise(this)

  override def requestFocus(): Unit = text.requestFocus()

  def innerSource = text.getText
  def getText = text.getText  // for ProceduresMenuTarget
  def headerSource = ""
  def source = headerSource + innerSource

  override def innerSource_=(s: String) = {
    text.setText(s)
    text.setCaretPosition(0)
    text.resetUndoHistory()
  }

  def select(start: Int, end: Int) = text.select(start, end)

  def classDisplayName = "Code"

  @throws(classOf[IOException])
  def print(g: Graphics, pageFormat: PageFormat,pageIndex: Int, printer: PrinterManager) =
    printer.printText(g, pageFormat, pageIndex, text.getText)

  def setIndenter(isSmart: Boolean): Unit = {
    if(isSmart) text.setIndenter(new SmartIndenter(new EditorAreaWrapper(text), workspace))
    else text.setIndenter(new DumbIndenter(text))
  }

  def lineNumbersVisible = scrollableEditor.lineNumbersEnabled
  def lineNumbersVisible_=(visible: Boolean) = scrollableEditor.setLineNumbersEnabled(visible)

  def isTextSelected: Boolean = text.getSelectedText != null && !text.getSelectedText.isEmpty

  private object CompileAction extends AbstractAction(I18N.gui.get("tabs.code.checkButton")) {
    println("         >Codetab, object CompileAction")
    putValue(Action.SMALL_ICON, icon("/images/check-gray.gif"))
    def actionPerformed(e: ActionEvent) = compile()
    def setDirty(isDirty: Boolean) = {
      val iconPath =
        if (isDirty) "/images/check.gif"
        else         "/images/check-gray.gif"
      putValue(Action.SMALL_ICON, icon(iconPath))
    }
    println("         <Codetab, object CompileAction")
  }
  println("Done code tab")
}
