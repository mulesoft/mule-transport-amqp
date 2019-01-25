String deployment_environment_choices = ["devx", "qax", "stgx", "prod", "prod-eu"].join("\n")

properties([
        parameters([
                choice(name: 'deployment_environment_param', choices: deployment_environment_choices, description: 'Target Environment'),
                string(name: 'new_version_param', defaultValue: '', description: ''),
                choice(name: 'slack_channel', choices: getDefaultChoiceSlackChannelsList().join("\n"), description: 'Slack channel to send the job notifications')
        ]),
        buildDiscarder(logRotator(artifactDaysToKeepStr: '14', artifactNumToKeepStr: '3', daysToKeepStr: '60', numToKeepStr: '')),
        [$class: 'GithubProjectProperty', displayName: '', projectUrlStr: 'https://github.com/mulesoft/mule-transport-amqp'],
])

def environments = [
        'devx'   : 'exchange-maven-facade-devx::https://maven.devx.anypoint.mulesoft.com/api/v1/organizations/68ef9520-24e9-4cf2-b2f5-620025690913/maven',
        'qax'    : 'exchange-maven-facade-qax::https://maven.qax.anypoint.mulesoft.com/api/v1/organizations/68ef9520-24e9-4cf2-b2f5-620025690913/maven',
        'stgx'   : 'exchange-maven-facade-stgx::https://maven.stgx.anypoint.mulesoft.com/api/v1/organizations/68ef9520-24e9-4cf2-b2f5-620025690913/maven',
        'stgxdr' : 'exchange-maven-facade-stgxdr::https://maven.stgxdr.anypoint.mulesoft.com/api/v1/organizations/68ef9520-24e9-4cf2-b2f5-620025690913/maven',
        'prod'   : 'exchange-maven-facade-prod::https://maven.anypoint.mulesoft.com/api/v1/organizations/68ef9520-24e9-4cf2-b2f5-620025690913/maven',
        'prod-eu': 'exchange-maven-facade-prod-eu::https://maven.eu1.anypoint.mulesoft.com/api/v1/organizations/e0b4a150-f59b-46d4-ad25-5d98f9deb24a/maven']

currentBuild.displayName = "${env.BUILD_ID}: - Version. ${new_version_param} - Env. ${deployment_environment_param}"

// Configuration

String groupId = "org.mule"
String groupIdStudioPlugin = "${groupId}.modules"
String groupIdJar = "${groupId}.transports"

String artifactId = "mule-transport-amqp"
String artifactIdStudioPlugin = "mule-transport-amqp-studio"

String version = new_version_param

String nexusRepositoryId = "mule-ee-releases"
String nexusRepositoryUrl = "https://repository-master.mulesoft.org/nexus-ee/content/repositories/releases/"

String[] parts = environments["${deployment_environment_param}"].split("::")
String exchangeMvnRepositoryId = parts[0]
String exchangeMvnRepositoryUrl = parts[1]

node('ubuntu-14.04') {
    try {
//    notifySlack('STARTED', slack_channel)

        timestamps {

            stage('Prepare workspace') {
                deleteDir()
                installJdk()
                installMaven()
            }

            stage('Deploy Studio artifact') {

                mvn "org.apache.maven.plugins:maven-dependency-plugin:2.1:get" +
                        " -DrepositoryId=$nexusRepositoryId" +
                        " -DrepoUrl=$nexusRepositoryUrl" +
                        " -Dartifact=$groupIdStudioPlugin:$artifactIdStudioPlugin:$version:zip"

                mvn "dependency:copy" +
                        " -Dartifact=$groupIdStudioPlugin:$artifactIdStudioPlugin:$version:pom" +
                        " -DoutputDirectory=."

                mvn "dependency:copy" +
                        " -Dartifact=$groupIdStudioPlugin:$artifactIdStudioPlugin:$version:zip:studio-plugin" +
                        " -DoutputDirectory=."

                echo "exchangeMvnRepositoryId: $exchangeMvnRepositoryId"
                echo "exchangeMvnRepositoryUrl: $exchangeMvnRepositoryUrl"

//            mvn "org.apache.maven.plugins:maven-deploy-plugin:2.8.2:deploy-file -X" +
//                    " -DgroupId=$groupIdStudioPlugin" +
//                    " -DartifactId=$artifactIdStudioPlugin" +
//                    " -Dversion=$version" +
//                    " -Dpackaging=zip" +
//                    " -Dfile=$artifactIdStudioPlugin-$version-studio-plugin.zip" +
//                    " -Dclassifier=studio-plugin" +
//                    " -DpomFile=$artifactIdStudioPlugin-$version.pom" +
//                    " -DrepositoryId=$exchangeMvnRepositoryId" +
//                    " -Durl=$exchangeMvnRepositoryUrl"
            }

            stage('Deploy Transport artifact') {

                mvn "org.apache.maven.plugins:maven-dependency-plugin:2.1:get" +
                        " -DrepositoryId=$nexusRepositoryId" +
                        " -DrepoUrl=$nexusRepositoryUrl" +
                        " -Dartifact=$groupIdJar:$artifactId:$version"

                mvn "dependency:copy" +
                        " -Dartifact=$groupIdJar:$artifactId:$version:pom" +
                        " -DoutputDirectory=."

                mvn "dependency:copy" +
                        " -Dartifact=$groupIdJar:$artifactId:$version:jar" +
                        " -DoutputDirectory=."

//            mvn "org.apache.maven.plugins:maven-deploy-plugin:2.8.2:deploy-file -X" +
//                    " -DgroupId=$groupIdJar" +
//                    " -DartifactId=$artifactId" +
//                    " -Dversion=$version" +
//                    " -Dpackaging=jar" +
//                    " -Dfile=$artifactId-$version.jar" +
//                    " -DgeneratePom=false" +
//                    " -DrepositoryId=$exchangeMvnRepositoryId" +
//                    " -Durl=$exchangeMvnRepositoryUrl"
            }
        }

    } catch (e) {
        currentBuild.result = 'FAILURE'
        throw e
    } finally {

        stage('Clean workspace') {
            deleteDir()
        }

//    notifySlack(currentBuild.result, slack_channel)
    }
}
