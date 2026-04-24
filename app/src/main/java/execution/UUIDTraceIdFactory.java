package execution;

import java.util.UUID;

public class UUIDTraceIdFactory implements TraceIdFactory {

    @Override
    public String createTraceId() {
        return UUID.randomUUID().toString();
    }
}
