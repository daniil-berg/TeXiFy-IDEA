package nl.hannahsten.texifyidea.run.latex.ui

import com.intellij.execution.ui.FragmentedSettingsBuilder
import com.intellij.execution.ui.TagButton
import com.intellij.icons.AllIcons
import com.intellij.ide.DataManager
import com.intellij.ide.dnd.*
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.Conditions
import com.intellij.openapi.util.Disposer
import com.intellij.ui.InplaceButton
import com.intellij.ui.components.labels.LinkLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.WrapLayout
import nl.hannahsten.texifyidea.run.latex.LatexRunConfiguration
import nl.hannahsten.texifyidea.run.latex.step.LatexCompileStep
import nl.hannahsten.texifyidea.run.latex.step.LatexCompileStepProvider
import nl.hannahsten.texifyidea.util.magic.CompilerMagic
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Point
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Box
import javax.swing.JLabel
import javax.swing.JLayeredPane
import javax.swing.JPanel

/**
 * Tag editor for defining a LaTeX compile sequence.
 *
 * Heavily inspired by [com.intellij.execution.ui.BeforeRunComponent].
 *
 * @author Sten Wessel
 */
class LatexCompileSequenceComponent(parentDisposable: Disposable)
    : JPanel(WrapLayout(FlowLayout.LEADING, 0, FragmentedSettingsBuilder.TAG_VGAP)),
      DnDTarget, Disposable {

    private val dropFirst = JLabel(AllIcons.General.DropPlace).apply {
        border = JBUI.Borders.empty()
    }

    private val addButton = InplaceButton("Add step", AllIcons.General.Add) { showPopup() }

    private val addPanel = JPanel().apply {
        border = JBUI.Borders.emptyRight(5)
        add(addButton)
    }

    private val addLabel = LinkLabel<Any>("Add step", null) { _, _ -> showPopup() }.apply {
        border = JBUI.Borders.emptyRight(5)
    }

    private val steps = mutableListOf<StepButton>()

    var changeListener: () -> Unit = {  }

    private lateinit var configuration: LatexRunConfiguration

    init {
        Disposer.register(parentDisposable, this)
        add(Box.createVerticalStrut(30))

        initDropPanel()

        DnDManager.getInstance().registerTarget(this, this, this)
    }

    private fun initDropPanel() {
        val panel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0)).apply {
            add(dropFirst)
            preferredSize = dropFirst.preferredSize
        }
        add(panel)
        dropFirst.isVisible = false
    }

    private fun buildPanel() {
        remove(addPanel)
        remove(addLabel)
        steps.forEach { add(it) }
        add(addPanel)
        add(addLabel)
        addLabel.isVisible = steps.isEmpty()
    }

    private fun showPopup() {
        val group = DefaultActionGroup()

        for (provider in CompilerMagic.compileStepProviders) {
            group.add(TagAction(provider))
        }

        val popup = JBPopupFactory.getInstance().createActionGroupPopup(
            "Add New Step",
            group,
            DataManager.getInstance().getDataContext(addButton),
            false, false, false, null, -1, Conditions.alwaysTrue()
        )

        popup.showUnderneathOf(addButton)
    }

    private fun createStep(provider: LatexCompileStepProvider) {
        val step = provider.createStep(configuration)
        val tag = StepButton(step)
        steps.add(tag)
        buildPanel()
        changeListener()
    }

    fun reset(c: LatexRunConfiguration) {
        configuration = c
        steps.forEach { remove(it) }
        steps.clear()

        configuration.compileSteps.forEach {
            val tag = StepButton(it)
            steps.add(tag)
        }

        buildPanel()
    }

    fun apply(c: LatexRunConfiguration) {
        c.compileSteps.apply {
            clear()
            addAll(steps.map { it.step })
        }
    }

    override fun update(event: DnDEvent?): Boolean {
        return true
    }

    override fun drop(event: DnDEvent) {
        buildPanel()
    }

    override fun dispose() {

    }

    private inner class StepButton(val step: LatexCompileStep) : TagButton(step.provider.name, { changeListener() }), DnDSource {

        private val dropPlace = JLabel(AllIcons.General.DropPlace)

        init {
            Disposer.register(this@LatexCompileSequenceComponent, this)
            add(dropPlace, JLayeredPane.DRAG_LAYER)
            dropPlace.isVisible = false

            myButton.icon = step.provider.icon

            myButton.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        step.configure()
                    }
                }
            })

            DnDManager.getInstance().registerSource(this, myButton, this)
            myButton.toolTipText = "Double click to edit settings.<br>Drag'n'drop to reorder."
            layoutButtons()
        }

        override fun layoutButtons() {
            super.layoutButtons()
            @Suppress("SENSELESS_COMPARISON")
            if (dropPlace == null) return

            val bounds = myButton.bounds
            val size = dropPlace.preferredSize
            val gap = JBUI.scale(2)
            preferredSize = Dimension(bounds.width + size.width + 2*gap, bounds.height)
            dropPlace.setBounds((bounds.maxX + gap).toInt(), bounds.y + (bounds.height - size.height) / 2, size.width, size.height)
        }

        override fun canStartDragging(action: DnDAction?, dragOrigin: Point?) = true

        override fun startDragging(action: DnDAction?, dragOrigin: Point?) = DnDDragStartBean(this)
    }

    private inner class TagAction(private val provider: LatexCompileStepProvider) : AnAction(provider.name, null, provider.icon) {

        override fun actionPerformed(e: AnActionEvent) {
            createStep(provider)
        }
    }
}
