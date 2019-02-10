package org.lonkar.jenkinsutils

import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import groovy.json.*
import hudson.model.*
import java.io.Serializable

class PipelineUtils implements Serializable {

    private static String CommonHeaderStyle = 'font-family: Roboto, sans-serif !important;text-align: center;margin-left: -250px;font-weight: bold;'
    private static String GlobalSeparatorStyle = 'display: none;'
    private static String GlobalHeaderDangerStyle = CommonHeaderStyle + 'background: #f8d7da;color: #721c24;'
    private static String GlobalHeaderSuccessStyle = CommonHeaderStyle + 'background: #d4edda;color: #155724;'
    private static String GlobalHeaderInfoStyle = CommonHeaderStyle + 'background: #d1ecf1;color: #0c5460;'

    def pipeline
    def exceptionInBuild

    PipelineUtils(pipeline) {
        this.pipeline = pipeline
    }

    def stage(name, execute, block) {
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
    }

    def markExceptionInBuild() {
        exceptionInBuild = true
    }

    @NonCPS
    def getChangeLogMessage() {
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
     * @param execute: boolean - if false slack notification is not send
     * @param config {
         buildStatus: optional string 
         buildMessage: optional string
         changeLogMessage?: optional string
         channel?: optional string
        }
     */
    def slackIt(execute, config = [:]) {
        if (!execute) {
            return
        }
        if (!config.changeLogMessage) {
            config.changeLogMessage = this.getChangeLogMessage()
        }
        println "pipeline.currentBuild -> " + pipeline.currentBuild
        def buildStatus;
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
                colorCode = 'danger'
                message = "Failed ${message}"
        }
        def attachmenPayload = [[
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
            // footer_icon: "https://cdn.pbrd.co/images/HKfyRXl.png",
            ts: new Date().time / 1000
        ]]
        if (buildStatus == 'FAILURE') {
            attachmenPayload[0].fields.add([  
                title: "Change log",
                value: "${config.changeLogMessage}\n<${pipeline.env.BUILD_URL}/changes| Details>",
                short: false
            ])
        }
        pipeline.slackSend(channel: config.channel, color: colorCode, attachments: new JsonBuilder(attachmenPayload).toPrettyString())
    }

    def workspaceRootPath() {
        return pipeline.pwd().replaceFirst(/(.*workspace)@?.*/)
    }

    def workspaceSciptPath() {
        return pipeline.pwd().replaceFirst(/(.*workspace)@?.*/, '$1@script')
    }

    def successParamSeperator(sectionHeader) {
        return this.paramSeperator(sectionHeader, GlobalSeparatorStyle, GlobalHeaderSuccessStyle)
    }

    def dangerParamSeperator(sectionHeader) {
        return this.paramSeperator(sectionHeader, GlobalSeparatorStyle, GlobalHeaderDangerStyle)
    }

    def infoParamSeperator(sectionHeader) {
        return this.paramSeperator(sectionHeader, GlobalSeparatorStyle, GlobalHeaderInfoStyle)
    }

    def paramSeperator(sectionHeader, separatorStyle, sectionHeaderStyle) {
        return [ 
            $class: 'ParameterSeparatorDefinition',
            name: 'seperator_section',
            sectionHeader: sectionHeader,
            separatorStyle: separatorStyle,
            sectionHeaderStyle: sectionHeaderStyle
        ]
    }

    @NonCPS
    def jsonPrettyString(obj) {
        return new JsonBuilder(obj).toPrettyString()
    }

    def currentArchitecure() {
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

    def currentOS() {
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