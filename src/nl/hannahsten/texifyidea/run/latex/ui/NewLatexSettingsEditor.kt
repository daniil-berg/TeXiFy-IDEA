package nl.hannahsten.texifyidea.run.latex.ui

import com.intellij.execution.ui.*
import nl.hannahsten.texifyidea.run.latex.LatexRunConfiguration
import nl.hannahsten.texifyidea.run.latex.LatexRunConfigurationExtensionsManager

class NewLatexSettingsEditor(settings: LatexRunConfiguration) : RunConfigurationFragmentedEditor<LatexRunConfiguration>(settings, LatexRunConfigurationExtensionsManager.instance) {


    override fun createRunFragments(): MutableList<SettingsEditorFragment<LatexRunConfiguration, *>> {
        val fragments = mutableListOf<SettingsEditorFragment<LatexRunConfiguration, *>>()

        val beforeRunComponent = BeforeRunComponent(this)
        fragments.add(BeforeRunFragment.createBeforeRun(beforeRunComponent, null))
        fragments.addAll(BeforeRunFragment.createGroup())

        fragments.add(CommonTags.parallelRun())

        // Working directory and environment variables
        val commonParameterFragments = CommonParameterFragments<LatexRunConfiguration>(mySettings.project) { false }
        fragments.addAll(commonParameterFragments.fragments)

        // LaTeX compiler arguments
        val compilerArguments = CommonLatexFragments.programArguments<LatexRunConfiguration>(
            "compilerArguments", "Compiler arguments", 200, { s -> s::compilerArguments },
            name = "Compiler a&rguments"
        )
        compilerArguments.setHint("CLI arguments for the LaTeX compiler")

        fragments.add(compilerArguments)

        fragments.add(CommonLatexFragments.latexCompiler(100))

        return fragments
    }
}