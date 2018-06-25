#!/bin/sh
set -x
if [ -z $5 ]; then
    echo "Usage: \033[1mupdate-version.sh\033[0m MAVEN_VERSION_FROM MAVEN_VERSION_TO OSGI_VERSION_FROM OSGI_VERSION_TO <MAC_OSK_SED> <AVOID_MVN_SET_VERSIONS>";
    echo
    echo "Example: \033[1mupdate-version.sh\033[0m 3.7.0-SNAPSHOT 3.7.0 3.7.0.qualifier 3.7.0 false";
    echo
    echo "Example: \033[1mupdate-version.sh\033[0m 3.7.0-SNAPSHOT 3.7.0 3.7.0.qualifier 3.7.0 true true";
    exit 1
fi

echo update-version "MAVEN_VERSION_FROM=${1} MAVEN_VERSION_TO=${2} OSGI_VERSION_FROM=${3} OSGI_VERSION_TO=${4} MAC_OSK_SED=${5} AVOID_MVN_SET_VERSIONS=${6}"

MAVEN_VERSION_FROM=${1}
MAVEN_VERSION_TO=${2}
OSGI_VERSION_FROM=${3}
OSGI_VERSION_TO=${4}
MAC_OSX_SED=${5}
AVOID_MVN_SET_VERSIONS=${6}

if [ -z "$AVOID_MVN_SET_VERSIONS" ] || [ "$AVOID_MVN_SET_VERSIONS" != "true" ]; then
    mvn versions:set versions:commit -DnewVersion="${MAVEN_VERSION_TO}"
fi
if [ "$MAC_OSX_SED" = "true" ]; then
    sed -e "s/target\/mule-transport-amqp-${MAVEN_VERSION_FROM}.jar/target\/mule-transport-amqp-${MAVEN_VERSION_TO}.jar/g" -i '' ${MAC_OSX_SED_INLINE_BACKUP_FILE} amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/build.properties
    sed -e "s/target\/mule-transport-amqp-${MAVEN_VERSION_FROM}.zip/target\/mule-transport-amqp-${MAVEN_VERSION_TO}.zip/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/build.properties
    find . -name MANIFEST.MF -exec sed -e "s/Bundle-Version:.*/Bundle-Version: ${OSGI_VERSION_TO}/g" -i '' {} \;
    find . -name feature.xml -exec sed -e "s/version=.*${OSGI_VERSION_FROM}/version=\"${OSGI_VERSION_TO}/g" -i '' {} \;
    sed -e "s/contributionJar=\"mule-transport-amqp-${MAVEN_VERSION_FROM}.jar\"/contributionJar=\"mule-transport-amqp-${MAVEN_VERSION_TO}.jar\"/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml
    sed -e "s/contributionLibs=\"mule-transport-amqp-${MAVEN_VERSION_FROM}.zip\"/contributionLibs=\"mule-transport-amqp-${MAVEN_VERSION_TO}.zip\"/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml
    sed -e "s/version=\"*${MAVEN_VERSION_FROM}\"/version=\"${MAVEN_VERSION_TO}\"/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml
    sed -e "s/version=\"${OSGI_VERSION_FROM}\"/version=\"${OSGI_VERSION_TO}\"/g" -i '' amqp-eclipse-plugin/org.mule.tooling.amqp/feature.xml.template
else
    sed -e "s/target\/mule-transport-amqp-${MAVEN_VERSION_FROM}.jar/target\/mule-transport-amqp-${MAVEN_VERSION_TO}.jar/g" -i amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/build.properties
    sed -e "s/target\/mule-transport-amqp-${MAVEN_VERSION_FROM}.zip/target\/mule-transport-amqp-${MAVEN_VERSION_TO}.zip/g" -i amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/build.properties
    find . -name MANIFEST.MF -exec sed -e "s/Bundle-Version:.*/Bundle-Version: ${OSGI_VERSION_TO}/g" -i {} \;
    find . -name feature.xml -exec sed -e "s/version=.*${OSGI_VERSION_FROM}/version=\"${OSGI_VERSION_TO}/g" -i {} \;
    sed -e "s/contributionJar=\"mule-transport-amqp-${MAVEN_VERSION_FROM}.jar\"/contributionJar=\"mule-transport-amqp-${MAVEN_VERSION_TO}.jar\"/g" -i amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml
    sed -e "s/contributionLibs=\"mule-transport-amqp-${MAVEN_VERSION_FROM}.zip\"/contributionLibs=\"mule-transport-amqp-${MAVEN_VERSION_TO}.zip\"/g" -i amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml
    sed -e "s/version=\"*${MAVEN_VERSION_FROM}\"/version=\"${MAVEN_VERSION_TO}\"/g" -i amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml
    sed -e "s/version=\"${OSGI_VERSION_FROM}\"/version=\"${OSGI_VERSION_TO}\"/g" -i amqp-eclipse-plugin/org.mule.tooling.amqp/feature.xml.template
fi
