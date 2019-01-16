#!groovy​

properties([
        parameters([
                string(name: 'repo_branch_from_param', defaultValue: '3.8.x', description: '''The branch from the release is going to be done. <b>IMPORTANT</b> doing the release from the "master" branch is not supported.
                                                                                                
                                                                                              For a new minor/major version it is needed to create a new ".x" (1.0.x, 1.1.x, etc) and use that branch. 
                                                                                                
                                                                                              Check this doc for more details: <a href="https://docs.google.com/document/d/17oVKSEqZY4hPi9yB0CvlEqTPEfDrJbsvxP9AFiBgJcI/edit#heading=h.bukd5eto4253">Extensions Release Doc</a>'''),
                booleanParam(name: 'update_version_param', defaultValue: true, description: "In case it is disabled it won't update the version of the poms inside the project (mvn versions:set ...)"),
                string(name: 'repo_version_from_param', defaultValue: '', description: 'Dev version'),
                string(name: 'repo_version_to_param', defaultValue: '', description: 'Version to release'),
                string(name: 'new_dev_version_in_from_branch_param', defaultValue: '', description: 'Next version to use in the dev branch'),
                booleanParam(name: 'skipTests', defaultValue: true, description: 'Check to skip the Run Tests Stage'),
                string(name: 'mvn_test_args_param', defaultValue: 'clean verify -Djarsigner.skip', description: ''),
                booleanParam(name: 'tag_release_param', defaultValue: true, description: '<hr>'),
                booleanParam(name: 'deploy_to_alt_repo_param', defaultValue: false, description: 'Boolean in case to want to change the repository to deploy.'),
                string(name: 'alt_deployment_repo_param', defaultValue: '', description: '''deployment repo URL:

                                                                                        <ul>
                                                                                          <li>mule-ee-releases::default::https://repository-master.mulesoft.org/nexus/content/repositories/ci-releases/</li>
                                                                                          <li>mule-ee-releases::default::https://repository-master.mulesoft.org/nexus/content/repositories/releases-ee/</li>
                                                                                          <li>sonatype-nexus-staging::default::https://oss.sonatype.org/service/local/staging/deploy/maven2/</li>
                                                                                        <ul><hr>'''),
                choice(name: 'slack_channel_param', choices: getDefaultChoiceSlackChannelsList().join("\n"), description: 'Slack channel to send the job notifications'),
                booleanParam(name: 'send_notification_on_completion_param', defaultValue: true, description: 'To send a Slack notification to the channel selected in the next paremeter when the extension was succesful released.'),
                choice(name: 'slack_channel_on_completion_param', choices: getDefaultChoiceSlackChannelsOnCompletionList().join("\n"), description: 'The slack channel to use for the notification when the whole pipeline has finished and the Extension Release has finished.<hr>'),
                booleanParam(name: 'use_different_branch_param', defaultValue: false, description: 'In case this flag is checked, the parameter <b>repo_branch_to_param</b> is going to be used for the name of the branch in place of <b>repo_version_to_param</b>'),
                string(name: 'repo_branch_to_param', defaultValue: '', description: "Branch name used for the release in case 'use_different_branch_param' is checked.<hr>"),
                booleanParam(name: 'dry_run_param', defaultValue: false, description: 'Change the place to deploy to use the test nexus instace used for dry-runs.'),
                string(name: 'pipeline_branch_param', defaultValue: '4.x', description: 'mule-runtime-release repo branch where the Jenkins file is going to be use'),
        ]),
        buildDiscarder(logRotator(artifactDaysToKeepStr: '14', artifactNumToKeepStr: '3', daysToKeepStr: '60', numToKeepStr: '')),
        [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/mulesoft/mule-runtime-release/'],
])


slack_channel = "mule-4-test-release"
slack_user = ""
node('ubuntu-14.04') {

    // Using the wrap BuildUser without a node fails with MissingContextVariableException: Required context class hudson.FilePath is missing
    wrap([$class: 'BuildUser']) {
        echo slack_user

        slack_user = "@" + "${env.BUILD_USER_ID}".replace("@mulesoft.com", "")
    }

}

String jobsNamePrefix = ""
String singleProjectJobsNameSuffix = ""
String jobsNameSuffix = ""
String keystoreId = ""

if (isJenkinsOnPrem()) {
    jobsNamePrefix = getMuleRuntimeReleaseJobFolderPreffix()
    singleProjectJobsNameSuffix = "/${pipeline_branch_param}"
    jobsNameSuffix = "/${repo_branch_from_param}"
    keystoreId = "mule-runtime-secret-file-muelsoft-keystore"
}

try {

    notifySlack()

    def repo_branch_from_arg = repo_branch_from_param

    def repo_version_from_arg = repo_version_from_param
    def repo_branch_to_arg = repo_version_to_param
    if ("${use_different_branch_param}".toBoolean()) {
        repo_branch_to_arg = repo_branch_to_param
    }

    Boolean update_version_arg = "${update_version_param}".toBoolean()
    def repo_version_to_arg = repo_version_to_param
    def new_dev_version_in_from_branch_arg = new_dev_version_in_from_branch_param

    def mvn_test_args = mvn_test_args_param

    def slack_channel_arg = slack_channel_param
    def slack_channel_on_completion_arg = slack_channel_on_completion_param
    def send_notification_on_completion_arg = send_notification_on_completion_param
    def pipeline_branch_arg = pipeline_branch_param

    def dry_run_arg = "${dry_run_param}".toBoolean()
    def tag_release_arg = "${tag_release_param}".toBoolean()
    def deploy_to_alt_repo_arg = "${deploy_to_alt_repo_param}".toBoolean()

    def alt_deployment_repo_arg = ""
    if (deploy_to_alt_repo_arg) {
        alt_deployment_repo_arg = "${alt_deployment_repo_param}"
    }

    def avoid_deploy = false

    currentBuild.displayName = "${BUILD_NUMBER} - version: '${repo_version_to_arg}'"

    if (dry_run_arg) {
        repo_branch_to_arg = "${repo_branch_to_arg}-DRY-RUN"

        tag_release_arg = false
        avoid_deploy = true
        currentBuild.displayName = currentBuild.displayName + " (dry-run)"
    }

    if (dry_run_arg) {

        stage('Delete Remote Dry Run Branches') {

            build job: jobsNamePrefix + 'Mule-4-Single-Project-Delete-Remote-Branch' + singleProjectJobsNameSuffix,
                    parameters: [string(name: 'repo_name_param', value: "mule-transport-amqp"),
                                 string(name: 'repo_branch_param', value: "${repo_branch_to_arg}"),
                                 string(name: 'slack_channel', value: "${slack_channel_arg}"),
                                 string(name: 'pipeline_branch', value: "${pipeline_branch_arg}")]

        }
    }

    stage('Create Branch') {

        build job: jobsNamePrefix + 'Mule-4-Single-Project-Create-Branch' + singleProjectJobsNameSuffix,
                parameters: [string(name: 'repo_name_param', value: "mule-transport-amqp"),
                             string(name: 'branch_from_param', value: "${repo_branch_from_arg}"),
                             string(name: 'branch_to_param', value: "${repo_branch_to_arg}"),
                             string(name: 'slack_channel', value: "${slack_channel_arg}"),
                             string(name: 'pipeline_branch', value: "${pipeline_branch_arg}")]

    }

    stage('Update Version') {

        if (update_version_arg) {
            build job: jobsNamePrefix + 'Mule-3-AMQP-Transport-Update-Version' + jobsNameSuffix,
                    parameters: [string(name: 'branch_param', value: "${repo_branch_to_arg}"),
                                 string(name: 'version_from_param', value: "${repo_version_from_arg}"),
                                 string(name: 'new_version_param', value: "${repo_version_to_arg}"),
                                 string(name: 'slack_channel', value: "${slack_channel_arg}")]

        } else {
            echo "[INFO]: Update Version Stage ignored due to 'update_version_arg' flag wasn't checked."
        }

    }


    if (!"${skipTests}".toBoolean()) {
        stage('Run Tests') {

            build job: jobsNamePrefix + 'Mule-4-Single-Project-Tests' + singleProjectJobsNameSuffix,
                    parameters: [string(name: 'repo_name_param', value: "mule-transport-amqp"),
                                 string(name: 'branch_param', value: "${repo_branch_to_arg}"),
                                 string(name: 'mvn_test_args', value: "${mvn_test_args}"),
                                 string(name: 'slack_channel', value: "${slack_channel_arg}"),
                                 string(name: 'pipeline_branch', value: "${pipeline_branch_arg}")]

        }
    }
    stage('Release artifacts') {

        releaseArtifacts(repo_branch_to_arg, deploy_to_alt_repo_arg, alt_deployment_repo_arg, avoid_deploy, keystoreId)
    }

    if (!dry_run_arg) {

        if (tag_release_arg) {
            stage('Tag release') {

                build job: jobsNamePrefix + 'Mule-4-Single-Project-Tag-Release' + singleProjectJobsNameSuffix,
                        parameters: [string(name: 'repo_name_param', value: "mule-transport-amqp"),
                                     string(name: 'branch_param', value: "${repo_branch_to_arg}"),
                                     string(name: 'release_version_param', value: "${repo_version_to_arg}"),
                                     string(name: 'slack_channel', value: "${slack_channel_arg}"),
                                     string(name: 'pipeline_branch', value: "${pipeline_branch_arg}")]
            }
        }

        stage('Delete Remote Branch') {

            build job: jobsNamePrefix + 'Mule-4-Single-Project-Delete-Remote-Branch' + singleProjectJobsNameSuffix,
                    parameters: [string(name: 'repo_name_param', value: "mule-transport-amqp"),
                                 string(name: 'repo_branch_param', value: "${repo_branch_to_arg}"),
                                 string(name: 'slack_channel', value: "${slack_channel_arg}"),
                                 string(name: 'pipeline_branch', value: "${pipeline_branch_arg}")]

        }

        stage('Update Dev Branch Version') {

            build job: jobsNamePrefix + 'Mule-3-AMQP-Transport-Update-Version' + jobsNameSuffix,
                    parameters: [string(name: 'branch_param', value: "${repo_branch_from_arg}"),
                                 string(name: 'version_from_param', value: "${repo_version_from_arg}"),
                                 string(name: 'new_version_param', value: "${new_dev_version_in_from_branch_arg}"),
                                 string(name: 'slack_channel', value: "${slack_channel_arg}")]

        }

        waitForInput("release notes", "Don't forget the Release Notes of mule-transport-amqp version: '${repo_version_to_arg}'. \nUse this link ${env.BUILD_URL}/input to send the Release notification when they are ready.", slack_user)

        if ("${send_notification_on_completion_arg}".toBoolean()) {
            String displayNameWithRepoNameInSlackBold = "${currentBuild.displayName.replaceAll("mule-transport-amqp", "*mule-transport-amqp*")}"
            slackSend(channel: "${slack_channel_on_completion_arg}", color: 'good', message: "SUCCESS: `${env.JOB_NAME}` ${displayNameWithRepoNameInSlackBold}:\n${env.BUILD_URL}")
        }

    }

} catch (e) {
    currentBuild.result = 'FAILURE'
    throw e
} finally {

    notifySlack(currentBuild.result)
}

def releaseArtifacts(branch_param, deploy_to_alt_repo, alt_deployment_repo, avoid_deploy_param, keystore_id) {
    node('ubuntu-14.04') {
        workspaceLocation = pwd()
        repositoryLocation = workspaceLocation + '/.repository'

        def deployment_repo = ""
        if ("${deploy_to_alt_repo}".toBoolean()) {
            deployment_repo = "-DaltDeploymentRepository=${alt_deployment_repo}"
        }

        String mvn_goal = "deploy"

        if ("${avoid_deploy_param}".toBoolean()) {
            mvn_goal = "install"
        }

        withCredentials([file(credentialsId: keystore_id, variable: 'keystore')]) {

            String mvn_args = "-X -e -U -Dkeystore.path=${keystore} -Dalias=mulesoft -Dstorepass='mulemani\$here' -Dkeypass='mulemani\$here' -Dsignature.key=8233e4ce-a587-4eeb-b1c6-7ac25f12adef -Pupdate-site ${deployment_repo}"

            stage('Prepare workspace') { // for display purposes
                deleteDir()
                installJdk()
                installMaven()
            }

            stage("Clone Repo") {
                sh "git clone --branch ${branch_param} git@github.com:mulesoft/mule-transport-amqp.git"
                dir("mule-transport-amqp") {
                    sh 'git --no-pager log --decorate=short --pretty=oneline -n1'
                }
            }

            stage('Build and Deploy Project') {
                dir("mule-transport-amqp") {
                    mvn("clean ${mvn_goal} ${mvn_args}")
                }
            }
        }
    }
}

def waitForInput(String projectName, String message, String slackChannel = "${slack_channel}") {
    stage("Wait for ${projectName}") {
        slackSend(channel: "${slackChannel}", color: "good", message: "${message}")

        input id: "release-${projectName.replaceAll(" ", "-")}", message: "Is ${projectName} Released?"

    }
}

