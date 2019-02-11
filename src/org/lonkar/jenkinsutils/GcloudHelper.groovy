
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
 * Gcloud Helper to install and setup gcloud in PATH variable using targeted binaries
 * from gcloud versioned archives
 *
 */
class GcloudHelper implements Serializable {

    private def pipeline
    private def utils

    /**
     * Instantiate GcloudHelper using WorkflowScript object
     * @see <a href="https://github.com/jenkinsci/workflow-cps-plugin/blob/0e4c25f8d7b84470aa523491e29933db3b3df588/src/main/java/org/jenkinsci/plugins/workflow/cps/CpsScript.java">CpsScript.java</a>
     *
     * @param pipeline - WorkflowScript
     */
    GcloudHelper(pipeline) {
        this.pipeline = pipeline
        this.utils = new PipelineUtils(pipeline)
    }

    /**
     * Installs terraform if not exists already using CustomTool and adds it to PATH
     *
     * @param version of terraform to use
     */
    def use(version = '233.0.0') {
        def os = utils.currentOS()
        def arch = utils.currentArchitecture().replace('i','')
        if (arch == 'amd64') {
            arch = 'x86_64'
        }
        def extension = pipeline.isUnix() ? '.tar.gz' : 'bundled-python.zip'
        def gcloudVersionPath = "${pipeline.env.JENKINS_HOME}/tools/gcloud/${version}"
        List<InstallSourceProperty> properties = [
            new InstallSourceProperty([
                new ZipExtractionInstaller(null, "https://dl.google.com/dl/cloudsdk/channels/rapid/downloads/google-cloud-sdk-${version}-${os}-${arch}${extension}", "${gcloudVersionPath}/google-cloud-sdk/bin/")
            ].toList())
        ].toList()

        def tool = new CustomTool("gcloud.${version}", gcloudVersionPath, properties, '', null, ToolVersionConfig.DEFAULT, null)
        def currNode = pipeline.getContext Node.class
        def currListener = pipeline.getContext TaskListener.class
        def gcloudPath = tool.forNode(currNode, currListener).getHome()
        pipeline.env.PATH = "${gcloudPath}:${pipeline.env.PATH}"
        pipeline.echo "using gcloud ${version}"
    }
}