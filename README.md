# OTEL-AZURE-FUNCTION
Deploy an azure function app in java with simple logic in local

Instrument it using Open telemetry (OTEL) manual instrumentation to emit metrics, traces & Logs

Logs should come from a logging pattern in logback.xml (log4j, sl4j anything is ok)

the otel trace, metrics, logs should propagate to a otel exporter(grafana alloy) and then to grafana (loki, mimir, tempo)

Java Azure Function with manual OpenTelemetry instrumentation for logs, metrics, and traces


-----

use these in grafana..


Use these in Grafana

Loki: {service_name="otel-azure-function"}
Mimir: http_server_requests_total{service_name="otel-azure-function"}
Tempo: Search with service name otel-azure-function in TraceQL/Search UI.

