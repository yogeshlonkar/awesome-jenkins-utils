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
 * Terraform Helper to install and setup terraform in PATH variable using targeted binaries
 * from Terraform release repository. This utility can also help generate override.tf.json file containing
 * terraform input with value of default set using build parameters
 *
 */
class TerraformHelper implements Serializable {

    private def pipeline
    private def transient varMapping
    private def transient jsonVarMapping

    /**
     * Instantiate TerraformHelper using WorkflowScript object
     * @see <a href="https://github.com/jenkinsci/workflow-cps-plugin/blob/0e4c25f8d7b84470aa523491e29933db3b3df588/src/main/java/org/jenkinsci/plugins/workflow/cps/CpsScript.java">CpsScript.java</a>
     *
     * @param pipeline - WorkflowScript
     */
    TerraformHelper(pipeline) {
        this.pipeline = pipeline
        this.varMapping = [:]
        this.jsonVarMapping = [:]
    }

    /**
     * Installs terraform if not exists already using CustomTool and adds it to path
     *
     * @param version of terraform
     */
    def void use(version = '0.11.11') {
        def utils = new PipelineUtils(pipeline)
        def os = utils.currentOS()
        def arch = utils.currentArchitecture().replace('i','')
        def tfVersionPath = "${pipeline.env.JENKINS_HOME}/tools/terraform/${version}"
        List<InstallSourceProperty> properties = [
            new InstallSourceProperty([
                new ZipExtractionInstaller('', "https://releases.hashicorp.com/terraform/${version}/terraform_${version}_${os}_${arch}.zip", '')
            ].toList())
        ].toList()
        def tool = new CustomTool("terraform.${version}", tfVersionPath, properties, tfVersionPath, null, ToolVersionConfig.DEFAULT, null)
        def currNode = pipeline.getContext Node.class
        def currListener = pipeline.getContext TaskListener.class
        def tfPath = tool.forNode(currNode, currListener).getHome()
        pipeline.env.PATH = "${tfPath}:${pipeline.env.PATH}"
        pipeline.echo "using terraform ${version}"
    }

    /**
     * Map build parameter to terraform variable. Currently string, boolean types are only supported.
     * Once terraform version 0.12 is released better additional types such as list and map can be supported
     * @see <a href="https://www.terraform.io/upgrade-guides/0-12.html">Terraform v0.12</a>
     *
     * @param buildParamName string build parameter name
     * @param tfVar terraform variable name
     * @param jsonType not yet supported
     */
    def void map(buildParamName, tfVar, jsonType = false) {
        if (jsonType) {
            jsonVarMapping[buildParamName] = [
                type: jsonType,
                tfVar: tfVar
            ]
        } else {
            varMapping[buildParamName] = tfVar
        }
    }

    /**
     * Generate terraformInput object
     *
     * @param tfvars array of string containing terraform variable names that only need to be parsed/mapped.
     *        if empty all mapped parameters will be return in terraformInput
     * @return object with terraform variable names as keys and object with default with build parameter value as value
     */
    @NonCPS
    def Object buildParamsToTFVars(tfvars = []) {
        def build = pipeline.currentBuild.rawBuild
        def terraformInput = [:]
        def jsonSlurper = new JsonSlurperClassic()
        build.actions.find{ it instanceof ParametersAction }?.parameters.each { buildParam ->
            def tfVar = varMapping[buildParam.name]
            def jsontfVar = jsonVarMapping[buildParam.name]
            if (tfvars.size() > 0 && !tfvars.contains(tfVar) && !tfvars.contains(jsontfVar)) {
                return
            }
            if (tfVar) {
                terraformInput[tfVar] = [
                    'default': buildParam.value
                ]
            } else if (jsonVarMapping[buildParam.name]) {
                tfVar = jsonVarMapping[buildParam.name].tfVar
                terraformInput[tfVar] = [
                    type: jsonVarMapping[buildParam.name].type,
                    'default': jsonSlurper.parseText(buildParam.value)
                ]
            }
        }
        return terraformInput
    }

    /**
     * stringify object to match terraform variable.tf.json format
     * @param terraformInput
     * @return JSON pretty string
     */
    @NonCPS
    def overrideTFJsonString(terraformInput) {
        return new JsonBuilder([ variable: terraformInput ]).toPrettyString()
    }
}