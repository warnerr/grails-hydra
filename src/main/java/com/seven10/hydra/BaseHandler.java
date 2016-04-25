package com.seven10.hydra;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.seven10.hydrawebservice.BaseHttpServlet;
import com.seven10.hydrawebservice.exception.BadRequestException;
import com.seven10.hydrawebservice.exception.InvalidMediaTypeException;
import com.seven10.hydrawebservice.helpers.JsonObjectTransformer;
import com.seven10.hydrawebservice.security.HydraSecurityManager;
import grails.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.shiro.session.ExpiredSessionException;
import org.apache.shiro.session.InvalidSessionException;
import org.apache.shiro.session.UnknownSessionException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.security.InvalidParameterException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by root on 4/25/16.
 */
public abstract class BaseHandler {

    /**
     * The Constant m_logger.
     */
    private static final Logger m_logger = LogManager.getFormatterLogger(com.seven10.hydrawebservice.BaseHttpServlet.class.getName());

    /**
     * The Constant SERVLET_ERROR_CODE.
     */
    public static final String SERVLET_ERROR_CODE = "code";

    /**
     * The Constant SERVLET_ERROR_DESCRIPTON.
     */
    public static final String SERVLET_ERROR_DESCRIPTON = "description";

    /**
     * The Constant SERVLET_ERROR_DESCRIPTION_NO_BODY.
     */
    public static final String SERVLET_ERROR_DESCRIPTION_NO_BODY = "Body was empty";

    /**
     * The Constant SERVLET_CONTENT_TYPE_JSON.
     */
    public static final String SERVLET_CONTENT_TYPE_JSON = "application/json";

    /**
     * The Constant SERVLET_CONTENT_TYPE_TEXT.
     */
    public static final String SERVLET_CONTENT_TYPE_TEXT = "text/plain";

    /**
     * The m_sec manager.
     */
    private static HydraSecurityManager m_secManager;

    /**
     * Instantiates a new base http servlet.
     *
     * @param secManager the sec manager
     */
    public BaseHandler(HydraSecurityManager secManager) {
        m_secManager = secManager;
    }

    /**
     * Gets the base endpoint.
     *
     * @return the base endpoint
     */
    public abstract String getBaseEndpoint();

    public BaseHandler() {

    }

    /**
     * Validate the auth header is valid, not expired.
     *
     * @param request
     * @param response
     * @throws IOException
     * @returns true if auth header is valid, false otherwise and writes the reason and code into the response
     */
    protected boolean validateAuthHeader(HttpServletRequest request,
                                         HttpServletResponse response) throws IOException {
        boolean rval = false;
        try {
            checkAuthorizationHeader(request.getHeader("x-seven10-auth"));
            rval = true;
        } catch (ExpiredSessionException ex) {
            writeResponseAndStatus(response,
                    createJSONErrorBody("Session has Expired"),
                    com.seven10.hydrawebservice.BaseHttpServlet.SERVLET_CONTENT_TYPE_JSON,
                    401);
        } catch (UnknownSessionException ex) {
            writeResponseAndStatus(response,
                    createJSONErrorBody("No Sesssion with id: " + request.getHeader("x-seven10-auth") + " found"),
                    com.seven10.hydrawebservice.BaseHttpServlet.SERVLET_CONTENT_TYPE_JSON,
                    401);
        } catch (NullPointerException ex) {
            writeResponseAndStatus(response,
                    createJSONErrorBody("Session is missing"),
                    com.seven10.hydrawebservice.BaseHttpServlet.SERVLET_CONTENT_TYPE_JSON,
                    401);
        } catch (Exception ex) {
            writeResponseAndStatus(response,
                    createJSONErrorBody("Internal error - " + ex.getMessage()),
                    com.seven10.hydrawebservice.BaseHttpServlet.SERVLET_CONTENT_TYPE_JSON,
                    401);
        }
        return rval;
    }

    /**
     * Get a map of the parameters.
     *
     * @param headerValue the header value
     * @throws InvalidSessionException the invalid session exception
     * @throws NullPointerException    the null pointer exception
     * @throws Exception               the exception
     */
    protected void checkAuthorizationHeader(String headerValue) throws InvalidSessionException, NullPointerException, Exception {
        Serializable serialSessionId = headerValue;

        // Check to see if the Session is valid.
        m_secManager.checkSessionID(serialSessionId);
    }

    /**
     * Get a map of the parameters.
     *
     * @param request the request
     * @return the map
     */
    protected Map<String, String> readRequestParameters(HttpServletRequest request) {
        m_logger.trace(".readRequestParameters(): Retrieving request parameters");

        // Determine if we have request query params
        @SuppressWarnings("unchecked")
        Enumeration<String> requestParams = request.getParameterNames();
        Map<String, String> parameterMap = null;

        //If we have parameters to process
        if (requestParams.hasMoreElements()) {
            // Map of the params to their value, so we can track and work on it
            parameterMap = new HashMap<String, String>();

            // Cycle through each of the parameter names and save them to our map
            while (requestParams.hasMoreElements()) {
                String strParam = requestParams.nextElement();
                parameterMap.put(strParam, request.getParameter(strParam));
            }
        }

        return parameterMap;
    }

    /**
     * Check whether request has an {@literal id="id"} parameter.
     *
     * @param request the HttpServletRequest
     * @return the id.
     * @throws BadRequestException the bad request exception
     */
    protected String requestHasId(HttpServletRequest request) throws BadRequestException {
        m_logger.trace(".requestHasId(): attempting to find 'id' query parameter");
        Map<String, String> requestParams = readRequestParameters(request);

        if ((requestParams == null)
                || !requestParams.containsKey("id"))
            return null;

        if (requestParams.get("id") == null) {
            m_logger.trace(".requestHasId(): 'id' parameter present but null");
            throw new BadRequestException("'id' parameter can not be null");
        }

        return requestParams.get("id");
    }

    /**
     * Convenience method to quickly return the Json Body of a request.
     *
     * @param request the request
     * @return the body json
     * @throws InvalidParameterException the invalid parameter exception
     * @throws JsonParseException        the json parse exception
     * @throws InvalidMediaTypeException the invalid media type exception
     * @throws IOException               Signals that an I/O exception has occurred.
     */
    protected JsonObject getBodyJson(HttpServletRequest request) throws InvalidParameterException,
            JsonParseException,
            InvalidMediaTypeException,
            IOException {
        return JsonObjectTransformer.convertStringToJson(getBodyString(request));
    }

    /**
     * Convert a request's body into a String.
     *
     * @param request POST, PUT or PATCH
     * @return The String of the JSON representation of the request's body
     * @throws InvalidMediaTypeException the invalid media type exception
     * @throws InvalidParameterException the invalid parameter exception
     * @throws IOException               Signals that an I/O exception has occurred.
     */
    protected String getBodyString(HttpServletRequest request) throws InvalidMediaTypeException,
            InvalidParameterException,
            IOException {
        m_logger.trace(".getBodyString(): converting request body into JSON string (size in bytes:%d)",
                request.getContentLength());

        // Ensure content length is not zero
        if (request.getContentLength() == 0) {
            m_logger.error(".getBodyString(): payload is zero bytes");
            throw new InvalidParameterException("Content length is zero");
        }
        // Make sure Content Type = application/json
        validateContentType(request);

        // The to be returned body
        String strRequestBody = "";

        // Read the data into the buffer
        try {
            strRequestBody = IOUtils.toString(request.getInputStream());
        } catch (IOException ex) {
            m_logger.error(".getBodyString(): generated an exception reading body - Exception:%s", ex.toString());
            m_logger.trace(".getBodyString(): generated an IOException", ex);
            throw ex;
        }

        m_logger.trace(".getBodyString(): successfully read request Json body");
        return strRequestBody;
    }

    /**
     * Validates the content type as being of the type "application/json"
     * Hackity hack to work with the fact that the content type and character encoding is a freaking set
     *
     * @param request the request
     * @throws InvalidMediaTypeException the invalid media type exception
     */
    private void validateContentType(HttpServletRequest request) throws InvalidMediaTypeException {
        m_logger.trace(".validateContentType(): Validating the content type of the request to ensure it's application/json");

        String strFullContentType = request.getContentType();
        if (strFullContentType == null || strFullContentType.isEmpty()) {
            m_logger.error(".validateContentType(): ContentType is not present");
            throw new InvalidMediaTypeException("ContentType not present");
        }

        String strContentType;
        //If we have the content type and character set, the content type will be the head of this string
        if (strFullContentType.contains(";")) {
            String[] contentTypeParams = strFullContentType.split(";");
            strContentType = contentTypeParams[0];
        } else
            strContentType = strFullContentType;

        if (!strContentType.contentEquals("application/json")) {
            m_logger.error(".validateContentType(): Request content type is not application/json, it is %s", strContentType);
            throw new InvalidMediaTypeException("Application type " + strContentType + " is not application/json");
        }
    }

    /**
     * Create a JSON error body message that looks like:
     * {
     * "Error": "Message"
     * }
     *
     * @param bodyErrorMessage
     * @return full JSON body
     */
    protected String createJSONErrorBody(String bodyErrorMessage) {
        return JsonObjectTransformer.formatJSONErrorBody("Error", bodyErrorMessage);
    }

    /**
     * Set a response and status.
     *
     * @param response       to write to.
     * @param strReply       (optional) String containing the body of the response
     * @param strContentType (optional) String specifying the content-type of {@literal strReply}
     * @param httpStatusCode - HTTP code to set in the response
     * @throws IOException Signals that an I/O exception has occurred.
     */
    protected void writeResponseAndStatus(HttpServletResponse response,
                                          String strReply,
                                          String strContentType,
                                          int httpStatusCode) throws IOException {
        // If there is no body, just respond with an empty.
        if ((strReply == null) || strReply.isEmpty()) {
            m_logger.trace(".writeResponseAndStatus(): writing response -- Code:%d",
                    httpStatusCode);

            response.setStatus(httpStatusCode);
            return;
        }

        m_logger.trace(".writeResponseAndStatus(): writing response -- Code:%d Reply:%s",
                httpStatusCode,
                strReply);

        response.setStatus(httpStatusCode);
        response.setContentType(strContentType);

        writeResponse(response, strReply);
    }

    /**
     * Write a response using a string.
     *
     * @param response to write to.
     * @param strReply string of the body in which to reply
     * @throws IOException               Signals that an I/O exception has occurred.
     * @throws InvalidParameterException reply is null
     */
    private void writeResponse(HttpServletResponse response,
                               String strReply) throws IOException {
        m_logger.trace(".writeResponse(): writing response");

        Writer writer;
        try {
            writer = response.getWriter();
            writer.write(strReply);
            writer.close();
        } catch (IOException ex) {
            m_logger.error(".writeResponse(): Generated an exception reading body - %s", ex.toString());
            m_logger.trace(".writeResponse(): Generated an IOException", ex);
            throw new IOException(ex);
        }

        m_logger.trace(".writeResponse(): response written");
    }

}
