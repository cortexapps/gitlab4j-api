package org.gitlab4j.api.http;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.core.MultivaluedMap;
import okhttp3.Headers;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okio.Buffer;
import okio.BufferedSink;
import org.jboss.resteasy.client.jaxrs.ClientHttpEngine;
import org.jboss.resteasy.client.jaxrs.internal.ClientInvocation;
import org.jboss.resteasy.client.jaxrs.internal.ClientResponse;
import org.jboss.resteasy.util.CaseInsensitiveMap;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

public class OkHttpClientEngine implements ClientHttpEngine {

    private final OkHttpClient client;

    private SSLContext sslContext;

    public OkHttpClientEngine(OkHttpClient client) {
        this.client = client;
    }

    @Override
    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public HostnameVerifier getHostnameVerifier() {
        return client.hostnameVerifier();
    }

    @Override
    public ClientResponse invoke(Invocation request) {
        Request req = createRequest((ClientInvocation) request);
        Response response;
        try {
            response = client.newCall(req).execute();
        } catch (IOException e) {
            throw new ProcessingException("Unable to invoke request", e);
        }
        return createResponse((ClientInvocation) request, response);
    }

    private Request createRequest(ClientInvocation request) {
        Request.Builder builder =
            new Request.Builder()
                .method(request.getMethod(), createRequestBody(request))
                .url(request.getUri().toString());
        for (Map.Entry<String, List<String>> header : request.getHeaders().asMap().entrySet()) {
            String headerName = header.getKey();
            for (String headerValue : header.getValue()) {
                builder.addHeader(headerName, headerValue);
            }
        }
        return builder.build();
    }

    private RequestBody createRequestBody(final ClientInvocation request) {
        if (request.getEntity() == null) {
            return null;
        }

        // NOTE: this will invoke WriterInterceptors which can possibly change the request,
        // so it must be done first, before reading any header.
        final Buffer buffer = new Buffer();
        try {
            request.writeRequestBody(buffer.outputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        jakarta.ws.rs.core.MediaType mediaType = request.getHeaders().getMediaType();
        final MediaType contentType =
            (mediaType == null) ? null : MediaType.parse(mediaType.toString());

        return new RequestBody() {
            @Override
            public long contentLength() throws IOException {
                return buffer.size();
            }

            @Override
            public MediaType contentType() {
                return contentType;
            }

            @Override
            public void writeTo(BufferedSink sink) throws IOException {
                buffer.copyTo(sink.buffer(), 0, buffer.size());
            }
        };
    }

    private ClientResponse createResponse(ClientInvocation request, final Response response) {
        ClientResponse clientResponse =
            new ClientResponse(request.getClientConfiguration()) {
                private InputStream stream;

                @Override
                protected InputStream getInputStream() {
                    if (stream == null) {
                        stream = response.body().byteStream();
                    }
                    return stream;
                }

                @Override
                protected void setInputStream(InputStream is) {
                    stream = is;
                }

                @Override
                public void releaseConnection() throws IOException {
                    // Stream might have been entirely replaced, so we need to close it independently from response.body()
                    Throwable primaryExc = null;
                    try {
                        if (stream != null) {
                            stream.close();
                        }
                    } catch (Throwable t) {
                        primaryExc = t;
                        throw t;
                    } finally {
                        if (primaryExc != null) {
                            try {
                                response.body().close();
                            } catch (Throwable suppressedExc) {
                                primaryExc.addSuppressed(suppressedExc);
                            }
                        } else {
                            response.body().close();
                        }
                    }
                }
            };

        clientResponse.setStatus(response.code());
        clientResponse.setHeaders(transformHeaders(response.headers()));

        return clientResponse;
    }

    private MultivaluedMap<String, String> transformHeaders(Headers headers) {
        MultivaluedMap<String, String> ret = new CaseInsensitiveMap<>();
        for (int i = 0, l = headers.size(); i < l; i++) {
            ret.add(headers.name(i), headers.value(i));
        }
        return ret;
    }

    @Override
    public void close() {
        // no-op
    }
}
