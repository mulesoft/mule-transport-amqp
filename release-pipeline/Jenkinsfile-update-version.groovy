properties([
        parameters([
                string(name: 'branch_param', defaultValue: '', description: 'Repository From Branch'),
                string(name: 'version_from_param', defaultValue: '', description: ''),
                string(name: 'new_version_param', defaultValue: '', description: ''),
                choice(name: 'slack_channel', choices: getDefaultChoiceSlackChannelsList().join("\n"), description: 'Slack channel to send the job notifications')
        ]),
        buildDiscarder(logRotator(artifactDaysToKeepStr: '14', artifactNumToKeepStr: '3', daysToKeepStr: '60', numToKeepStr: '')),
        [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/mulesoft/mule-runtime-release/'],
])

node('ubuntu-14.04') {

    try {

        def version_from_arg = version_from_param
        def new_version_arg = new_version_param

        def osgi_version_from_arg = version_from_param.replaceAll("-SNAPSHOT", ".qualifier")
        def osgi_version_to_arg = new_version_arg.replaceAll("-SNAPSHOT", ".qualifier")

        stage('Prepare workspace') { // for display purposes
            deleteDir()
            installJdk()
            installMaven()
        }

        stage("Clone Repo") {
            sh "git clone --branch ${branch_param} git@github.com:mulesoft/mule-transport-amqp.git"
        }

        currentBuild.displayName = "${env.BUILD_ID} - version ${new_version_arg} in branch ${branch_param}"

        stage('Update Version') {
            dir("mule-transport-amqp") {
                sh "./update-version.sh ${version_from_arg} ${new_version_arg} ${osgi_version_from_arg} ${osgi_version_to_arg} false true"
                mvn("org.codehaus.mojo:versions-maven-plugin:2.4:set org.codehaus.mojo:versions-maven-plugin:2.4:commit -DnewVersion=\"${new_version_arg}\"")
            }
        }

        stage('Commit and push changes') {
            dir("mule-transport-amqp") {
                setGlobalGitUserNameAndEmail()
                sh "git commit -a -m 'Update version to ${new_version_arg}'"
                sh "git push origin refs/heads/${branch_param}:refs/heads/${branch_param}"
            }
        }
    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {

        stage('Clean workspace') {
            deleteDir()
        }

        notifySlack(currentBuild.result, slack_channel)
    }
}
