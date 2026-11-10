package com.medibook.backend.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Component;

import com.medibook.backend.config.VnpayProperties;

@Component
public class VnpayGateway {

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final VnpayProperties properties;

    public VnpayGateway(VnpayProperties properties) {
        this.properties = properties;
    }

    public String createPaymentUrl(
            String txnRef,
            LocalDateTime createdAt,
            LocalDateTime expiresAt,
            String clientIp
    ) {
        properties.requireConfigured();

        if (txnRef == null
                || !txnRef.matches("[A-Za-z0-9]{1,100}")) {
            throw new IllegalArgumentException(
                    "Ma tham chieu thanh toan khong hop le."
            );
        }

        if (createdAt == null
                || expiresAt == null
                || !expiresAt.isAfter(createdAt)
                || expiresAt.isAfter(
                        createdAt.plusMinutes(
                                properties.getExpireMinutes()
                        )
                )) {
            throw new IllegalArgumentException(
                    "Thoi gian thanh toan khong hop le."
            );
        }

        if (clientIp == null
                || clientIp.length() < 3
                || clientIp.length() > 45
                || !clientIp.matches("[0-9A-Fa-f:.]+")) {
            throw new IllegalArgumentException(
                    "Dia chi IP thanh toan khong hop le."
            );
        }

        long gatewayAmount = Math.multiplyExact(
                properties.getAmount(),
                100L
        );

        Map<String, String> parameters = new TreeMap<>();

        parameters.put("vnp_Version", "2.1.0");
        parameters.put("vnp_Command", "pay");
        parameters.put("vnp_TmnCode", properties.getTmnCode());
        parameters.put("vnp_Amount", Long.toString(gatewayAmount));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", txnRef);
        parameters.put(
                "vnp_OrderInfo",
                "Thanh toan lich kham MediBook " + txnRef
        );
        parameters.put("vnp_OrderType", "other");
        parameters.put("vnp_Locale", "vn");
        parameters.put("vnp_ReturnUrl", properties.getReturnUrl());
        parameters.put("vnp_IpAddr", clientIp);
        parameters.put("vnp_CreateDate", DATE_FORMAT.format(createdAt));
        parameters.put("vnp_ExpireDate", DATE_FORMAT.format(expiresAt));

        String query = canonicalQuery(parameters);
        String signature = sign(query);

        return properties.getPayUrl()
                + "?"
                + query
                + "&vnp_SecureHash="
                + signature;
    }

    public boolean hasValidSignature(
            Map<String, String> responseParameters
    ) {
        properties.requireConfigured();

        if (responseParameters == null
                || responseParameters.isEmpty()) {
            return false;
        }

        String receivedHash =
                responseParameters.get("vnp_SecureHash");

        if (receivedHash == null
                || !receivedHash.matches("[0-9A-Fa-f]{128}")) {
            return false;
        }

        Map<String, String> signedParameters = new TreeMap<>();

        responseParameters.forEach((key, value) -> {
            if (key != null
                    && key.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(key)
                    && !"vnp_SecureHashType".equals(key)
                    && value != null
                    && !value.isEmpty()) {
                signedParameters.put(key, value);
            }
        });

        if (signedParameters.isEmpty()) {
            return false;
        }

        String expectedHash = sign(
                canonicalQuery(signedParameters)
        );

        byte[] expectedBytes =
                HexFormat.of().parseHex(expectedHash);

        byte[] receivedBytes =
                HexFormat.of().parseHex(receivedHash);

        return MessageDigest.isEqual(
                expectedBytes,
                receivedBytes
        );
    }

    private String canonicalQuery(
            Map<String, String> parameters
    ) {
        StringJoiner query = new StringJoiner("&");

        new TreeMap<>(parameters).forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                query.add(
                        encode(key) + "=" + encode(value)
                );
            }
        });

        return query.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(
                value,
                StandardCharsets.UTF_8
        );
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");

            SecretKeySpec key = new SecretKeySpec(
                    properties.getHashSecret()
                            .getBytes(StandardCharsets.UTF_8),
                    "HmacSHA512"
            );

            mac.init(key);

            byte[] signature = mac.doFinal(
                    data.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(signature);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "Khong the tao chu ky VNPAY.",
                    exception
            );
        }
    }
}