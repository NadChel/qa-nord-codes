package org.example;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import io.qameta.allure.Description;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.example.api.ApiClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Epic("ACTION")
@Feature("ACTION")
@DisplayName("ACTION tests")
public class ActionTest extends BaseApiTest {

    private static final String VALID_TOKEN = "A".repeat(32);
    private static final MappingBuilder POST_AUTH = post("/auth");
    private static final MappingBuilder POST_ACTION = post("/doAction");

    private ApiClient client;

    @BeforeEach
    void setUp() throws ConfigurationException {
        client = ApiClient.withDefaults();
    }

    @AfterEach
    void tearDown() {
        client.logout(VALID_TOKEN);
    }

    @Test
    @Story("ACTION with no API key")
    @Description("Given no API key, LOGIN returns 401")
    void givenNoApiKey_actionFails() {
        stubServer(POST_AUTH, 200);
        client.setApiKey(null);

        client.login(VALID_TOKEN)
                .then()
                .statusCode(401)
                .body("result", equalTo("ERROR"))
                .body("message", not(blankOrNullString()));
    }

    @Test
    @Story("ACTION with no prior LOGIN")
    @Description("Given no prior LOGIN, ACTION returns 403")
    void givenNoPriorLogin_actionReturns403() {
        stubServer(POST_AUTH, 200);

        client.doAction(VALID_TOKEN)
                .then()
                .statusCode(403)
                .body("result", equalTo("ERROR"))
                .body("message", not(blankOrNullString()));
    }

    @ParameterizedTest(name = "Case: {0}")
    @Story("ACTION with a prior successful LOGIN but error from the external server")
    @ValueSource(ints = {500, 403})
    @Description("Given successful LOGIN, but external server error, ACTION fails, preserving error code")
    void givenPriorLogin_butExternalError_actionFails_preservingErrorCode(int errorStatusCode) {
        stubServer(POST_AUTH, 200);
        stubServer(POST_ACTION, errorStatusCode);
        client.assumingLoginSuccess(VALID_TOKEN)
                .doAction(VALID_TOKEN)
                .then()
                .statusCode(errorStatusCode)
                .body("result", equalTo("ERROR"))
                .body("message", not(blankOrNullString()));
    }

    @Test
    @Story("ACTION with a prior successful LOGIN and a successful response from the external server")
    @Description("Given successful LOGIN and external server returning 200, ACTION succeeds")
    void givenSomething_thenSomethingElse() {
        stubServer(POST_AUTH, 200);
        stubServer(POST_ACTION, 200);
        client.assumingLoginSuccess(VALID_TOKEN);

        client.doAction(VALID_TOKEN)
                .then()
                .statusCode(200)
                .body("result", equalTo("OK"));
    }
}
