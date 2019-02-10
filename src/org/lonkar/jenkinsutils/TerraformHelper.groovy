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

class TerraformHelper implements Serializable {

    def pipeline
    def transient varMapping
    def transient jsonVarMapping

    TerraformHelper(pipeline) {
        this.pipeline = pipeline
        this.varMapping = [:]
        this.jsonVarMapping = [:]
    }

    /**
     * Installs terraform if not exists already from CustomTool and add it to path
     */
    def use(version = '0.11.11') {
        def utils = new PipelineUtils(pipeline)
        def os = utils.currentOS()
        def arch = utils.currentArchitecure().replace('i','')
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

    def map(buildParamName, tfVar, jsonType = false) {
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
     * tfvars - array of string containing values that only to be parsed/mapped.
     */
    @NonCPS
    def buildParamsToTFVars(tfvars = []) {
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

    @NonCPS
    def overrideTFJsonString(terraformInput) {
        return new JsonBuilder([ variable: terraformInput ]).toPrettyString()
    }
}