package com.medibook.backend.config;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class VnpayProperties {

    private final String tmnCode;
    private final String hashSecret;
    private final String payUrl;
    private final String returnUrl;
    private final long amount;
    private final int expireMinutes;

    public VnpayProperties(
            @Value("${app.vnpay.tmn-code:}") String tmnCode,
            @Value("${app.vnpay.hash-secret:}") String hashSecret,
            @Value("${app.vnpay.pay-url:https://sandbox.vnpayment.vn/paymentv2/vpcpay.html}")
            String payUrl,
            @Value("${app.vnpay.return-url:http://localhost:5173/payment/vnpay-return}")
            String returnUrl,
            @Value("${app.vnpay.amount:100000}") long amount,
            @Value("${app.vnpay.expire-minutes:15}") int expireMinutes
    ) {
        this.tmnCode = tmnCode.trim();
        this.hashSecret = hashSecret.trim();
        this.payUrl = payUrl.trim();
        this.returnUrl = returnUrl.trim();
        this.amount = amount;
        this.expireMinutes = expireMinutes;
    }

    public void requireConfigured() {
        if (!tmnCode.matches("[A-Za-z0-9]{8}")) {
            throw new IllegalStateException(
                    "Chua cau hinh VNPAY_TMN_CODE hop le."
            );
        }

        if (hashSecret.isBlank()) {
            throw new IllegalStateException(
                    "Chua cau hinh VNPAY_HASH_SECRET."
            );
        }

        if (amount != 100000L) {
            throw new IllegalStateException(
                    "Phi kham hien tai phai la 100000 VND."
            );
        }

        if (expireMinutes < 1 || expireMinutes > 15) {
            throw new IllegalStateException(
                    "Thoi han thanh toan phai tu 1 den 15 phut."
            );
        }

        if (!"https://sandbox.vnpayment.vn/paymentv2/vpcpay.html"
                .equals(payUrl)) {
            throw new IllegalStateException(
                    "Giai doan nay chi ho tro endpoint VNPAY Sandbox."
            );
        }

        URI uri;

        try {
            uri = URI.create(returnUrl);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "VNPAY Return URL khong hop le."
            );
        }

        boolean https = "https".equalsIgnoreCase(uri.getScheme());

        boolean localHttp =
                "http".equalsIgnoreCase(uri.getScheme())
                        && "localhost".equalsIgnoreCase(uri.getHost());

        if ((!https && !localHttp)
                || uri.getHost() == null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || uri.getRawUserInfo() != null
                || returnUrl.length() > 255) {
            throw new IllegalStateException(
                    "VNPAY Return URL phai la HTTPS hoac HTTP localhost, "
                            + "khong chua query, fragment hay user info."
            );
        }
    }

    public String getTmnCode() {
        return tmnCode;
    }

    public String getHashSecret() {
        return hashSecret;
    }

    public String getPayUrl() {
        return payUrl;
    }

    public String getReturnUrl() {
        return returnUrl;
    }

    public long getAmount() {
        return amount;
    }

    public int getExpireMinutes() {
        return expireMinutes;
    }
}