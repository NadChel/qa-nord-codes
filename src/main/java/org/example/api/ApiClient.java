package org.example.api;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.apache.commons.configuration2.ex.ConfigurationException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assumptions.assumeThat;

public class ApiClient {

    private static final String API_KEY_HEADER = "X-Api-Key";
    private static final String FORM_PARAM_TOKEN = "token";
    private static final String FORM_PARAM_ACTION = "action";
    private final String url;
    private String apiKey;

    private ApiClient(Configuration config) {
        apiKey = config.getString("api.key");
        url = config.getString("base.url") + "/endpoint";
    }

    public static ApiClient withDefaults() throws ConfigurationException {
        PropertiesConfiguration config = new Configurations().properties("application.properties");
        return new ApiClient(config);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public Response login(String token) {
        return action(Action.LOGIN, token).post();
    }

    private RequestSpecification action(Action action, String token) {
        RequestSpecification spec = given()
                .baseUri(url)
                .contentType(ContentType.URLENC)
                .formParam(FORM_PARAM_TOKEN, token)
                .formParam(FORM_PARAM_ACTION, action);
        if (apiKey != null) spec.header(API_KEY_HEADER, apiKey);
        return spec;
    }

    public ApiClient assumingLoginSuccess(String token) {
        Response response = login(token);
        assumeThat(response.getStatusCode()).as("Login check.").isEqualTo(200);
        return this;
    }

    public Response logout(String token) {
        return action(Action.LOGOUT, token).post();
    }

    public Response doAction(String token) {
        return action(Action.ACTION, token).post();
    }

    private enum Action {

        LOGIN, ACTION, LOGOUT
    }
}
