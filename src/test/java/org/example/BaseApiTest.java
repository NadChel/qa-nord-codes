package org.example;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.MappingBuilder;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.net.MalformedURLException;
import java.net.URL;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;

public abstract class BaseApiTest {

    private static WireMockServer mockServer;

    @BeforeAll
    static void beforeAll() throws ConfigurationException, MalformedURLException {
        Configuration config = new Configurations().properties("application.properties");
        URL url = new URL(config.getString("mock.url"));
        mockServer = new WireMockServer(url.getPort());
        mockServer.start();
    }

    @AfterAll
    static void afterAll() {
        mockServer.stop();
    }

    @BeforeEach
    void resetServer() {
        resetServerStubs();
    }

    private static void resetServerStubs() {
        mockServer.resetAll();
    }

    protected static void stubServer(MappingBuilder request, int statusCode) {
        mockServer.stubFor(request.willReturn(
                aResponse().withStatus(statusCode)));
    }
}
