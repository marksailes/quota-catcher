package software.sailes.monitoring;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.crt.AwsCrtAsyncHttpClient;
import software.amazon.awssdk.services.servicequotas.ServiceQuotasAsyncClient;
import software.amazon.awssdk.services.servicequotas.model.GetServiceQuotaRequest;
import software.amazon.awssdk.services.servicequotas.model.GetServiceQuotaResponse;
import software.amazon.cloudwatchlogs.emf.logger.MetricsLogger;
import software.amazon.cloudwatchlogs.emf.model.DimensionSet;

import java.util.concurrent.CompletableFuture;

/**
 * Handler for requests to Lambda function.
 */
public class QuotaCatcherHandler implements RequestHandler<ScheduledEvent, String> {

    private static final Logger logger = LoggerFactory.getLogger(QuotaCatcherHandler.class);

    public static final String LAMBDA_CONCURRENCY_QUOTA_CODE = "L-B99A9384";
    public static final String LAMBDA_SERVICE_CODE = "lambda";

    private final ServiceQuotasAsyncClient serviceQuotasClient = ServiceQuotasAsyncClient.builder()
            .httpClient(AwsCrtAsyncHttpClient.create())
            .build();

    public String handleRequest(final ScheduledEvent scheduledEvent, final Context context) {
        logger.info("Event: " + scheduledEvent.toString());

        try {
            logger.info("Calling Service Quota API");
            CompletableFuture<GetServiceQuotaResponse> future = serviceQuotasClient.getServiceQuota(GetServiceQuotaRequest.builder()
                    .quotaCode(LAMBDA_CONCURRENCY_QUOTA_CODE)
                    .serviceCode(LAMBDA_SERVICE_CODE)
                    .build());
            GetServiceQuotaResponse getServiceQuotaResponse = future.get();

            Double lambdaConcurrencyQuota = getServiceQuotaResponse.quota().value();

            logger.info("Recording Lambda Concurrent Executions quota: " + lambdaConcurrencyQuota.toString());
            MetricsLogger metricsLogger = new MetricsLogger();
            metricsLogger.setNamespace("Quotas");
            metricsLogger.setDimensions(DimensionSet.of("By Service", "Lambda"));
            metricsLogger.putMetric("Lambda Concurrent Executions", lambdaConcurrencyQuota);

            metricsLogger.flush();
            logger.info("Metric flushed");

            return "ok";
        } catch (Exception e) {
            logger.error(e.getMessage());
            return "error";
        }
    }

}
