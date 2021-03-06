package org.carlspring.strongbox.controllers.configuration;

import io.restassured.module.mockmvc.response.ValidatableMockMvcResponse;
import org.apache.commons.collections.MapUtils;
import org.apache.http.pool.PoolStats;
import org.carlspring.strongbox.config.IntegrationTest;
import org.carlspring.strongbox.configuration.Configuration;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.rest.common.RestAssuredBaseTest;
import org.carlspring.strongbox.storage.repository.ImmutableRepository;
import org.carlspring.strongbox.storage.repository.Repository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import javax.inject.Inject;
import java.util.Optional;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;

/**
 * @author Pablo Tirado
 */
@IntegrationTest
public class HttpConnectionPoolConfigurationManagementControllerTestIT
        extends RestAssuredBaseTest
{

    @Inject
    private ConfigurationManager configurationManager;


    @Override
    @BeforeEach
    public void init()
            throws Exception
    {
        super.init();
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE })
    void testSetAndGetMaxNumberOfConnectionsForProxyRepository(String acceptHeader)
    {

        int newMaxNumberOfConnections = 200;

        String url = getContextBaseUrl() + "/api/configuration/proxy/connection-pool/max/" + newMaxNumberOfConnections;

        ValidatableMockMvcResponse response = given().accept(acceptHeader)
                                                     .when()
                                                     .put(url)
                                                     .peek()
                                                     .then()
                                                     .statusCode(HttpStatus.OK.value());

        String message = "Max number of connections for proxy repository was updated successfully.";
        validateResponseBody(response, acceptHeader, message);

        url = getContextBaseUrl() + "/api/configuration/proxy/connection-pool";
        response = given().accept(acceptHeader)
                          .when()
                          .get(url)
                          .peek()
                          .then()
                          .statusCode(HttpStatus.OK.value());

        validateResponseBodyConnections(response, acceptHeader, newMaxNumberOfConnections);
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE })
    void testSetAndGetDefaultNumberOfConnectionsForProxyRepository(String acceptHeader)
    {
        int newDefaultNumberOfConnections = 5;

        String url =
                getContextBaseUrl() + "/api/configuration/proxy/connection-pool/default/" + newDefaultNumberOfConnections;

        ValidatableMockMvcResponse response = given().accept(acceptHeader)
                                                     .when()
                                                     .put(url)
                                                     .peek()
                                                     .then()
                                                     .statusCode(HttpStatus.OK.value());

        String message = "Default number of connections for proxy repository was updated successfully.";
        validateResponseBody(response, acceptHeader, message);

        url = getContextBaseUrl() + "/api/configuration/proxy/connection-pool/default-number";

        response = given().accept(acceptHeader)
                          .when()
                          .get(url)
                          .peek()
                          .then()
                          .statusCode(HttpStatus.OK.value());

        validateResponseBodyConnections(response, acceptHeader, newDefaultNumberOfConnections);
    }

    @ParameterizedTest
    @ValueSource(strings = { MediaType.APPLICATION_JSON_VALUE,
                             MediaType.TEXT_PLAIN_VALUE })
    void testSetAndGetNumberOfConnectionsForProxyRepositoryWithTextAcceptHeader(String acceptHeader)
    {
        Configuration configuration = configurationManager.getConfiguration();
        Optional<Repository> repositoryOpt = configuration.getStorages()
                                                          .values()
                                                          .stream()
                                                          .filter(stg -> MapUtils.isNotEmpty(stg.getRepositories()))
                                                          .flatMap(stg -> stg.getRepositories().values().stream())
                                                          .map(r -> (ImmutableRepository)r)
                                                          .filter(repository ->
                                                                          repository.getRemoteRepository() != null &&
                                                                          repository.getRemoteRepository().getUrl() !=
                                                                          null)
                                                          .map(r -> (Repository) r)
                                                          .findAny();

        Repository repository = repositoryOpt.get();
        int numberOfConnections = 5;

        String url = getContextBaseUrl() + "/api/configuration/proxy/connection-pool/" +
                     repository.getStorage().getId() + "/" +
                     repository.getId() + "/" +
                     numberOfConnections;

        ValidatableMockMvcResponse response = given().accept(acceptHeader)
                                                     .when()
                                                     .put(url)
                                                     .peek()
                                                     .then()
                                                     .statusCode(HttpStatus.OK.value());

        String message = "Number of pool connections for repository was updated successfully.";
        validateResponseBody(response, acceptHeader, message);

        url = getContextBaseUrl() + "/api/configuration/proxy/connection-pool/" +
              repository.getStorage().getId() + "/" +
              repository.getId();

        PoolStats expectedPoolStats = new PoolStats(0, 0, 0, numberOfConnections);
        response = given().accept(acceptHeader)
                          .when()
                          .get(url)
                          .peek()
                          .then()
                          .statusCode(HttpStatus.OK.value());

        validateResponseBodyPoolStats(acceptHeader, response, expectedPoolStats);
    }

    private void validateResponseBodyConnections(ValidatableMockMvcResponse response,
                                                 String acceptHeader,
                                                 int newMaxNumberOfConnections)
    {
        if (acceptHeader.equals(MediaType.APPLICATION_JSON_VALUE))
        {
            response.body("numberOfConnections", equalTo(newMaxNumberOfConnections));
        }
        else if (acceptHeader.equals(MediaType.TEXT_PLAIN_VALUE))
        {
            response.body(equalTo(String.valueOf(newMaxNumberOfConnections)));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported content type: " + acceptHeader);
        }
    }

    private void validateResponseBody(ValidatableMockMvcResponse response,
                                      String acceptHeader,
                                      String message)
    {
        if (acceptHeader.equals(MediaType.APPLICATION_JSON_VALUE))
        {
            response.body("message", equalTo(message));
        }
        else if (acceptHeader.equals(MediaType.TEXT_PLAIN_VALUE))
        {
            response.body(equalTo(message));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported content type: " + acceptHeader);
        }
    }

    private void validateResponseBodyPoolStats(String acceptHeader,
                                               ValidatableMockMvcResponse response,
                                               PoolStats expectedPoolStats)
    {
        if (acceptHeader.equals(MediaType.APPLICATION_JSON_VALUE))
        {
            response.body("max", equalTo(expectedPoolStats.getMax()));
        }
        else if (acceptHeader.equals(MediaType.TEXT_PLAIN_VALUE))
        {
            response.body(containsString("max: " + expectedPoolStats.getMax()));
        }
        else
        {
            throw new IllegalArgumentException("Unsupported content type: " + acceptHeader);
        }
    }
}
