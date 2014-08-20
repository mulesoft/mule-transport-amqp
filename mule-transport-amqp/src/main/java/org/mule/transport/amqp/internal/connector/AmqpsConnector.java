/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.amqp.internal.connector;

import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.mule.api.MuleContext;
import org.mule.api.lifecycle.CreateException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.security.tls.TlsConfiguration;
import org.mule.config.i18n.MessageFactory;

import com.rabbitmq.client.ConnectionFactory;
import com.thoughtworks.xstream.InitializationException;

/**
 * Connects to a particular virtual host on a particular AMQP broker.
 */
public class AmqpsConnector extends AmqpConnector
{
    public static final String AMQPS = "amqps";

    // SSL support
    private final TlsConfiguration tls;

    private TrustManager sslTrustManager;

    public AmqpsConnector(final MuleContext muleContext)
    {
        super(muleContext);

        tls = new TlsConfiguration(null);
        tls.setSslType(null);

        setPort(ConnectionFactory.DEFAULT_AMQP_OVER_SSL_PORT);
    }

    @Override
    public String getProtocol()
    {
        return AMQPS;
    }

    @Override
    public void doInitialise() throws InitialisationException
    {
        final boolean configuredWithStores = (tls.getTrustStore() != null) || (tls.getKeyStore() != null);

        if ((sslTrustManager != null) && configuredWithStores)
        {
            throw new InitialisationException(
                MessageFactory.createStaticMessage("Either configure a trust manager or key stores"), this);
        }

        super.doInitialise();

        try
        {
            if (configuredWithStores)
            {
                try
                {
                    if (tls.getKeyStore() != null)
                    {
                        tls.initialise(false, null);
                    }
                    else
                    {
                        tls.initialise(true, null);
                    }
                }
                catch (final CreateException e)
                {
                    throw new InitialisationException(e, this);
                }

                getConnectionFactory().useSslProtocol(tls.getSslContext());
            }
            else if (sslTrustManager == null)
            {
                getConnectionFactory().useSslProtocol(tls.getSslType());
            }
            else
            {
                getConnectionFactory().useSslProtocol(tls.getSslType(), sslTrustManager);
            }
        }
        catch (final GeneralSecurityException gse)
        {
            throw new InitializationException("Failed to configure SSL", gse);
        }
    }

    public TrustManager getSslTrustManager()
    {
        return sslTrustManager;
    }

    public void setSslTrustManager(final TrustManager sslTrustManager)
    {
        this.sslTrustManager = sslTrustManager;
    }

    // TLS config delegates

    public String getSslProtocol()
    {
        return tls.getSslType();
    }

    public void setSslProtocol(final String sslProtocol)
    {
        tls.setSslType(sslProtocol);
    }

    public void setKeyPassword(final String keyPassword)
    {
        tls.setKeyPassword(keyPassword);
    }

    public void setKeyStore(final String keyStore) throws IOException
    {
        tls.setKeyStore(keyStore);
    }

    public void setKeyStoreType(final String keystoreType)
    {
        tls.setKeyStoreType(keystoreType);
    }

    public void setKeyStorePassword(final String storePassword)
    {
        tls.setKeyStorePassword(storePassword);
    }

    public void setKeyManagerAlgorithm(final String keyManagerAlgorithm)
    {
        tls.setKeyManagerAlgorithm(keyManagerAlgorithm);
    }

    public void setTrustStore(final String trustStore) throws IOException
    {
        tls.setTrustStore(trustStore);
    }

    public void setTrustStorePassword(final String trustStorePassword)
    {
        tls.setTrustStorePassword(trustStorePassword);
    }

    public void setTrustStoreType(final String trustStoreType)
    {
        tls.setTrustStoreType(trustStoreType);
    }

    public void setTrustManagerAlgorithm(final String trustManagerAlgorithm)
    {
        tls.setTrustManagerAlgorithm(trustManagerAlgorithm);
    }

    public void setTrustManagerFactory(final TrustManagerFactory trustManagerFactory)
    {
        tls.setTrustManagerFactory(trustManagerFactory);
    }

    public void setExplicitTrustStoreOnly(final boolean explicitTrustStoreOnly)
    {
        tls.setExplicitTrustStoreOnly(explicitTrustStoreOnly);
    }
}
