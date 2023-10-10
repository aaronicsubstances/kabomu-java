package com.aaronicsubstances.kabomu.abstractions;

import java.util.function.BiFunction;
import java.util.function.Function;

public interface QuasiHttpAltTransport {
    BiFunction<QuasiHttpConnection, QuasiHttpRequest, Boolean> getRequestSerializer();
    BiFunction<QuasiHttpConnection, QuasiHttpResponse, Boolean> getResponseSerializer();
    Function<QuasiHttpConnection, QuasiHttpRequest> getRequestDeserializer();
    Function<QuasiHttpConnection, QuasiHttpResponse> getResponseDeserializer();
}
