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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@Epic("Authentication")
@DisplayName("Authentication tests")
public class AuthenticationTest extends BaseApiTest {

    private static final String VALID_TOKENS_CSV = "/data/valid_tokens.csv";
    private static final String INVALID_TOKENS_CSV = "/data/invalid_tokens.csv";
    private static final String VALID_TOKEN = "A".repeat(32);
    private static final MappingBuilder POST_AUTH = post("/auth");

    private ApiClient client;
    private String currentToken;

    @BeforeEach
    void setUp() throws ConfigurationException {
        client = ApiClient.withDefaults();
    }

    @AfterEach
    void tearDown() {
        if (currentToken == null) return;
        client.logout(currentToken);
        currentToken = null;
    }

    @Nested
    @Feature("LOGIN")
    @DisplayName("LOGIN tests")
    class LoginTest {

        @Test
        @Story("LOGIN with no API key")
        @Description("Given no API key, LOGIN returns 401")
        void givenNoApiKey_loginFails() {
            stubServer(POST_AUTH, 200);
            client.setApiKey(null);

            currentToken = VALID_TOKEN;
            client.login(currentToken)
                    .then()
                    .statusCode(401)
                    .body("result", equalTo("ERROR"))
                    .body("message", not(blankOrNullString()));
        }

        @ParameterizedTest(name = "Case: {1}")
        @Story("LOGIN with invalid token")
        @CsvFileSource(resources = INVALID_TOKENS_CSV)
        @Description("Given invalid token, LOGIN returns 400")
        void givenInvalidToken_loginFails(String invalidToken, String description) {
            stubServer(POST_AUTH, 200);

            currentToken = invalidToken;
            client.login(currentToken)
                    .then()
                    .statusCode(400)
                    .body("result", equalTo("ERROR"))
                    .body("message", not(blankOrNullString()));
        }

        @ParameterizedTest(name = "Case: {0}")
        @Story("LOGIN with valid token but authentication server error")
        @ValueSource(ints = {500, 403})
        @Description("Given valid token, but error code from external API, LOGIN fails, preserving the error")
        void givenValidToken_butExternalServerError_loginFails_preservingErrorCode(int errorCode) {
            stubServer(POST_AUTH, errorCode);

            currentToken = VALID_TOKEN;
            client.login(currentToken)
                    .then()
                    .statusCode(errorCode)
                    .body("result", equalTo("ERROR"))
                    .body("message", not(blankOrNullString()));
        }

        @Test
        @Story("Repeatable LOGIN with the same token")
        @Description("Given repeatable LOGIN with the same token, LOGIN returns 409")
        void givenValidToken_sentTwice_secondLoginReturns409() {
            stubServer(POST_AUTH, 200);

            currentToken = VALID_TOKEN;
            client.assumingLoginSuccess(currentToken)
                    .login(currentToken)
                    .then()
                    .statusCode(409)
                    .body("result", equalTo("ERROR"))
                    .body("message", not(blankOrNullString()));
        }

        @ParameterizedTest(name = "Case: {1}")
        @Story("LOGIN with a valid token and a successful response from the authentication server")
        @CsvFileSource(resources = VALID_TOKENS_CSV)
        @Description("Given valid token and 200 from external API, LOGIN succeeds")
        void givenValidToken_and200FromExternalServer_loginSucceeds(String token, String description) {
            stubServer(POST_AUTH, 200);

            currentToken = token;
            client.login(currentToken)
                    .then()
                    .statusCode(200)
                    .body("result", equalTo("OK"));
        }
    }

    @Nested
    @Feature("LOGOUT")
    @DisplayName("LOGOUT tests")
    class LogoutTest {

        @Test
        @Story("LOGOUT with no prior successful LOGIN")
        @Description("Given no prior LOGIN with a given token, LOGOUT returns 409")
        void givenNoPriorLoginWithGivenToken_logoutFails() {
            stubServer(POST_AUTH, 200);

            client.logout(VALID_TOKEN)
                    .then()
                    .statusCode(403)
                    .body("result", equalTo("ERROR"))
                    .body("message", not(blankOrNullString()));
        }

        @Test
        @Story("LOGOUT with a prior successful LOGIN")
        @Description("Given prior LOGIN with given token, LOGOUT succeeds, LOGIN with the same token is possible again")
        void givenValidLogin_logoutSucceeds() {
            stubServer(POST_AUTH, 200);

            currentToken = VALID_TOKEN;
            client.assumingLoginSuccess(currentToken)
                    .logout(currentToken)
                    .then()
                    .statusCode(200)
                    .body("result", equalTo("OK"));

            client.login(currentToken)
                    .then()
                    .statusCode(200)
                    .body("result", equalTo("OK"));
        }
    }
}