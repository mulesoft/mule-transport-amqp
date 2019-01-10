properties([
        parameters([
                string(name: 'version_to_release', defaultValue: '', description: '')
        ]),
        buildDiscarder(logRotator(artifactDaysToKeepStr: '14', artifactNumToKeepStr: '3', daysToKeepStr: '60', numToKeepStr: '')),
])

node('hi-speed||ubuntu-14.04||ubuntu-18.04') {

    try {

        currentBuild.displayName = "${env.BUILD_ID} - version: '${version_to_release}'"

        stage('Create Branch') {
            sh "git branch -D ${version_to_release} || true" // In case there is a local branch with the same name
            sh "git checkout -b ${version_to_release}"
        }

//        stage('Push branch') {
//            dir("${repo_name_param}") {
//                sh "git push origin ${branch_to_param}:${branch_to_param}"
//            }
//        }

    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {

        stage('Clean workspace') {
            deleteDir()
        }

    }
}

