package org.gitlab4j.api.http;

import net.ltgt.resteasy.client.okhttp3.OkHttpClientEngine;
import okhttp3.OkHttpClient;
import org.gitlab4j.api.utils.JacksonJson;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;

public class OkHttpResteasyClientFactory {
    public static ResteasyClient getClient(OkHttpClient client) {
        ResteasyProviderFactory instance = ResteasyProviderFactory.getInstance();
        instance.registerProvider(JacksonJson.class);
        RegisterBuiltin.register(instance);

        return new ResteasyClientBuilderImpl().providerFactory(instance)
            .httpEngine(new OkHttpClientEngine(client))
            .build();
    }
}
