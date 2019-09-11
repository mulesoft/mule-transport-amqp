Mule AMQP Transport
===================

Read the [complete user guide](http://github.com/mulesoft/mule-transport-amqp/blob/master/GUIDE.md).

Or check the [MuleStudio Plug-In installation and demo documentation](http://mulesoft.github.io/mule-transport-amqp/AMQP-Transport-MuleStudio-Plugin.html).

Supported AMQP Versions
-----------------------

This transport is based on the RabbitMQ Java Client, which is compatible with brokers supporting AMQP version 0.9.1.


Features
--------

- Inbound message receiving via subscription to existing or declared exchanges and queues.
- Outbound message publication to existing or declared exchanges.
- Outbound request-response pattern supported via temporary reply queues.
- Inbound and outbound transaction support, with optional channel self-recovery.
- Synchronous Message requesting with time-out.
- Passive or active-only exchange and queue declarations.
- Support for connection fallback accross a list of AMQP hosts.
- Support of all AMQP's message properties, including custom headers.
- Support of reply to (publishing replies to the default exchange).
- Support of automatic, Mule-driven and manual message acknowledgment.
- Support of manual message rejection.
- Support of manual channel recover.
- Support of the default exchange semantics in outbound endpoints.
- Support of mandatory and immediate publish parameters and handling of returned (undelivered) messages.
- Support of prefetch size and count "quality of service" settings.
- Support of noLocal and exclusive consumers.
- Support of custom exchange and queue arguments.
- Support for SSL connectivity.


Integration Testing
-------------------

Run:

    mvn -Pit clean verify

The integration tests rely on a locally running RabbitMQ broker with a default configuration. They have been run using a RabbitMQ installation from Homebrew (https://www.rabbitmq.com/install-homebrew.html) set up with the configuration found in /mule-transport-amqp/src/it/resources/ssl/rabbit/rabbitmq.config (in case of running with JDK6 please use rabbitmq-jdk6.config which is in the same folder).

If you have a different configuration, you can use the following arguments:

    -DamqpPort=6666 -DamqpSslPort=6665 -DamqpVirtualHost=/ -DamqpUserName=guest \
    -DamqpPassword=guest -DamqpHost=localhost
    
    
SSL Support
-------------

If you have configured SSL support on RabbitMQ as detailed [here](http://www.rabbitmq.com/ssl.html) on the default port you can include the SSL tests by running:

    mvn -Pit -DrunAmqpsTests=true clean verify

You have to create your own self signed certificates for testing purposes.

You can follow the next steps:

1. Create a public and a private key.

	keytool -genkeypair \
	-keystore trustStore.jks \
	-storepass rabbitstore \
	-keyalg RSA \
	-validity 365 \
	-keypass MySecretPassword \
	-alias rabbitmq \
	-dname "CN=*.corp-ext.local,OU=Test, O=Corp, L=Buenos Aires S=BA C=AR"


2. Import the RabbitMQ key pair to the PKCS12 trust store.

	 keytool -importkeystore -srckeystore trustStore.jks \
	-destkeystore keycert.p12 -deststoretype pkcs12 \
	-srcstorepass MySecretPassword -deststorepass MySecretPassword \
	-alias rabbitmq

When prompted use MySecretPassword

3. Convert the key pair file to PEM format

	openssl pkcs12 -in keycert.p12 \
	-out foo.pem -passin pass:MySecretPassword \
	-passout pass:MySecretPassword


4. Extract the encrypted private key

	sed -n '/-----BEGIN ENCRYPTED PRIVATE KEY-----/,/-----END ENCRYPTED PRIVATE KEY-----/p' \
	foo.pem > enc.pem

5. Decrypt the private key

	openssl rsa  -in enc.pem  \
	-out unenc.pem  -passin pass:MySecretPassword

6. Extract the certificate

	sed -n '/-----BEGIN CERTIFICATE-----/,/-----END CERTIFICATE-----/p' \
	foo.pem > cert.pem

7. Copy the certs to the following folders

	cp cert.pem <PROJECT_FOLDER>/src/it/ssl/testca/cacert.pem 
	cp cert.pem <PROJECT_FOLDER>/src/it/ssl/server/cert.pem 
	cp unenc.pem <PROJECT_FOLDER>/src/it/ssl/server/key.pem 
	cp cert.pem <PROJECT_FOLDER>/src/it/ssl/client/cert.pem 
	cp unenc.pem <PROJECT_FOLDER>/src/it/ssl/client/key.pem 
	cp trustStore.jks <PROJECT_FOLDER>/src/it/ssl/client/trustStore.jks

Maven Support
-------------

Add the following repository:

    <repository>
      <id>muleforge-repo</id>
      <name>MuleForge Repository</name>
      <url>https://repository.mulesoft.org/nexus/content/repositories/releases</url>
      <layout>default</layout>
    </repository>

To add the Mule AMQP transport to a Maven project add the following dependency:

    <dependency>
      <groupId>org.mule.transports</groupId>
      <artifactId>mule-transport-amqp</artifactId>
      <version>x.y.z</version>
    </dependency>
