package org.lonkar.jenkinsutils

import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import groovy.json.*
import hudson.model.*
import java.io.Serializable

/**
 * Common utilities for using in scripted pipeline
 *
 */
class PipelineUtils implements Serializable {

    private static String CommonHeaderStyle = 'font-family: Roboto, sans-serif !important;text-align: center;margin-left: -250px;font-weight: bold;'
    private static String GlobalSeparatorStyle = 'display: none;'
    private static String GlobalHeaderDangerStyle = CommonHeaderStyle + 'background: #f8d7da;color: #721c24;'
    private static String GlobalHeaderSuccessStyle = CommonHeaderStyle + 'background: #d4edda;color: #155724;'
    private static String GlobalHeaderInfoStyle = CommonHeaderStyle + 'background: #d1ecf1;color: #0c5460;'
    private static String GlobalHeaderSecondaryStyle = CommonHeaderStyle + 'background: #ccc;color: #000;'

    private def pipeline
    private def exceptionInBuild
    private boolean hasAnsiSupport
    private boolean disableAnsi;

    /**
     * Instantiate PipelineUtils using WorkflowScript object
     * @see <a href="https://github.com/jenkinsci/workflow-cps-plugin/blob/0e4c25f8d7b84470aa523491e29933db3b3df588/src/main/java/org/jenkinsci/plugins/workflow/cps/CpsScript.java">CpsScript.java</a>
     *
     * @param pipeline - WorkflowScript
     */
    PipelineUtils(pipeline, disableAnsi = false) {
        this.pipeline = pipeline
        this.disableAnsi = disableAnsi
        try {
            Class.forName('hudson.plugins.ansicolor.AnsiColorBuildWrapper', false, pipeline.getClass().getClassLoader())
            this.hasAnsiSupport = true
        } catch (java.lang.ClassNotFoundException e) {
            this.hasAnsiSupport = false
        }
    }

    /**
     * A Stage that can be skipped based on execute condition.
     * The stage is wrapped in AnsiColorBuildWrapper
     *
     * @param name of stage
     * @param execute boolean flag
     * @param block stage block code
     * @return stage object
     */
    def stage(name, execute, block) {
        if (hasAnsiSupport && !disableAnsi) {
            if (execute) {
                return this.pipeline.wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                    pipeline.echo new AnsiText().bold().a('Executing stage ').fgGreen().a(name).toString()
                    pipeline.stage(name, block)
                    pipeline.echo new AnsiText().bold().a('Stage ').fgGreen().a(name).fgBlack().a(' completed').toString()
                }
            } else {
                return this.pipeline.wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'xterm']) {
                    pipeline.stage(name, {
                        pipeline.echo new AnsiText().bold().a('Skipped stage ').fgYellow().a(name).toString()
                        Utils.markStageSkippedForConditional(name)
                    })
                }
            }
        } else {
            if (execute) {
                return pipeline.stage(name, block)
            } else {
                return pipeline.stage(name, {
                    pipeline.echo "Skipped stage ${name}"
                    Utils.markStageSkippedForConditional(name)
                })
            }
        }
    }

    /**
     * mark exception in build for sending failure notification with slack
     */
    def void markExceptionInBuild() {
        exceptionInBuild = true
    }

    /**
     * get the change log aggregated message in newline separated string
     * @return aggregated change log
     */
    @NonCPS
    def String getChangeLogMessage() {
        def changeLogSets = pipeline.currentBuild.changeSets
        def changeLogMessage = "${pipeline.currentBuild.changeSets.size()} Repository change(s)\n"
        def totalCommits = 0
        def fileChanges = 0
        pipeline
            .currentBuild
            .changeSets
            .each { changeLogSet ->
                totalCommits += changeLogSet.items.size()
                changeLogSet.items.each { changeSet ->
                    fileChanges += changeSet.affectedFiles.size()
                }
            }
        changeLogMessage += "${totalCommits} commit(s)\n"
        changeLogMessage += "${fileChanges} file change(s)\n"
        return changeLogMessage
    }

    /**
     * Send slack notification using slackSend required jenkins-slack-plugin
     *
     * @param execute boolean if false slack notification is not send
     * @param config object
     * @param config.buildStatus optional string expected values STARTED | SUCCESS | UNSTABLE | ABORTED | FAILURE
     * @param config.buildMessage optional string to append header on slack message
     * @param config.changeLogMessage?: optional string for custom change log message to attach
     * @param config.channel?: optional string for sending notification to @individual or #group
     */
    def void slackIt(execute, config = [:]) {
        if (!execute) {
            return
        }
        if (!config.changeLogMessage) {
            config.changeLogMessage = this.getChangeLogMessage()
        }
        def buildStatus
        if (exceptionInBuild) {
            buildStatus = 'FAILURE'
        } else if (config.buildStatus){
            buildStatus = config.buildStatus
        } else {
            buildStatus = pipeline.currentBuild.currentResult
        }
        def message = config.buildMessage ? config.buildMessage : "after ${pipeline.currentBuild.durationString.replace('and counting','')}"
        def colorCode
        switch(buildStatus) {
            case 'STARTED':
                message = "${pipeline.currentBuild.rawBuild.getCauses().get(0).getShortDescription()}"
                colorCode = '#ccc'
                break
            case 'PROGRESS':
                message = "In-progress ${message}"
                colorCode = '#007bff'
                break
            case 'SUCCESS':
                message = "Success ${message}"
                colorCode = 'good'
                break
            case 'UNSTABLE':
                message = "Unstable ${message}"
                colorCode = 'warning'
                break
            case 'ABORTED':
                message = "Aborted ${message}"
                colorCode = '#ccc'
                break
            default:
                buildStatus = 'FAILURE'
                colorCode = 'danger'
                message = "Failed ${message}"
        }
        def attachmentPayload = [[
            fallback: "${pipeline.env.JOB_NAME} execution #${pipeline.env.BUILD_NUMBER} - ${buildStatus}",
            author_link: "",
            author_icon: "",
            title: "${pipeline.env.JOB_NAME}",
            title_link: "${pipeline.env.JOB_URL}",
            color: colorCode,
            fields: [
                [
                    value: "Build <${pipeline.env.RUN_DISPLAY_URL}| #${pipeline.env.BUILD_NUMBER}> - ${message}",
                    short: false
                ]
            ],
            footer: "<${pipeline.env.JENKINS_URL}| Jenkins>",
            ts: new Date().time / 1000
        ]]
        if (buildStatus == 'FAILURE') {
            attachmentPayload[0].fields.add([
                title: "Change log",
                value: "${config.changeLogMessage}\n<${pipeline.env.BUILD_URL}/changes| Details>",
                short: false
            ])
        }
        pipeline.slackSend(channel: config.channel, color: colorCode, attachments: new JsonBuilder(attachmentPayload).toPrettyString())
    }

    /**
     * get root workspace of job. Generally /var/lib/jenkins/jobs/JOB_NAME/workspace
     *
     * @return workspace root path
     */
    def String workspaceRootPath() {
        return pipeline.pwd().replaceFirst(/(.*workspace)@?.*/, '$1')
    }

    /**
     * get Jenkinsfile script path. . Generally /var/lib/jenkins/jobs/JOB_NAME/workspace@script
     *
     * @return workspace script path
     */
    def String workspaceScriptPath() {
        return pipeline.pwd().replaceFirst(/(.*workspace)@?.*/, '$1@script')
    }

    /**
     * Create and <hr /> tag to differentiate sets of parameters or group them as bootstrap-4 SUCCESS styling
     * Requires <a href="https://plugins.jenkins.io/parameter-separator">Parameter Separator</a>
     *
     * @param sectionHeader string or param group name
     * @return Parameter
     */
    def successParamSeparator(sectionHeader) {
        return this.paramSeparator(sectionHeader, GlobalSeparatorStyle, GlobalHeaderSuccessStyle)
    }

    /**
     * Create and <hr /> tag to differentiate sets of parameters or group them as bootstrap-4 DANGER styling
     * Requires <a href="https://plugins.jenkins.io/parameter-separator">Parameter Separator</a>
     *
     * @param sectionHeader string or param group name
     * @return Parameter
     */
    def dangerParamSeparator(sectionHeader) {
        return this.paramSeparator(sectionHeader, GlobalSeparatorStyle, GlobalHeaderDangerStyle)
    }

    /**
     * Create and <hr /> tag to differentiate sets of parameters or group them as bootstrap-4 INFO styling
     * Requires <a href="https://plugins.jenkins.io/parameter-separator">Parameter Separator</a>
     *
     * @param sectionHeader string or param group name
     * @return Parameter
     */
    def infoParamSeparator(sectionHeader) {
        return this.paramSeparator(sectionHeader, GlobalSeparatorStyle, GlobalHeaderInfoStyle)
    }

    /**
     * Create and <hr /> tag to differentiate sets of parameters or group them as bootstrap-4 SECONDARY styling
     * Requires <a href="https://plugins.jenkins.io/parameter-separator">Parameter Separator</a>
     *
     * @param sectionHeader string or param group name
     * @return Parameter
     */
    def infoParamSeparator(sectionHeader) {
        return this.paramSeparator(sectionHeader, GlobalSeparatorStyle, GlobalHeaderSecondaryStyle)
    }

    /**
     * Create and <hr /> tag to differentiate sets of parameters or group them on build parameter page
     * Requires <a href="https://plugins.jenkins.io/parameter-separator">Parameter Separator</a>
     *
     * @param sectionHeader string or param group name
     * @param separatorStyle string for separator CSS style
     * @param sectionHeaderStyle string containing CSS style for section header
     * @return Parameter
     */
    def paramSeparator(sectionHeader, separatorStyle, sectionHeaderStyle) {
        return [ 
            $class: 'ParameterSeparatorDefinition',
            name: 'separator_section',
            sectionHeader: sectionHeader,
            separatorStyle: separatorStyle,
            sectionHeaderStyle: sectionHeaderStyle
        ]
    }

    /**
     * Get JSON pretty string of object
     *
     * @param obj to stringify
     * @return json string
     */
    @NonCPS
    def String jsonPrettyString(obj) {
        return new JsonBuilder(obj).toPrettyString()
    }


    /**
     * Get current node/slaves Operating system Architecture
     *
     * @return possible values for *nix amd64 | i386 | arm For windows 32 | 64
     */
    def String currentArchitecture() {
        if (pipeline.isUnix()) {
            def arch = this.silentBash(script: 'uname -m', returnStdout: true).trim()
            if (arch == 'x86_64') {
                return 'amd64'
            } else {
                return arch
            }
        } else {
            def arch = pipeline.bat(script: '%PROCESSOR_ARCHITECTURE%', returnStdout: true).trim()
            if (arch == 'x86') {
                return '32'
            } else {
                return '64'
            } 
        }
    }

    /**
     * Get current os
     *
     * @return possible values darwin | freebsd | openbsd | solaris | linux | windows
     */
    def String currentOS() {
        if (pipeline.isUnix()) {
            def uname = this.silentBash(script: 'uname', returnStdout: true).trim()
            if (uname.startsWith('Darwin')) {
                return 'darwin'
            } else if (uname.startsWith('FreeBSD')) {
                return 'freebsd'
            } else if (uname.startsWith('OpenBSD')) {
                return 'openbsd'
            } else if (uname.startsWith('SunOS')) {
                return 'solaris'  
            } else {
                return 'linux'
            }
        } else {
            return 'windows'
        }
    }

    /**
     * Don't log bash commands in build while executing
     *
     * @param args string or arguments similar to https://jenkins.io/doc/pipeline/steps/workflow-durable-task-step/#sh-shell-script
     * @return
     */
    def silentBash(args) {
        def shArgs = [:]
        if (args instanceof Map) {
            shArgs = args
        } else {
            shArgs.script = args
        }
        shArgs.script = "#!/bin/bash -e\n${shArgs.script}"
        return pipeline.sh(shArgs)
    }
}
