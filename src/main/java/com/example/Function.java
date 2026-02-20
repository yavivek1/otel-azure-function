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
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.BatchSpanProcessor;
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.api.common.Attributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

public class Function {
    private static final Logger logger = LoggerFactory.getLogger(Function.class);
    private static final OpenTelemetry otel = initOpenTelemetry();
    private static final Tracer tracer = otel.getTracer("azure-func-tracer");

    @FunctionName("HttpExample")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.GET}, authLevel = AuthorizationLevel.ANONYMOUS) 
            HttpRequestMessage<Optional<String>> request,
            final ExecutionContext context) {

        Span span = tracer.spanBuilder("handle-request").startSpan();
        try (Scope scope = span.makeCurrent()) {
            logger.info("OTEL Instrumentation: Processing request via Logback...");
            return request.createResponseBuilder(HttpStatus.OK).body("Instrumentation Success!").build();
        } finally {
            span.end();
        }
    }

    private static OpenTelemetry initOpenTelemetry() {
        String endpoint = "http://localhost:4317"; 
        Resource res = Resource.getDefault().merge(Resource.builder()
                .put("service.name", "java-azure-func").build());

        return OpenTelemetrySdk.builder()
            .setTracerProvider(SdkTracerProvider.builder()
                .addSpanProcessor(BatchSpanProcessor.builder(OtlpGrpcSpanExporter.builder().setEndpoint(endpoint).build()).build())
                .setResource(res).build())
            .setLoggerProvider(SdkLoggerProvider.builder()
                .addLogRecordProcessor(BatchLogRecordProcessor.builder(OtlpGrpcLogRecordExporter.builder().setEndpoint(endpoint).build()).build())
                .setResource(res).build())
            .buildAndRegisterGlobal();
    }
}