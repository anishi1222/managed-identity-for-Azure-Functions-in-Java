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
import java.time.Duration;
import java.util.Optional;

public class HttpTriggerFunction {

    static final String audienceID = "";
    static final String targetUrl = "";

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
        if (funcType.length() < 1) {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Please pass a type on the query string or in the request body[2]").build();
        }

        switch (funcType.toLowerCase())
        {
            case "c02":
            case "j02":
                break;
            default:
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST).body("Your specified API type is out of scope[" + funcType + "].").build();
        }

        ResMsg responseMessage = new ResMsg();
        try {
            DefaultAzureCredential defaultAzureCredential = new DefaultAzureCredentialBuilder().build();
            TokenRequestContext tokenRequestContext = new TokenRequestContext().addScopes("api://" + audienceID + "/.default");
            String accessToken = defaultAzureCredential.getToken(tokenRequestContext).map(AccessToken::getToken).block();
            HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofSeconds(60))
                .build();

            HttpRequest httpRequest = HttpRequest.newBuilder(URI.create(targetUrl))
                .setHeader("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            responseMessage.setStatus(httpResponse.statusCode());
            if(httpResponse.statusCode()!=200) {
                responseMessage.setResponse("Unauthorized");
            } else {
                responseMessage.setResponse(httpResponse.body());
            }
        }
        catch (Exception e) {
            context.getLogger().severe(e.getLocalizedMessage());
            e.printStackTrace();
            responseMessage.setResponse(e.getLocalizedMessage());
            responseMessage.setStatus(HttpStatus.SERVICE_UNAVAILABLE.value());
        }

        return request.createResponseBuilder(HttpStatus.OK).header("Content-Type","application/json").body(responseMessage).build();
    }
}
