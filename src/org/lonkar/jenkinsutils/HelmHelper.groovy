
package org.lonkar.jenkinsutils

import java.io.Serializable
import hudson.*
import hudson.model.*
import hudson.slaves.*
import hudson.tools.*
import groovy.json.*
import org.jenkinsci.plugins.structs.*
import com.cloudbees.jenkins.plugins.customtools.*
import com.synopsys.arc.jenkinsci.plugins.customtools.versions.*


/**
 * Helm Helper to install and setup helm in PATH variable using targeted binaries
 * from HELM release repository
 *
 */
class HelmHelper implements Serializable {

    private def pipeline
    private def utils

    /**
     * Instantiate HelmHelper using WorkflowScript object
     * @see <a href="https://github.com/jenkinsci/workflow-cps-plugin/blob/0e4c25f8d7b84470aa523491e29933db3b3df588/src/main/java/org/jenkinsci/plugins/workflow/cps/CpsScript.java">CpsScript.java</a>
     *
     * @param pipeline - WorkflowScript
     */
    HelmHelper(pipeline) {
        this.pipeline = pipeline
        this.utils = new PipelineUtils(pipeline)
    }

    /**
     * Installs terraform if not exists already using CustomTool and adds it to PATH
     *
     * @param version of terraform to use
     */
    def use(version = '2.12.3') {
        if (!(version ==~ /^v.*$/)) {
            version = "v${version}" 
        }
        def os = utils.currentOS()
        def arch = utils.currentArchitecture().replace('i','')
        def extension = pipeline.isUnix() ? 'tar.gz' : 'zip'
        def helmVersionPath = "${pipeline.env.JENKINS_HOME}/tools/helm/${version}/${os}-${arch}"
        List<InstallSourceProperty> properties = [
            new InstallSourceProperty([
                new ZipExtractionInstaller(null, "https://storage.googleapis.com/kubernetes-helm/helm-${version}-${os}-${arch}.${extension}", "${helmVersionPath}")
            ].toList())
        ].toList()
        def tool = new CustomTool("helm.${version}", helmVersionPath, properties, helmVersionPath, null, ToolVersionConfig.DEFAULT, null)
        def currNode = pipeline.getContext Node.class
        def currListener = pipeline.getContext TaskListener.class
        def helmPath = tool.forNode(currNode, currListener).getHome()
        pipeline.env.PATH = "${helmPath}:${pipeline.env.PATH}"
        pipeline.echo "using helm ${version}"
    }
}