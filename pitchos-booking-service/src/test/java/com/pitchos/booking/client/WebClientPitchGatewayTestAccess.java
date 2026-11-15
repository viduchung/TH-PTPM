package com.pitchos.booking.client;

/** Cho test ở package khác gọi phương thức phân loại lỗi (package-private) của WebClientPitchGateway. */
public final class WebClientPitchGatewayTestAccess {

    private WebClientPitchGatewayTestAccess() {
    }

    public static boolean isTransient(Throwable error) {
        return WebClientPitchGateway.isTransient(error);
    }
}
