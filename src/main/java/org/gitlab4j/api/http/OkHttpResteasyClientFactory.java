package org.gitlab4j.api.http;

import jakarta.ws.rs.client.ClientBuilder;
import okhttp3.OkHttpClient;
import org.gitlab4j.api.utils.JacksonJson;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.internal.LocalResteasyProviderFactory;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class OkHttpResteasyClientFactory {
    public static ResteasyClient getClient(OkHttpClient client) {
        // https://docs.jboss.org/resteasy/docs/5.0.1.Final/userguide/html/RESTEasy_Client_Framework.html#jetty_client
        LocalResteasyProviderFactory factory = new LocalResteasyProviderFactory();
        if (!factory.isEnabled(JacksonFeature.class)) {
            factory.addFeature(JacksonFeature.class);
        }

        if (!factory.isEnabled(MultiPartFeature.class)) {
            factory.addFeature(MultiPartFeature.class);
        }

        ResteasyProviderFactory.setInstance(factory);
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();

        if (!instance.isRegistered(JacksonJson.class)) {
            instance.registerProvider(JacksonJson.class);
        }

        RegisterBuiltin.register(instance);

        return new ResteasyClientBuilderImpl().providerFactory(instance)
            .httpEngine(new OkHttpClientEngine(client))
            .build();
    }
}
