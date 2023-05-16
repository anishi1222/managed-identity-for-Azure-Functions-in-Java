package org.example.functions;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.DefaultAzureCredential;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class HttpTriggerFunction {

    static final Map<String, TargetInfo> targetInfo = new ConcurrentHashMap<String, TargetInfo>();

    public HttpTriggerFunction() {
        targetInfo.put("c02", new TargetInfo("https://func-cs02.azurewebsites.net/api/auth-cs", "appId for func-cs02"));
        targetInfo.put("j02", new TargetInfo("https://func-j02.azurewebsites.net/api/auth-java", "appId for func-j02"));
    }

    @FunctionName("call-from-java")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        // Parse query parameter
        final String funcType = request.getQueryParameters().get("type");

        if (Optional.ofNullable(funcType).isEmpty()) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a type on the query string or in the request body[1]").build();
        }
        if (targetInfo.containsKey(funcType.toLowerCase())) {
            ResMsg responseMessage = new ResMsg();
            try {
                DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder().build();
                TokenRequestContext tokenRequestContext = new TokenRequestContext().addScopes(String.format("api://%s/.default", targetInfo.get(funcType).audienceID()));
                String accessToken = defaultAzureCredential.getToken(tokenRequestContext).map(AccessToken::getToken).block();
                HttpClient httpClient = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();
                context.getLogger().info(String.format("token [%s]", accessToken));
                HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(targetInfo.get(funcType).targetUrl()))
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                responseMessage.setStatus(httpResponse.statusCode());
                if (httpResponse.statusCode() != 200) {
                    responseMessage.setResponse(String.format("responded status [%d]", httpResponse.statusCode()));
                } else {
                    responseMessage.setResponse(httpResponse.body());
                }
            } catch (Exception e) {
                context.getLogger().severe(e.getLocalizedMessage());
                e.printStackTrace();
                responseMessage.setResponse(e.getLocalizedMessage());
                responseMessage.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
            }

            return request.createResponseBuilder(HttpStatus.OK).header("Content-Type", "application/json").body(responseMessage).build();
        } else {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Your specified API type is out of scope[" + funcType + "].").build();
        }

    }
}
