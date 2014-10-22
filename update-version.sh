#!/bin/sh
if [ -z $4 ]; then
    echo "Usage: \033[1mupdate-version.sh\033[0m MAVEN_VERSION_FROM MAVEN_VERSION_TO OSGI_VERSION_FROM OSGI_VERSION_TO";
    exit 1
fi
echo update-version "$1 $2 $3 $4"
mvn versions:set versions:commit -DnewVersion="$2"
sed -e "s/mule-transport-amqp-$1.jar/mule-transport-amqp-$2.jar/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/build.properties
sed -e "s/mule-transport-amqp-$1.zip/mule-transport-amqp-$2.zip/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/build.properties
find . -name MANIFEST.MF -exec sed -e "s/Bundle-Version:.*/Bundle-Version: $4/g" -i '' {} \; 
find . -name feature.xml -exec sed -e "s/version=.*$3/version=\"$4/g" -i '' {} \; 
sed -e "s/contributionJar=\"mule-transport-amqp-$1.jar\"/contributionJar=\"mule-transport-amqp-$2.jar\"/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml 
sed -e "s/contributionLibs=\"mule-transport-amqp-$1.zip\"/contributionJar=\"mule-transport-amqp-$2.zip\"/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml 
sed -e "s/version=.*$1/version=\"$2/g" -i '' amqp-eclipse-plugin/org.mule.tooling.ui.contribution.amqp/plugin.xml