# Telemetry data collection

Telemetry events can be collected by the LSP client program.

The following information is emitted when the language server starts:

 * JVM information:
    * Whether it is being run with Java or as a GraalVM native image (binary)
    * The name of the vm (`java.vm.name`)
    * The name of the runtime (`java.runtime.name`)
    * The version of the JVM (`java.version`)
    * The free, total, and max VM memory
 * Note: Does NOT include the `JAVA_HOME` environment variable for privacy reasons

Currently, the startup event is the only telemetry event that is emitted.