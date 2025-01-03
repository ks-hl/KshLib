package dev.kshl.kshlib.net;

public enum HTTPResponseCode {
    // Informational responses
    CONTINUE(100, "This interim response indicates that the client should continue the request or ignore the response if the request is already finished."), //
    SWITCHING_PROTOCOLS(101, "This code is sent in response to an Upgrade request header from the client and indicates the protocol the server is switching to."), //
    PROCESSING(102, "This code indicates that the server has received and is processing the request, but no response is available yet."), //
    EARLY_HINTS(103, "This status code is primarily intended to be used with the Link header, letting the user agent start preloading resources while the server prepares a response."), //

    // Successful responses
    OK(200, "The request succeeded. The meaning of 'success' depends on the HTTP method."), //
    CREATED(201, "The request succeeded, and a new resource was created."), //
    ACCEPTED(202, "The request has been received but not yet acted upon."), //
    NON_AUTHORITATIVE_INFORMATION(203, "This response code means returned metadata is not exactly the same as from the origin server."), //
    NO_CONTENT(204, "There is no content to send for this request, but the getHeaders may be useful."), //
    RESET_CONTENT(205, "Tells the user agent to reset the document which sent this request."), //
    PARTIAL_CONTENT(206, "This response code is used when the Range header is sent from the client to request only part of a resource."), //
    MULTI_STATUS(207, "Conveys information about multiple resources."), //
    ALREADY_REPORTED(208, "Used inside a <dav:propstat> response element."), //
    IM_USED(226, "The server has fulfilled a GET request for the resource."), //

    // Redirection messages
    MULTIPLE_CHOICES(300, "The request has more than one possible response."), //
    MOVED_PERMANENTLY(301, "The URL of the requested resource has been changed permanently."), //
    FOUND(302, "The URI of requested resource has been changed temporarily."), //
    SEE_OTHER(303, "The server sent this response to direct the client to get the resource at another URI."), //
    NOT_MODIFIED(304, "This is used for caching purposes."), //
    USE_PROXY(305, "The requested response must be accessed by a proxy."), //
    TEMPORARY_REDIRECT(307, "The server sends this response to direct the client to get the resource at another URI."), //
    PERMANENT_REDIRECT(308, "The resource is now permanently located at another URI."), //

    // Client error responses
    BAD_REQUEST(400, "The server cannot or will not process the request due to a client error."), //
    UNAUTHORIZED(401, "The client must authenticate itself to get the requested response."), //
    PAYMENT_REQUIRED(402, "This response code is reserved for future use."), //
    FORBIDDEN(403, "The client does not have access rights to the content."), //
    NOT_FOUND(404, "The server cannot find the requested resource."), //
    METHOD_NOT_ALLOWED(405, "The request method is known but not supported by the target resource."), //
    NOT_ACCEPTABLE(406, "The server cannot find content matching the criteria given by the user agent."), //
    PROXY_AUTHENTICATION_REQUIRED(407, "Authentication is needed to be done by a proxy."), //
    REQUEST_TIMEOUT(408, "This response is sent on an idle connection by some servers."), //
    CONFLICT(409, "This response is sent when a request conflicts with the current state of the server."), //
    GONE(410, "The requested content has been permanently deleted from server."), //
    LENGTH_REQUIRED(411, "Server rejected the request because the Content-Length header field is not defined."), //
    PRECONDITION_FAILED(412, "The client has indicated preconditions in its getHeaders which the server does not meet."), //
    PAYLOAD_TOO_LARGE(413, "Request entity is larger than limits defined by server."), //
    URI_TOO_LONG(414, "The URI requested by the client is longer than the server is willing to interpret."), //
    UNSUPPORTED_MEDIA_TYPE(415, "The media format of the requested data is not supported by the server."), //
    RANGE_NOT_SATISFIABLE(416, "The range specified by the Range header field in the request cannot be fulfilled."), //
    EXPECTATION_FAILED(417, "The expectation indicated by the Expect request header field cannot be met by the server."), //
    IM_A_TEAPOT(418, "The server refuses the attempt to brew coffee with a teapot."), //
    MISDIRECTED_REQUEST(421, "The request was directed at a server that is not able to produce a response."), //
    UNPROCESSABLE_ENTITY(422, "The request was well-formed but was unable to be followed due to semantic errors."), //
    LOCKED(423, "The resource that is being accessed is locked."), //
    FAILED_DEPENDENCY(424, "The request failed due to failure of a previous request."), //
    TOO_EARLY(425, "Indicates that the server is unwilling to risk processing a request that might be replayed."), //
    UPGRADE_REQUIRED(426, "The server refuses to perform the request using the current protocol."), //
    PRECONDITION_REQUIRED(428, "The origin server requires the request to be conditional."), //
    TOO_MANY_REQUESTS(429, "The user has sent too many requests in a given amount of time."), //
    REQUEST_HEADER_FIELDS_TOO_LARGE(431, "The server is unwilling to process the request because its header fields are too large."), //
    UNAVAILABLE_FOR_LEGAL_REASONS(451, "The user agent requested a resource that cannot legally be provided."), //

    // Server error responses
    INTERNAL_SERVER_ERROR(500, "The server has encountered a situation it doesn't know how to handle."), //
    NOT_IMPLEMENTED(501, "The request method is not supported by the server and cannot be handled."), //
    BAD_GATEWAY(502, "The server, while working as a gateway, got an invalid response."), //
    SERVICE_UNAVAILABLE(503, "The server is not ready to handle the request."), //
    GATEWAY_TIMEOUT(504, "The server is acting as a gateway and cannot get a response in time."), //
    HTTP_VERSION_NOT_SUPPORTED(505, "The HTTP version used in the request is not supported by the server."), //
    VARIANT_ALSO_NEGOTIATES(506, "The server has an internal configuration error."), //
    INSUFFICIENT_STORAGE(507, "The method could not be performed on the resource because the server is unable to store the representation needed."), //
    LOOP_DETECTED(508, "The server detected an infinite loop while processing the request."), //
    NOT_EXTENDED(510, "Further extensions to the request are required for the server to fulfill it."), //
    NETWORK_AUTHENTICATION_REQUIRED(511, "Indicates that the client needs to authenticate to gain network access.");
    private final int code;
    private final String description;

    HTTPResponseCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String toStringCapitalized() {
        return toString().charAt(0) + toString().toLowerCase().substring(1).replace("_", " ");
    }

    public static HTTPResponseCode getByCode(int code) {
        for (HTTPResponseCode value : values()) {
            if (value.code == code) return value;
        }
        return null;
    }

    public boolean isServerError() {
        return getCode() >= 500;
    }

    public boolean isClientError() {
        return getCode() >= 400 && getCode() < 500;
    }

    public boolean isError() {
        return getCode() >= 400;
    }
}
