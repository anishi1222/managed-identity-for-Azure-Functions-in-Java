package org.example.functions;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class HttpTriggerFunction {
    @FunctionName("auth-java")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {
        request.getHeaders().forEach((k,v)->context.getLogger().info(String.format("%s: %s", k, v)));
        String[] token = request.getHeaders().get("authorization").split("\\s+");
        DecodedJWT decodedJWT = JWT.decode(token[1]);
        context.getLogger().info(String.format("<<%s>> Audience [%s] appId [%s] oid[%s]",
            context.getFunctionName(), decodedJWT.getAudience().get(0), decodedJWT.getClaim("appid").asString(), decodedJWT.getClaim("oid").asString()));
        String responseMessage = "This is responded by function[auth-java] in func-j02. It means HTTP triggered function executed successfully.";
        return request.createResponseBuilder(HttpStatus.OK)
            .body(responseMessage)
            .build();
    }
}
