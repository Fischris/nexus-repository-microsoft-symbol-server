package org.sonatype.nexus.plugins.microsoftsymbolserver.internal;

import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.sonatype.nexus.testsuite.testsupport.raw.RawClient;

import java.net.URI;

public class MicrosoftSymbolServerHostedClient extends RawClient {


    public MicrosoftSymbolServerHostedClient(CloseableHttpClient httpClient, HttpClientContext httpClientContext, URI repositoryBaseUri) {
        super(httpClient, httpClientContext, repositoryBaseUri);
    }
}

