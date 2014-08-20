Mule AMQP Transport - User Guide
================================

Welcome to AMQP
---------------

The Advanced Message Queuing Protocol (AMQP) is an open standard application layer protocol for message-oriented middleware. The defining features of AMQP are message orientation, queuing, routing (including point-to-point and publish-and-subscribe), reliability and security ([Wikipedia](http://en.wikipedia.org/wiki/AMQP)).
The specifications are available [on-line](http://amqp.org) and several broker implementations exist, like the very popular [VMware RabbitMQ](http://www.rabbitmq.com) and [Apache Qpid](http://qpid.apache.org/).   

AMQP is built around a few easy to grasp concepts:

- Clients connect via channels to AMQP brokers in order to send or receive [messages](http://en.wikipedia.org/wiki/AMQP#Messages),
- They can publish messages to [exchanges](http://en.wikipedia.org/wiki/AMQP#Exchanges),
- Messages published to exchanges a routed to [queues](http://en.wikipedia.org/wiki/AMQP#Queues) where they get accumulated for later consuming.
- The queue that will constitute the final destination of a message is not known by the message publisher: it is determined by the type of the exchange and a piece of meta information known as the "routing key".
- It is possible for a message to end-up nowhere if no queue has been bound to the targeted exchange or if no routing rules haven't matched any existing queue.
- There are four main types of exchanges: [direct, fanout, topic and headers](http://en.wikipedia.org/wiki/AMQP#Exchange_types_and_the_effect_of_bindings). 
- Clients interested in consuming messages must create queues and [bind](http://en.wikipedia.org/wiki/AMQP#Bindings) these queues to exchanges.
- Queue and exchange declaration is an idempotent operation hence it is common practice to declare them on each client startup. 
	
> ** AMQP for the JMS savvy **
>
> If you're a Java developer, chances are you have been exposed to JMS and are wondering how AMQP differs from JMS.
>
> In a nutshell, the main differences are the following:
>
> - AMQP defines both an API and a wire-format, ensuring compatibility between implementations (JMS only defines an API),
>
> - In JMS you publish directly to destinations (queues or topic) while in AMQP you publish to exchanges to which queues are bound (or not), which decouples the producer from the final destination of its messages.
>
> - For some types of exchanges, the delivery to the final destination depends on a routing key, a simple string that provides the necessary meta-information for successfully routing the message (unlike in JMS where the *name* of the destination is all it's needed).  

Core Transport Principles
-------------------------

The Mule AMQP Transport is an abstraction built on top of the previously introduced AMQP constructs: connection, channel, exchanges, queues and messages.

The transport hides the low level concepts, like dealing with channels, but gives a great deal of control on all the constructs it encapsulates allowing you to experience the richness of AMQP without the need to code to its API.

Here is a quick review of the main configuration elements you'll deal with when using the transport:

- The *connector* element take care of establishing the connection to AMQP brokers, deals with channels and manages a set of common properties that will be shared by all consumers or publishers that will use this connector.
- The *inbound endpoint* elements are in charge of consuming messages from AMQP queues and route them to your components, transformers, routers or other outbound endpoints as defined in your Mule configuration.
- The *outbound endpoint* elements are in charge of publishing messages to AMQP exchanges from your Mule configuration. 

### Message payload and properties

The AMQP transport works with another abstraction that is very important to understand: the *Mule Message*. A Mule Message is a transport agnostic abstraction that encapsulates a payload and meta-information named properties: this allows your different configuration element to deal with messages while being oblivious to the source or destination of such messages.

An AMQP message also has the notion of a payload (in bytes) and message properties, which are composed of a set of pre-defined ones (know as basic properties) and any additional custom ones. Moreover, when a message is delivered, extra properties, known as envelope properties, are also available.

The AMQPtransport will create Mule Messages with byte[] payloads for inbound messages and will rely on Mule's auto transformation infrastructure to extract byte[] payloads from Mule Messages for outbound messages. Should you need to use a particular payload representation (for example XML or JSON), it is up to you to add the necessary transformers to perform the desired serialization/deserialization steps.  

The transport also takes care of making the properties of inbound messages available as standard Mule Message properties and, conversely, converting properties of Mule Messages into AMQP properties for outbound messages.

Here is the list of properties supported by the transport:

<!--
    Generated with: org.mule.transport.amqp.AmqpConstants.main()
-->
<table>
<tr><th>Basic Properties</th><th>Envelope Properties</th><th>Technical Properties</th></tr>
<tr><td>app-id</td><td>delivery-tag</td><td>amqp.headers</td></tr>
<tr><td>content-encoding</td><td>exchange</td><td>consumer-tag</td></tr>
<tr><td>content-type</td><td>redelivered</td><td>amqp.channel</td></tr>
<tr><td>correlation-id</td><td>routing-key</td><td>amqp.delivery-tag</td></tr>
<tr><td>delivery_mode</td><td></td><td>amqp.return.listener</td></tr>
<tr><td>expiration</td><td></td><td>amqp.return.reply-code</td></tr>
<tr><td>message-id</td><td></td><td>amqp.return.reply-text</td></tr>
<tr><td>priority</td><td></td><td>amqp.return.exchange</td></tr>
<tr><td>reply-to</td><td></td><td>amqp.return.routing-key</td></tr>
<tr><td>timestamp</td><td></td><td></td></tr>
<tr><td>type</td><td></td><td></td></tr>
<tr><td>user-id</td><td></td><td></td></tr>
</table>

On top of that all custom headers defined in the AMQP basic properties, which are available in a map under the `amqp.headers` inbound property, are added as standard inbound properties.

Configuration Reference
-----------------------

All the configuration parameters supported by the connector and endpoint configuration elements are described in this section.

### Connector Attributes
<!--
	Generated with: http://svn.codehaus.org/mule/branches/mule-2.0.x/tools/schemadocs/src/main/resources/xslt/single-element.xsl
	Parameter     : elementName=connector
-->

The AMQP connector defines what broker to connect to, which credentials to use when doing so and all the common properties used by the inbound and outbound endpoints using this connector.

It is possible to create several connectors connected to the same broker for the purpose of having different sets of common properties that the endpoints will use. 

<table class="confluenceTable">
  <tr>
    <th style="width:10%" class="confluenceTh">Name</th><th style="width:10%" class="confluenceTh">Type</th><th style="width:10%" class="confluenceTh">Required</th><th style="width:10%" class="confluenceTh">Default</th><th class="confluenceTh">Description</th>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">host</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">localhost</td><td class="confluenceTd">
      <p>
          The main AMQP broker host to connect to.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">port</td><td style="text-align: center" class="confluenceTd">port number</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">5672</td><td class="confluenceTd">
      <p>
          The port to use to connect to the main
          AMQP broker.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">fallbackAddresses</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
          A comma-separated list of "host:port" or
          "host", defining fallback brokers to attempt connection
          to, should the connection to main broker fail.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">virtualHost</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">/</td><td class="confluenceTd">
      <p>
          The virtual host to connect to on the
          AMQP broker.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">username</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">guest</td><td class="confluenceTd">
      <p>
          The user name to use to connect to the
          AMQP broker.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">password</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">guest</td><td class="confluenceTd">
      <p>
          The password to use to connect to the
          AMQP broker.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">deliveryMode</td><td style="text-align: center" class="confluenceTd"><b>PERSISTENT</b> / <b>NON_PERSISTENT</b></td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">PERSISTENT</td><td class="confluenceTd">
      <p>
          The delivery mode to use when publishing
          to the AMQP broker.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">priority</td><td style="text-align: center" class="confluenceTd"></td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">0</td><td class="confluenceTd">
      <p>
          The priority to use when publishing to
          the AMQP broker.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">mandatory</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">false</td><td class="confluenceTd">
      <p>
          This flag tells the server how to react
          if the message cannot be
          routed to a queue. If this flag is
          set to true, the server will throw an exception for any
          unroutable message. If this flag is false, the server
          silently drops the message.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">immediate</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">false</td><td class="confluenceTd">
      <p>
          This flag tells the server how to react
          if the message cannot be
          routed to a queue consumer
          immediately. If this flag is set to true, the server
          will
          throw an exception for any undeliverable message. If
          this
          flag is false, the server will queue the message, but
          with
          no guarantee that it will ever be consumed.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">default-return-endpoint-ref</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
          Reference to an endpoint to which AMQP
          returned message should be
          dispatched to.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">ackMode</td><td style="text-align: center" class="confluenceTd"><b>AMQP_AUTO</b> / <b>MULE_AUTO</b> / <b>MANUAL</b></td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">AMQP_AUTO</td><td class="confluenceTd">
      <p>
          The acknowledgment mode to use when
          consuming from the AMQP broker.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">prefetchSize</td><td style="text-align: center" class="confluenceTd">integer</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">0</td><td class="confluenceTd">
      <p>
          The maximum amount of content (measured
          in octets) that the server will deliver, 0 if unlimited.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">prefetchCount</td><td style="text-align: center" class="confluenceTd">integer</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">0</td><td class="confluenceTd">
      <p>
          The maximum number of messages that the
          server will deliver, 0 if unlimited.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">noLocal</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">false</td><td class="confluenceTd">
      <p>
          If the no-local field is set the server
          will not send messages to the connection that published
          them.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">exclusiveConsumers</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">false</td><td class="confluenceTd">
      <p>
          Set to true if the connector should only
          create exclusive consumers.
        </p>
    </td>
  </tr>
    <tr>
    <td rowspan="1" class="confluenceTd">numberOfConsumers</td><td style="text-align: center" class="confluenceTd">integer</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">4</td><td class="confluenceTd">
      <p>
          The number of concurrent consumer threads that will be used to receive AMQP messages.
        </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">activeDeclarationsOnly</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd">false</td><td class="confluenceTd">
      <p>
          Defines if the connector should only do
          active exchange and queue declarations or can also perform
          passive declarations to enforce their existence.
        </p>
    </td>
  </tr>
</table>

### Endpoint Attributes

Endpoint attributes are interpreted differently if they are used on inbound or outbound endpoints. For example, routingKey on an inbound endpoint is meant to be used for queue binding while it is used as basic publish parameter on outbound endpoints. 

<!--
	Generated with: http://svn.codehaus.org/mule/branches/mule-2.0.x/tools/schemadocs/src/main/resources/xslt/single-element.xsl
	Parameter     : elementName=endpoint
-->
<table class="confluenceTable">
  <tr>
    <th style="width:10%" class="confluenceTh">Name</th><th style="width:10%" class="confluenceTh">Type</th><th style="width:10%" class="confluenceTh">Required</th><th style="width:10%" class="confluenceTh">Default</th><th class="confluenceTh">Description</th>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">exchangeName</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      The exchange to publish to or bind queues to.
      Use <b>AMQP.DEFAULT.EXCHANGE</b> for the default exchange
      (the previous approach that consists in leaving blank or omitting `exchangeName` for the default exchange still works but is not recommended).
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">queueName</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      The queue name to consume from.
      Leave blank or omit for using a new private exclusive server-named queue.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">routingKey</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      Comma-separated routing keys to use when binding a queue or publishing a message.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">consumerTag</td><td style="text-align: center" class="confluenceTd">string</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      A client-generated consumer tag to establish context.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">exchangeType</td><td style="text-align: center" class="confluenceTd"><b>fanout</b> / <b>direct</b> / <b>topic</b> / <b>headers</b></td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      The type of exchange to be declared.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">exchangeDurable</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      The durability of the declared exchange.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">exchangeAutoDelete</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      Specifies if the declared exchange should be
      autodeleted.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">queueDurable</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      Specifies if the declared queue is durable.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">queueAutoDelete</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      Specifies if the declared queue should be
      autodeleted.
    </p>
    </td>
  </tr>
  <tr>
    <td rowspan="1" class="confluenceTd">queueExclusive</td><td style="text-align: center" class="confluenceTd">boolean</td><td style="text-align: center" class="confluenceTd">no</td><td style="text-align: center" class="confluenceTd"></td><td class="confluenceTd">
      <p>
      Specifies if the declared queue is exclusive.
    </p>
    </td>
  </tr>
</table>

Examples
--------

There are many ways to use the AMQP connector and endpoints. The following examples will demonstrate the common use cases.

### Connection fallback

It is possible to define a list of host:port or host (implying default port) to try to connect to in case the main one fails to connect.

    <amqp:connector name="amqpConnectorWithFallback"
                    host="rabbit1"
                    port="9876"
                    fallbackAddresses="rabbit1:9875,rabbit2:5672,rabbit3"
                    virtualHost="mule-test"
                    username="my-user"
                    password="my-pwd" />

### Listen to messages with exchange re-declaration and queue creation

This is a typical AMQP pattern where consumers redeclare the exchanges they intend to bind queues to.

    <amqp:connector name="amqpAutoAckLocalhostConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    activeDeclarationsOnly="true" />

    <amqp:inbound-endpoint exchangeName="my-exchange"
                           exchangeType="fanout"
                           exchangeAutoDelete="false"
                           exchangeDurable="true"
                           queueName="my-queue"
                           queueDurable="false"
                           queueExclusive="false"
                           queueAutoDelete="true"
                           connector-ref="amqpAutoAckLocalhostConnector" />

### Listen to messages with exchange re-declaration and private queue creation

In this variation of the previous example, Mule will create an exclusive server-named, auto-delete, non-durable queue and bind it to the re-declared exchange.

    <amqp:connector name="amqpAutoAckLocalhostConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    activeDeclarationsOnly="true" />

    <amqp:inbound-endpoint exchangeName="my-exchange"
                           exchangeType="fanout"
                           exchangeAutoDelete="false"
                           exchangeDurable="true"
                           connector-ref="amqpAutoAckLocalhostConnector" />

### Listen to messages on a pre-existing exchange

In this mode, the inbound connection will fail if the exchange doesn't pre-exist.

This behavior is enforced by activeDeclarationsOnly=false, which means that Mule will strictly ensure the pre-existence of the exchange before trying to subscribe to it. 

    <amqp:connector name="amqpAutoAckStrictLocalhostConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    activeDeclarationsOnly="false" />

    <amqp:inbound-endpoint exchangeName="my-exchange"
                           queueName="my-queue"
                           queueDurable="false"
                           queueExclusive="false"
                           queueAutoDelete="true"
                           queueName="my-queue"
                           connector-ref="amqpAutoAckStrictLocalhostConnector" />

### Listen to messages on a pre-existing queue

Similarly to the previous example, the inbound connection will fail if the queue doesn't pre-exist.

    <amqp:connector name="amqpAutoAckStrictLocalhostConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    activeDeclarationsOnly="false" />

    <amqp:inbound-endpoint queueName="my-queue"
                           connector-ref="amqpAutoAckStrictLocalhostConnector" />

### Listen to messages on declared but unbound queue

In this case, the queue is declared but not bound to any custom exchange,
except the default exchange to which all declared queues are bound by default.

Configuring any of the `queueDurable`, `queueAutoDelete` or `queueExclusive` attributes is needed to trigger the queue creation.

    <amqp:inbound-endpoint queueName="my-queue"
                           queueDurable="true" />

### Manual message acknowledgement and rejection

So far, all incoming messages were automatically acknowledged by the AMQP client.

The following example shows how to manually acknowledge or reject messages within a flow, based on criteria of your choice.
 
    <amqp:connector name="amqpManualAckLocalhostConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    ackMode="MANUAL" />

    <flow name="amqpChoiceAckNackService">
      <amqp:inbound-endpoint queueName="my-queue"
                             connector-ref="amqpManualAckLocalhostConnector" />
      <choice>
        <when ...condition...>
          <amqp:acknowledge-message />
        </when>
        <otherwise>
          <amqp:reject-message requeue="true" />
        </otherwise>
      </choice>
    </flow>

### Manual channel recovery

To manually recover the channel that is associated with the current message, use:

    <amqp:reject-message />

If you want the messages to be re-queued use:

    <amqp:reject-message requeue="true" />


### Flow control

Expanding on the previous example, it is possible to throttle the delivery of messages by configuring the connector accordingly.

The following demonstrates a connector that fetches messages one by one and a flow that uses manual acknowledgment to throttle the message delivery.

    <amqp:connector name="amqpThrottledConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    prefetchCount="1"
                    ackMode="MANUAL" />

    <flow name="amqpManualAckService">
      <amqp:inbound-endpoint queueName="my-queue"
                             connector-ref="amqpThrottledConnector" />
      <!--
      components, routers... go here
      -->
      <amqp:acknowledge-message />
    </flow>

### Publish messages to a redeclared exchange

This is a typical AMQP pattern where producers redeclare the exchanges they intend to publish to.

    <amqp:connector name="amqpLocalhostConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    activeDeclarationsOnly="true" />

    <amqp:outbound-endpoint routingKey="my-key"
                            exchangeName="my-exchange"
                            exchangeType="fanout"
                            exchangeAutoDelete="false"
                            exchangeDurable="false"
                            connector-ref="amqpLocalhostConnector" />

### Publish messages to a pre-existing exchange

It is also possible to publish to a pre-existing exchange:

    <amqp:outbound-endpoint exchangeName="my-exchange"
                            connector-ref="amqpLocalhostConnector" />

It can be desirable to strictly enforce the existence of this exchange before publishing to it.

This is done by configuring the connector to perform passive declarations:

    <amqp:connector name="amqpStrictLocalhostConnector"
                    virtualHost="my-vhost"
                    username="my-user"
                    password="my-pwd"
                    activeDeclarationsOnly="false" />

    <amqp:outbound-endpoint routingKey="my-key"
                            exchangeName="my-exchange"
                            connector-ref="amqpStrictLocalhostConnector" />

### Declare and bind outbound queue

It's also possible to declare queue in outbound endpoints, as shown here:

    <amqp:outbound-endpoint exchangeName="amqpOutBoundQueue-exchange"
                            exchangeType="fanout"
                            queueName="amqpOutBoundQueue-queue"
                            queueDurable="true" />

Note that the queue will be declared and bound in a lazy fashion, ie only when the outbound endpoint is used.

### Message level override of exchange and routing key

It is possible to override some outbound endpoint attributes with **outbound-scoped** message properties:

- *routing-key* overrides the routingKey attribute,
- *exchange* overrides the exchangeName attribute.

### Mandatory and immediate deliveries and returned message handling

The connector supports the mandatory and immediate publication flags, as show hereafter:

  <amqp:connector name="mandatoryAmqpConnector"
                  virtualHost="mule-test"
                  username="mule"
                  password="elum"
                  mandatory="true"
                  immediate="true" />

If a message sent with this connector can't be delivered, the AMQP broker will return it asynchronously.

The AMQP transport offers the possibility to dispatch these returned messages to user defined endpoints for custom processing.

You can define the endpoint in charge of handling returned messages at connector level. Here is an example that targets a VM endpoint:

  <vm:endpoint name="globalReturnedMessageChannel" path="global.returnedMessages" />

  <amqp:connector name="mandatoryAmqpConnector"
                  virtualHost="mule-test"
                  username="mule"
                  password="elum"
                  mandatory="true"
                  default-return-endpoint-ref="globalReturnedMessageChannel" />

It is also possible to define the returned message endpoint at flow level:

    <vm:endpoint name="flowReturnedMessageChannel" path="flow.returnedMessages" />

    <flow name="amqpMandatoryDeliveryFailureFlowHandler">
      <!--
      inbound endpoint, components, routers ...
      -->

      <amqp:return-handler>
        <vm:outbound-endpoint ref="flowReturnedMessageChannel" />
      </amqp:return-handler>

      <amqp:outbound-endpoint routingKey="my-key"
                              exchangeName="my-exchange"
                              connector-ref="mandatoryAmqpConnector" />
    </flow>

If both are configured, the handler defined in the flow will supersede the one defined in the connector. 

If none is configured, Mule will log a warning with the full details of the returned message.

### Request-response publication

It is possible to perform synchronous (request-response) outbound operations:

    <amqp:outbound-endpoint routingKey="my-key"
                            exchange-pattern="request-response"
                            exchangeName="my-exchange"
                            connector-ref="amqpLocalhostConnector" />

In that case, Mule will:

- create a temporary auto-delete private reply queue,
- set-it as the reply-to property of the current message,
- publish the message to the specified exchange,
- wait for a response to be sent to the reply-queue (via the default exchange).

### Transaction support

AMQP local transactions are supported by using the standard Mule transaction configuration element.
For example the following declares an AMQP inbound endpoint that will start a new transaction for each new received message:

    <amqp:inbound-endpoint queueName="amqpTransactedBridge-queue"
                           connector-ref="amqpConnector">
        <amqp:transaction action="ALWAYS_BEGIN" />
    </amqp:inbound-endpoint>

The following declares a transacted AMQP bridge:

    <bridge name="amqpTransactedBridge" exchange-pattern="one-way" transacted="true">
        <amqp:inbound-endpoint queueName="amqpTransactedBridge-queue"
                               connector-ref="amqpConnector">
            <amqp:transaction action="ALWAYS_BEGIN" />
        </amqp:inbound-endpoint>
        <amqp:outbound-endpoint exchangeName="amqpOneWayBridgeTarget-exchange"
                                connector-ref="amqpConnector">
            <amqp:transaction action="ALWAYS_JOIN" />
        </amqp:outbound-endpoint>
    </bridge>

If an error occurs during the processing of the message after the inbound endpoint, the transaction will automatically be rolled back.
Otherwise the transaction will be committed after successful dispatch in the outbound endpoint.

By default, no channel recovery is performed upon rollback. To change that configure a `recoverStrategy` on the transaction element, like

    <amqp:transaction action="ALWAYS_BEGIN" recoverStrategy="REQUEUE" />

Valid values for the `recoverStrategy` option are: `NONE, `NO_REQUEUE` and `REQUEUE`.

Transactions in AMQP are not behaving like JMS transactions: it is strongly suggested that you read [this overview of transaction support in AMQP 0.91](http://www.rabbitmq.com/amqp-0-9-1-reference.html#class.tx) before using transactions.
It is important to understand that when a transaction gets started on a Mule-managed channel, via for example `<amqp:transaction action="ALWAYS_BEGIN" />`, this channel will remain transactional for its lifetime.

### Exchange and Queue Declaration Arguments

AMQP supports the notion of custom arguments during the declartion of exchanges and queues.
The connector supports these custom arguments:
they must be passed as endpoint properties with names prefixed with `amqp-exchange.` or `amqp-queue.`
respectively for exchange or queue arguments.

The following declares a global endpoint that uses the `alternate-exchange` argument during the exchange declaration
and the `x-dead-letter-exchange` argument during the queue declaration:

    <amqp:endpoint name="amqpEndpointWithArguments" exchangeName="target-exchange"
        exchangeType="fanout" exchangeDurable="true" exchangeAutoDelete="false"
        queueName="target-queue" queueDurable="true" queueAutoDelete="false"
        queueExclusive="true" routingKey="a.b.c">
        <properties>
            <spring:entry key="amqp-exchange.alternate-exchange"
                value="some-exchange" />
            <spring:entry key="amqp-queue.x-dead-letter-exchange"
                value="some-queue" />
        </properties>
    </amqp:endpoint>

### Programmatic message requesting

It is possible to programmatically get messages from an AMQP queue.

For this you need first to build a URI that identifies the AMQP queue that you want to consume from. Here is the syntax to use, with optional parameters in square brackets:

    amqp://[${exchangeName}/]amqp-queue.${queueName}[?connector=${connectorName}[&...other parameters...]]

For example, the following identifies a prexisting queue named "my-queue" and will consume it with a unique AMQP connector available in the Mule configuration:

    amqp://amqp-queue.my-queue

This example will create and bind a non-durable auto-delete non-exclusive queue named "new-queue" to a pre-existing exchange named "my-exchange" with the provided routing key on the specified connector:

    amqp://my-exchange/amqp-queue.new-queue?connector=amqpAutoAckLocalhostConnector&queueDurable=false&queueExclusive=false&queueAutoDelete=true

With such a URI defined, it is possible to retrieve a message from the queue using the Mule Client, as shown in the following code sample:

    MuleMessage message = new MuleClient(muleContext).request("amqp://amqp-queue.my-queue", 2500L);
 
The above will wait for 2.5 seconds for a message and will return null if none has shown up in the queue after this amount of time.

### SSL Connectivity

The transport can connect to the broker using SSLv3 or TLS.
This is done by using the `AMQPS` connector using the following XML namespace declaration:

    xmlns:amqps="http://www.mulesoft.org/schema/mule/amqps"
    http://www.mulesoft.org/schema/mule/amqps http://www.mulesoft.org/schema/mule/amqps/current/mule-amqps.xsd

Connect using SSLv3 (default) and uses a trust manager that accepts all certificates as valid:

    <amqps:connector name="amqpsDefaultSslConnector" />

Connect using TLS and uses a trust manager that accepts all certificates as valid:

    <amqps:connector name="amqpsTlsConnector" sslProtocol="TLS" />

Connect using SSLv3 (default) and uses a custom trust manager:

    <amqps:connector name="amqpsTrustManagerConnector"
        sslTrustManager-ref="myTrustManager" />

Connect using TLS and uses a custom trust manager:

    <amqps:connector name="amqpsTlsTrustManagerConnector"
        sslProtocol="TLS" sslTrustManager-ref="myTrustManager" />

Connect with key and trust stores:

    <amqps:connector name="amqpsTlsKeyStores">
        <amqps:ssl-key-store path="keycert.p12" type="PKCS12"
            algorithm="SunX509" keyPassword="MySecretPassword"
            storePassword="MySecretPassword" />
        <amqps:ssl-trust-store path="trustStore.jks" type="JKS"
            algorithm="SunX509" storePassword="rabbitstore" />
    </amqps:connector>
