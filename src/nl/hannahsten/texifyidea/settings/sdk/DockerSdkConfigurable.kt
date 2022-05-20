package nl.hannahsten.texifyidea.settings.sdk

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.projectRoots.AdditionalDataConfigurable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.components.JBLabel
import nl.hannahsten.texifyidea.util.runCommand
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Customized UI for the SDK settings page of [DockerSdk].
 *
 * @author Thomas
 */
class DockerSdkConfigurable : AdditionalDataConfigurable {

    private var sdk: Sdk? = null
    override fun setSdk(sdk: Sdk?) {
        this.sdk = sdk
    }

    private lateinit var imageName: ComboBox<String>

    override fun createComponent(): JComponent {
        // docker image ls --format 'table {{.Repository}}:{{.Tag}}'
        val availableDockerImages = runCommand("docker", "image", "ls", "--format", "table {{.Repository}}:{{.Tag}}")?.split('\n')
            ?.drop(1) // header
            ?.filter { it.isNotBlank() } ?: emptyList()

        imageName = ComboBox(availableDockerImages.toTypedArray())

        // Set width
        val longestString = availableDockerImages.maxByOrNull { it.length } ?: "miktex:latest"
        val width = imageName.getFontMetrics(imageName.font)
            .stringWidth(longestString)
        imageName.preferredSize = Dimension(width, imageName.preferredSize.height)

        return JPanel(FlowLayout(FlowLayout.LEFT)).apply {
            add(JBLabel("Docker image name: "))
            add(imageName)
        }
    }

    override fun isModified(): Boolean {
        val data = sdk?.sdkAdditionalData as? DockerSdkAdditionalData
        return data?.imageName != this.imageName.selectedItem as? String
    }

    override fun apply() {
        if (sdk == null) return
        val newData = DockerSdkAdditionalData(this.imageName.selectedItem as String)
        val modificator = sdk!!.sdkModificator
        modificator.versionString = newData.imageName
        modificator.sdkAdditionalData = newData
        ApplicationManager.getApplication().runWriteAction { modificator.commitChanges() }
    }

    override fun reset() {
        if (sdk == null) return
        val data = sdk!!.sdkAdditionalData as? DockerSdkAdditionalData ?: return
        imageName.selectedItem = data.imageName ?: return
    }
}