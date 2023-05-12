package org.gitlab4j.api.http;

import jakarta.ws.rs.client.ClientBuilder;
import okhttp3.OkHttpClient;
import org.gitlab4j.api.utils.JacksonJson;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class OkHttpResteasyClientFactory {
    public static ResteasyClient getClient(OkHttpClient client) {
        // https://docs.jboss.org/resteasy/docs/5.0.1.Final/userguide/html/RESTEasy_Client_Framework.html#jetty_client
        return ((ResteasyClientBuilder) ClientBuilder
            .newBuilder()
            .property(ClientProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true)
            .property(ClientProperties.METAINF_SERVICES_LOOKUP_DISABLE, true)
            .register(JacksonJson.class)
            .register(JacksonFeature.class)
            .register(MultiPartFeature.class))
            .httpEngine(new OkHttpClientEngine(client))
            .build();
    }
}
