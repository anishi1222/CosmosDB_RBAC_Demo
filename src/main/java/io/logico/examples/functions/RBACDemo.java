package io.logico.examples.functions;

import com.azure.core.credential.TokenCredential;
import com.azure.cosmos.CosmosClient;
import com.azure.cosmos.CosmosClientBuilder;
import com.azure.cosmos.CosmosContainer;
import com.azure.cosmos.CosmosException;
import com.azure.cosmos.models.CosmosItemRequestOptions;
import com.azure.cosmos.models.CosmosItemResponse;
import com.azure.cosmos.models.CosmosQueryRequestOptions;
import com.azure.cosmos.models.PartitionKey;
import com.azure.cosmos.util.CosmosPagedIterable;
import com.azure.identity.DefaultAzureCredentialBuilder;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Optional;

/**
 * Azure Functions with HTTP Trigger.
 */
public class RBACDemo {

    private static final String DATABASE = "Inventories";
    private static final String CONTAINER = "Tracks";

    @FunctionName("readonly")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.GET, HttpMethod.POST, HttpMethod.DELETE},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<Track>> request,
            final ExecutionContext context) {
        TokenCredential tokenCredential;
        tokenCredential = new DefaultAzureCredentialBuilder().build();
        HttpResponseMessage responseMessage;

        String acountEndPoint = System.getenv("ACCOUNT_ENDPOINT");
        if(acountEndPoint.isEmpty() || acountEndPoint.isBlank()) {
            HashMap<String, String> responseBody = new HashMap<>();
            responseBody.put("error", "ACCOUNT_ENDPOINT is not set in Application Settings.");
            return request.createResponseBuilder(HttpStatus.FAILED_DEPENDENCY)
                    .body(responseBody)
                    .header("Content-Type","application/json")
                    .build();
        }
        try (CosmosClient client = new CosmosClientBuilder()
                .endpoint(acountEndPoint)
                .credential(tokenCredential)
                .gatewayMode()
                .buildClient()) {
            CosmosContainer cosmosContainer = client.getDatabase(DATABASE).getContainer(CONTAINER);

            context.getLogger().info(request.getHttpMethod().name().toUpperCase(Locale.ROOT));
            switch (request.getHttpMethod().name().toUpperCase(Locale.ROOT)) {
                case "GET":
                    String sql = "select c.id, c.purchaseDate, c.remarks, c.type from c";
                    context.getLogger().info(sql);
                    CosmosQueryRequestOptions cosmosQueryRequestOptions
                            = new CosmosQueryRequestOptions().setMaxDegreeOfParallelism(2);
                    ArrayList<Track> trackArrayList = new ArrayList<>();
                    CosmosPagedIterable<Track> trackCosmosPagedIterable = cosmosContainer.queryItems(sql, cosmosQueryRequestOptions, Track.class);
                    trackCosmosPagedIterable.forEach(trackArrayList::add);
                    responseMessage = request.createResponseBuilder(HttpStatus.OK)
                            .header("Content-Type", "application/json")
                            .body(trackArrayList)
                            .build();
                    break;
                case "POST":
                    if (request.getBody().isEmpty()) {
                        HashMap<String, String> errorMessage = new HashMap<>();
                        errorMessage.put("errorMessage", "Data is mandatory.");
                        responseMessage = request.createResponseBuilder(HttpStatus.BAD_REQUEST).header("Content-Type", "application/json").body(errorMessage).build();
                    } else {
                        Track track = request.getBody().get();
                        CosmosItemResponse<Track> cosmosItemResponse = cosmosContainer.upsertItem(track);
                        context.getLogger().info("[Diagnostics] " + cosmosItemResponse.getDiagnostics().toString());
                        context.getLogger().info("[Status Code from CosmosDB]" + cosmosItemResponse.getStatusCode());
//                        System.out.printf("status [%d] [%s]¥n", cosmosItemResponse.getStatusCode(), cosmosItemResponse.getDiagnostics().toString());
                        responseMessage = request.createResponseBuilder(HttpStatusType.custom(cosmosItemResponse.getStatusCode()))
                                .header("Content-Type", "application/json")
                                .body(track)
                                .build();
                    }
                    break;
                case "DELETE":
                    if (request.getBody().isEmpty()) {
                        HashMap<String, String> errorMessage = new HashMap<>();
                        errorMessage.put("errorMessage", "Data is mandatory.");
                        responseMessage = request.createResponseBuilder(HttpStatus.BAD_REQUEST).header("Content-Type", "application/json").body(errorMessage).build();
                    } else {
                        Track track = request.getBody().get();
                        CosmosItemResponse<Object> cosmosItemResponse = cosmosContainer.deleteItem(track.getId(), new PartitionKey(track.getId()), new CosmosItemRequestOptions());
                        context.getLogger().info("[Diagnostics] " + cosmosItemResponse.getDiagnostics().toString());
                        context.getLogger().info("[Status Code from CosmosDB] " + cosmosItemResponse.getStatusCode());
//                        System.out.printf("status [%d] [%s]¥n", cosmosItemResponse.getStatusCode(), cosmosItemResponse.getDiagnostics().toString());
                        responseMessage = request.createResponseBuilder(HttpStatusType.custom(cosmosItemResponse.getStatusCode()))
                                .header("Content-Type", "application/json").body(track).build();
                    }
                    break;
                default:
                    HashMap<String, String> errorMessage = new HashMap<>();
                    errorMessage.put("errorMessage", "HTTP method is not allowed [" + request.getHttpMethod().name() + "]");
                    responseMessage = request.createResponseBuilder(HttpStatus.METHOD_NOT_ALLOWED).header("Content-Type", "application/json").body(errorMessage).build();
                    break;
            }
        } catch (CosmosException e) {
            context.getLogger().info(e.toString());
            responseMessage = request.createResponseBuilder(HttpStatus.valueOf(e.getStatusCode()))
                    .body(e.getMessage())
                    .header("content-type", "application/json")
                    .build();
        }
        return responseMessage;
    }
}
