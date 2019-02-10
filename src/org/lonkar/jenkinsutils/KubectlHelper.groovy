
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
 * Kubectl Helper to install and setup Kubectl in PATH variable using targeted binaries
 *
 */
class KubectlHelper implements Serializable {

    private def pipeline
    private def utils

    /**
     * Instantiate KubectlHelper using WorkflowScript object
     * @see <a href="https://github.com/jenkinsci/workflow-cps-plugin/blob/0e4c25f8d7b84470aa523491e29933db3b3df588/src/main/java/org/jenkinsci/plugins/workflow/cps/CpsScript.java">CpsScript.java</a>
     *
     * @param pipeline - WorkflowScript
     */
    KubectlHelper(pipeline) {
        this.pipeline = pipeline
        this.utils = new PipelineUtils(pipeline)
    }

    /**
     * Installs kubectl if not exists already using bash/bat and add it to path
     *
     * @param version of kubectl to use
     */
    def use(version = false) {
        if (!version && pipeline.isUnix()) {
            version = utils.silentBash(script: 'curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt', returnStdout: true).trim()
        } else if (!version) {
            version = pipeline.bat(script: 'curl -s https://storage.googleapis.com/kubernetes-release/release/stable.txt', returnStdout: true).trim()
        } else if (!(version ==~ /^v.*$/)) {
            version = "v${version}" 
        }
        def kubectlHome = "${pipeline.env.JENKINS_HOME}/tools/kubectl/${version}"
        if (pipeline.isUnix()) {
            def exists = utils.silentBash(script: "#!/bin/bash -e\n[[ -e ${kubectlHome}/kubectl ]]", returnStatus: true)
            if (exists != 0) {
                utils.silentBash script: """
                    mkdir -p ${kubectlHome}
                    cd ${kubectlHome}
                    curl -LO https://storage.googleapis.com/kubernetes-release/release/${version}/bin/linux/amd64/kubectl
                    chmod +x kubectl
                """
            }
        } else {
            def exists = pipeline.bat(script: "if exist ${kubectlHome}/kubectl.exec ( rem true ) else ( rem false )", returnStdout: true).trim()
            if (exists == 'false') {
                pipeline.bat script: """
                    @echo off
                    setlocal EnableExtensions DisableDelayedExpansion
                    set "Directory=${kubectlHome}"
                    if not exist "%Directory%\\*" md "%Directory%" || pause & goto :EOF
                    rem Other commands after successful creation of the directory.
                    endlocal
                    cd ${kubectlHome}
                    curl -LO https://storage.googleapis.com/kubernetes-release/release/${version}/bin/windows/amd64/kubectl.exe
                """
            }
        }
        pipeline.env.PATH = "${kubectlHome}:${pipeline.env.PATH}"
        pipeline.echo "using kubectl ${version}"
    }
}