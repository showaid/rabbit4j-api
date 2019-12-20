package org.rabbit4j.api;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response;

import org.rabbit4j.api.Constants.TokenType;
import org.rabbit4j.api.models.Version;
import org.rabbit4j.api.utils.MaskingLoggingFilter;

/**
 * This class is provides a simplified interface to a 3rabbitz book server, and divides the API up into
 * a separate API class for each concern.
 */
public class RabbitApi {

    private final static Logger LOGGER = Logger.getLogger(RabbitApi.class.getName());

    /** Rabbit4J default per page.  Rabbit will ignore anything over 100. */
    public static final int DEFAULT_PER_PAGE = 20;

    /** Specifies the version of the Rabbit API to communicate with. */
    public enum ApiVersion {
        r;

        public String getApiNamespace() {
            return ("/!#/" + name().toLowerCase());
        }
    }

    // Used to keep track of RabbitApiExceptions on calls that return Optional<?>
    private static final Map<Integer, RabbitApiException> optionalExceptionMap =
            Collections.synchronizedMap(new WeakHashMap<Integer, RabbitApiException>());

    RabbitApiClient apiClient;
    private ApiVersion apiVersion;
    private String rabbitServerUrl;
    private int defaultPerPage = DEFAULT_PER_PAGE;
    
    private UserApi userApi;

    /**
     * Get the Rabbitz4J shared Logger instance.
     *
     * @return the Rabbitz4J shared Logger instance
     */
    public static final Logger getLogger() {
        return (LOGGER);
    }

    /**
     * Constructs a RabbitApi instance set up to interact with the Rabbit server using Rabbit API version 4.
     *
     * @param hostUrl the URL of the Rabbit server
     * @param privateToken to private token to use for access to the API
     * @param secretToken use this token to validate received payloads
     */
    public RabbitApi(String hostUrl, String username, String password) {
        this(ApiVersion.r, hostUrl, TokenType.PRIVATE, username, password);
    }
    
    /**
     * Constructs a RabbitApi instance set up to interact with the Rabbit server using the specified Rabbit API version.
     *
     * @param apiVersion the ApiVersion specifying which version of the API to use
     * @param hostUrl the URL of the Rabbit server
     * @param tokenType the type of auth the token is for, PRIVATE or ACCESS
     * @param authToken the token to use for access to the API
     * @param secretToken use this token to validate received payloads
     */
    public RabbitApi(ApiVersion apiVersion, String hostUrl, TokenType tokenType, String authToken, String secretToken) {
        this(apiVersion, hostUrl, tokenType, authToken, secretToken, null);
    }

    /**
     *  Constructs a RabbitApi instance set up to interact with the Rabbit server specified by Rabbit API version.
     *
     * @param apiVersion the ApiVersion specifying which version of the API to use
     * @param hostUrl the URL of the Rabbit server
     * @param tokenType the type of auth the token is for, PRIVATE or ACCESS
     * @param authToken to token to use for access to the API
     * @param secretToken use this token to validate received payloads
     * @param clientConfigProperties Map instance with additional properties for the Jersey client connection
     */
    public RabbitApi(ApiVersion apiVersion, String hostUrl, TokenType tokenType, String authToken, String secretToken, Map<String, Object> clientConfigProperties) {
        this.apiVersion = apiVersion;
        this.rabbitServerUrl = hostUrl;
        apiClient = new RabbitApiClient(apiVersion, hostUrl, tokenType, authToken, secretToken, clientConfigProperties);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API
     * using the Rabbit4J shared Logger instance and Level.FINE as the level.
     *
     * @return this RabbitApi instance
     */
    public RabbitApi withRequestResponseLogging() {
        enableRequestResponseLogging();
        return (this);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API
     * using the Rabbit4J shared Logger instance.
     *
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @return this RabbitApi instance
     */
    public RabbitApi withRequestResponseLogging(Level level) {
        enableRequestResponseLogging(level);
        return (this);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API.
     *
     * @param logger the Logger instance to log to
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @return this RabbitApi instance
     */
    public RabbitApi withRequestResponseLogging(Logger logger, Level level) {
        enableRequestResponseLogging(logger, level);
        return (this);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API
     * using the Rabbit4J shared Logger instance and Level.FINE as the level.
     */
    public void enableRequestResponseLogging() {
        enableRequestResponseLogging(LOGGER, Level.FINE);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API
     * using the Rabbit4J shared Logger instance. Logging will NOT include entity logging and
     * will mask PRIVATE-TOKEN and Authorization headers.
     *
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     */
    public void enableRequestResponseLogging(Level level) {
        enableRequestResponseLogging(LOGGER, level, 0);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API using the
     * specified logger. Logging will NOT include entity logging and will mask PRIVATE-TOKEN 
     * and Authorization headers..
     *
     * @param logger the Logger instance to log to
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     */
    public void enableRequestResponseLogging(Logger logger, Level level) {
        enableRequestResponseLogging(logger, level, 0);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API using the
     * Rabbit4J shared Logger instance. Logging will mask PRIVATE-TOKEN and Authorization headers.
     *
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @param maxEntitySize maximum number of entity bytes to be logged.  When logging if the maxEntitySize
     * is reached, the entity logging  will be truncated at maxEntitySize and "...more..." will be added at
     * the end of the log entry. If maxEntitySize is &lt;= 0, entity logging will be disabled
     */
    public void enableRequestResponseLogging(Level level, int maxEntitySize) {
        enableRequestResponseLogging(LOGGER, level, maxEntitySize);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API using the
     * specified logger. Logging will mask PRIVATE-TOKEN and Authorization headers.
     *
     * @param logger the Logger instance to log to
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @param maxEntitySize maximum number of entity bytes to be logged.  When logging if the maxEntitySize
     * is reached, the entity logging  will be truncated at maxEntitySize and "...more..." will be added at
     * the end of the log entry. If maxEntitySize is &lt;= 0, entity logging will be disabled
     */
    public void enableRequestResponseLogging(Logger logger, Level level, int maxEntitySize) {
        enableRequestResponseLogging(logger, level, maxEntitySize, MaskingLoggingFilter.DEFAULT_MASKED_HEADER_NAMES);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API using the
     * Rabbit4J shared Logger instance.
     *
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @param maskedHeaderNames a list of header names that should have the values masked
     */
    public void enableRequestResponseLogging(Level level, List<String> maskedHeaderNames) {
        apiClient.enableRequestResponseLogging(LOGGER, level, 0, maskedHeaderNames);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API using the
     * specified logger.
     *
     * @param logger the Logger instance to log to
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @param maskedHeaderNames a list of header names that should have the values masked
     */
    public void enableRequestResponseLogging(Logger logger, Level level, List<String> maskedHeaderNames) {
        apiClient.enableRequestResponseLogging(logger, level, 0, maskedHeaderNames);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API using the
     * Rabbit4J shared Logger instance.
     *
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @param maxEntitySize maximum number of entity bytes to be logged.  When logging if the maxEntitySize
     * is reached, the entity logging  will be truncated at maxEntitySize and "...more..." will be added at
     * the end of the log entry. If maxEntitySize is &lt;= 0, entity logging will be disabled
     * @param maskedHeaderNames a list of header names that should have the values masked
     */
    public void enableRequestResponseLogging(Level level, int maxEntitySize, List<String> maskedHeaderNames) {
        apiClient.enableRequestResponseLogging(LOGGER, level, maxEntitySize, maskedHeaderNames);
    }

    /**
     * Enable the logging of the requests to and the responses from the Rabbit server API using the
     * specified logger.
     *
     * @param logger the Logger instance to log to
     * @param level the logging level (SEVERE, WARNING, INFO, CONFIG, FINE, FINER, FINEST)
     * @param maxEntitySize maximum number of entity bytes to be logged.  When logging if the maxEntitySize
     * is reached, the entity logging  will be truncated at maxEntitySize and "...more..." will be added at
     * the end of the log entry. If maxEntitySize is &lt;= 0, entity logging will be disabled
     * @param maskedHeaderNames a list of header names that should have the values masked
     */
    public void enableRequestResponseLogging(Logger logger, Level level, int maxEntitySize, List<String> maskedHeaderNames) {
        apiClient.enableRequestResponseLogging(logger, level, maxEntitySize, maskedHeaderNames);
    }

    /**
     * Get the auth token being used by this client.
     *
     * @return the auth token being used by this client
     */
    public String getAuthToken() {
        return (apiClient.getAuthToken());
    }

    /**
     * Get the secret token.
     *
     * @return the secret token
     */
    public String getSecretToken() {
        return (apiClient.getSecretToken());
    }

    /**
     * Get the TokenType this client is using.
     *
     * @return the TokenType this client is using
     */
    public TokenType getTokenType() {
        return (apiClient.getTokenType());
    }

    /**
     * Return the Rabbit API version that this instance is using.
     *
     * @return the Rabbit API version that this instance is using
     */
    public ApiVersion getApiVersion() {
        return (apiVersion);
    }

    /**
     * Get the URL to the Rabbit server.
     *
     * @return the URL to the Rabbit server
     */
    public String getRabbitServerUrl() {
        return (rabbitServerUrl);
    }

    /**
     * Get the default number per page for calls that return multiple items.
     *
     * @return the default number per page for calls that return multiple item
     */
    public int getDefaultPerPage() {
        return (defaultPerPage);
    }

    /**
     * Set the default number per page for calls that return multiple items.
     *
     * @param defaultPerPage the new default number per page for calls that return multiple item
     */
    public void setDefaultPerPage(int defaultPerPage) {
        this.defaultPerPage = defaultPerPage;
    }

    /**
     * Return the RabbitApiClient associated with this instance. This is used by all the sub API classes
     * to communicate with the Rabbit API.
     *
     * @return the RabbitApiClient associated with this instance
     */
    RabbitApiClient getApiClient() {
        return (apiClient);
    }

    /**
     * Returns true if the API is setup to ignore SSL certificate errors, otherwise returns false.
     *
     * @return true if the API is setup to ignore SSL certificate errors, otherwise returns false
     */
    public boolean getIgnoreCertificateErrors() {
        return (apiClient.getIgnoreCertificateErrors());
    }

    /**
     * Sets up the Jersey system ignore SSL certificate errors or not.
     *
     * @param ignoreCertificateErrors if true will set up the Jersey system ignore SSL certificate errors
     */
    public void setIgnoreCertificateErrors(boolean ignoreCertificateErrors) {
        apiClient.setIgnoreCertificateErrors(ignoreCertificateErrors);
    }

    /**
     * Get the version info for the Rabbit server using the Rabbit Version API.
     *
     * @return the version info for the Rabbit server
     * @throws RabbitApiException if any exception occurs
     */
    public Version getVersion() throws RabbitApiException {

        class VersionApi extends AbstractApi {
            VersionApi(RabbitApi rabbitApi) {
                super(rabbitApi);
            }
        }

        Response response = new VersionApi(this).get(Response.Status.OK, null, "version");
        return (response.readEntity(Version.class));
    }

    /**
     * Gets the UserApi instance owned by this RabbitApi instance. The UserApi is used
     * to perform all user related API calls.
     *
     * @return the UserApi instance owned by this RabbitApi instance
     */
    public UserApi getUserApi() {

        if (userApi == null) {
            synchronized (this) {
                if (userApi == null) {
                    userApi = new UserApi(this);
                }
            }
        }

        return (userApi);
    }

    /**
     * Create and return an Optional instance associated with a RabbitApiException.
     *
     * @param <T> the type of the Optional instance
     * @param glae the RabbitApiException that was the result of a call to the Rabbit API
     * @return the created Optional instance
     */
    protected static final <T> Optional<T> createOptionalFromException(RabbitApiException glae) {
        Optional<T> optional = Optional.empty();
        optionalExceptionMap.put(System.identityHashCode(optional),  glae);
        return (optional);
    }

    /**
     * Get the exception associated with the provided Optional instance, or null if no exception is
     * associated with the Optional instance.
     *
     * @param optional the Optional instance to get the exception for
     * @return the exception associated with the provided Optional instance, or null if no exception is
     * associated with the Optional instance
     */
    public static final RabbitApiException getOptionalException(Optional<?> optional) {
        return (optionalExceptionMap.get(System.identityHashCode(optional)));
    }

    /**
     * Return the Optional instances contained value, if present, otherwise throw the exception that is
     * associated with the Optional instance.
     *
     * @param <T> the type for the Optional parameter
     * @param optional the Optional instance to get the value for
     * @return the value of the Optional instance if no exception is associated with it
     * @throws RabbitApiException if there was an exception associated with the Optional instance
     */
    public static final <T> T orElseThrow(Optional<T> optional) throws RabbitApiException {

        RabbitApiException glea = getOptionalException(optional);
        if (glea != null) {
            throw (glea);
        }

        return (optional.get());
    }
}
