package org.example.functions;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.HttpStatusType;

class HttpTriggerFunctionTest {

    @Test
    void runReturnsBadRequestWhenTypeIsMissing() {
        HttpTriggerFunction function = new HttpTriggerFunction();
        HttpRequestMessage<Optional<String>> request = requestWithQueryParameters(Map.of());

        HttpResponseMessage response = function.run(request, executionContext());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertEquals("Please pass a type on the query string or in the request body[1]", response.getBody());
    }

    @Test
    void runReturnsBadRequestWhenTypeIsUnsupported() {
        HttpTriggerFunction function = new HttpTriggerFunction();
        HttpRequestMessage<Optional<String>> request = requestWithQueryParameters(Map.of("type", "unknown"));

        HttpResponseMessage response = function.run(request, executionContext());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatus());
        assertEquals("Your specified API type is out of scope[unknown].", response.getBody());
    }

    private static HttpRequestMessage<Optional<String>> requestWithQueryParameters(Map<String, String> queryParameters) {
        @SuppressWarnings("unchecked")
        HttpRequestMessage<Optional<String>> request = mock(HttpRequestMessage.class);
        when(request.getQueryParameters()).thenReturn(queryParameters);
        when(request.createResponseBuilder(HttpStatus.BAD_REQUEST)).thenAnswer(invocation -> new TestResponseBuilder().status(invocation.getArgument(0)));
        return request;
    }

    private static ExecutionContext executionContext() {
        ExecutionContext context = mock(ExecutionContext.class);
        when(context.getLogger()).thenReturn(Logger.getLogger(HttpTriggerFunctionTest.class.getName()));
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