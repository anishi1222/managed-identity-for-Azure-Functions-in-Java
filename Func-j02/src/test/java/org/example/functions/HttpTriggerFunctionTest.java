package org.example.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;

class HttpTriggerFunctionTest {

    @Test
    void runReturnsOkResponseWhenAuthorizationHeaderContainsJwt() {
        HttpTriggerFunction function = new HttpTriggerFunction();
        String token = JWT.create()
                .withAudience("api://func-j02")
                .withClaim("appid", "caller-app-id")
                .withClaim("oid", "caller-object-id")
                .sign(Algorithm.none());
        HttpRequestMessage<Optional<String>> request = requestWithHeaders(Map.of("authorization", "Bearer " + token));

        HttpResponseMessage response = function.run(request, executionContext());

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals("This is responded by function[auth-java] in func-j02. It means HTTP triggered function executed successfully.", response.getBody());
    }

    private static HttpRequestMessage<Optional<String>> requestWithHeaders(Map<String, String> headers) {
        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);
        when(request.getHeaders()).thenReturn(headers);
        when(request.createResponseBuilder(HttpStatus.OK)).thenAnswer(invocation -> new TestResponseBuilder().status(invocation.getArgument(0)));
        return request;
    }

    private static ExecutionContext executionContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger(HttpTriggerFunctionTest.class.getName()));
        when(context.getFunctionName()).thenReturn("auth-java");
        return context;
    }

    private static final class TestResponseMessage implements HttpResponseMessage {
        private final HttpStatusType status;
        private final Map<String, String> headers;
        private final Object body;

        private TestResponseMessage(HttpStatusType status, Map<String, String> headers, Object body) {
            this.status = status;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public HttpStatusType getStatus() {
            return status;
        }

        @Override
        public int getStatusCode() {
            return status.value();
        }

        @Override
        public String getHeader(String key) {
            return headers.get(key);
        }

        @Override
        public Object getBody() {
            return body;
        }
    }

    private static final class TestResponseBuilder implements HttpResponseMessage.Builder {
        private HttpStatusType status;
        private final Map<String, String> headers = new HashMap<>();
        private Object body;

        @Override
        public HttpResponseMessage.Builder status(HttpStatusType status) {
            this.status = status;
            return this;
        }

        @Override
        public HttpResponseMessage.Builder header(String key, String value) {
            headers.put(key, value);
            return this;
        }

        @Override
        public HttpResponseMessage.Builder body(Object body) {
            this.body = body;
            return this;
        }

        @Override
        public HttpResponseMessage build() {
            return new TestResponseMessage(status, Map.copyOf(headers), body);
        }
    }
}