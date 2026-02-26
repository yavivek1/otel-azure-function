package com.example;

import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.*;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.exporter.otlp.metrics.OtlpGrpcMetricExporter;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;
import java.time.Duration;

public class Function {
    private static final Logger logger = LoggerFactory.getLogger(Function.class);
    private static final OpenTelemetry otel = initOpenTelemetry();
    private static final Tracer tracer = otel.getTracer("azure-func-tracer");
    private static final io.opentelemetry.api.logs.Logger otelLogger = otel.getLogsBridge().loggerBuilder("azure-func-logger").build();
    private static final Meter meter = otel.getMeter("azure-func-meter");
    private static final LongCounter requestCounter = meter.counterBuilder("http.server.requests")
            .setDescription("Count of HTTP requests handled by Azure Function")
            .build();

    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.ANONYMOUS) 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        Span span = tracer.spanBuilder("handle-request").startSpan();
        try (Scope scope = span.makeCurrent()) {
            logger.info("OTEL Instrumentation: Processing request via Logback...");
            otelLogger.logRecordBuilder()
                    .setSeverity(Severity.INFO)
                    .setBody("Function request processed")
                    .emit();
            requestCounter.add(1);
            return request.createResponseBuilder(HttpStatus.OK).body("Instrumentation Success!").build();
        } finally {
            span.end();
        }
    }

    private static OpenTelemetry initOpenTelemetry() {
        String endpoint = System.getenv().getOrDefault("OTEL_EXPORTER_OTLP_ENDPOINT", "http://localhost:4317");
        Resource res = Resource.getDefault().merge(Resource.builder()
                .put("service.name", "otel-azure-function").build());

        return OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build()).build())
                .setResource(res).build())
            .setLoggerProvider(SdkLoggerProvider.builder()
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(OtlpGrpcLogRecordExporter.builder().setEndpoint(endpoint).build()).build())
                .setResource(res).build())
            .setMeterProvider(SdkMeterProvider.builder()
                .registerMetricReader(PeriodicMetricReader.builder(OtlpGrpcMetricExporter.builder().setEndpoint(endpoint).build())
                    .setInterval(Duration.ofSeconds(10))
                    .build())
                .setResource(res)
                .build())
            .buildAndRegisterGlobal();
    }
}