package com.gameshowcenter.offline.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LicensePayload {

    public static final String FORMAT_VERSION_V2 = "GSC-OFFLINE-LICENSE-V2";
    public static final String FORMAT_VERSION_V3 = "GSC-OFFLINE-LICENSE-V3";
    public static final String FORMAT_VERSION = FORMAT_VERSION_V3;

    public static final String STATUS_UNBOUND = "UNBOUND";
    public static final String STATUS_BOUND = "BOUND";

    private String format = FORMAT_VERSION;
    private String licenseId;
    private long createdAt;
    private int maxActivationDays = 3;
    private int annualValidityDays = 365;
    private String status = STATUS_UNBOUND;
    private String boundHwid;
    private long activatedAt = 0;
    private long expiresAt = 0;
    private String tier = "ENTERPRISE_PRO";
    private String issuer = "YonIA Enterprise Security Authority";
    private String signature; // Legacy V2 signature
    private String issuerSignature; // Asymmetric Ed25519 signature of issuance metadata
    private String boundSignature;  // Hardware bound cryptographic verification signature

    public LicensePayload() {
    }

    public boolean isBound() {
        return STATUS_BOUND.equalsIgnoreCase(status) && boundHwid != null && !boundHwid.isBlank();
    }

    public boolean isActivationWindowExpired() {
        if (isBound()) {
            return false;
        }
        long maxMillis = maxActivationDays * 24L * 60L * 60L * 1000L;
        long elapsed = System.currentTimeMillis() - createdAt;
        return elapsed > maxMillis;
    }

    public boolean isAnnualExpired() {
        if (!isBound() || expiresAt <= 0) {
            return false;
        }
        return System.currentTimeMillis() > expiresAt;
    }

    public long getRemainingDays() {
        if (!isBound() || expiresAt <= 0) {
            return 0;
        }
        long remainingMillis = expiresAt - System.currentTimeMillis();
        if (remainingMillis <= 0) {
            return 0;
        }
        return remainingMillis / (24L * 60L * 60L * 1000L);
    }

    public String formatCreatedAt() {
        return formatDate(createdAt);
    }

    public String formatExpiresAt() {
        return formatDate(expiresAt);
    }

    public String formatActivationWindowExpiry() {
        long windowExpiry = createdAt + (maxActivationDays * 24L * 60L * 60L * 1000L);
        return formatDate(windowExpiry);
    }

    private static String formatDate(long millis) {
        if (millis <= 0) return "N/A";
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault());
        return formatter.format(Instant.ofEpochMilli(millis));
    }

    /**
     * Canonical string used for asymmetric Ed25519 issuance signature.
     */
    public String getCanonicalIssuanceData() {
        return String.format("%s|%d|%d|%d|%s|%s",
                licenseId != null ? licenseId.trim() : "",
                createdAt,
                maxActivationDays,
                annualValidityDays,
                tier != null ? tier.trim() : "ENTERPRISE_PRO",
                issuer != null ? issuer.trim() : "YonIA Enterprise Security Authority");
    }

    /**
     * Canonical string used for hardware-binding cryptographic verification.
     */
    public String getCanonicalBindingData() {
        return String.format("%s|%s|%d|%d|%s",
                licenseId != null ? licenseId.trim() : "",
                boundHwid != null ? boundHwid.trim() : "",
                activatedAt,
                expiresAt,
                status != null ? status.trim() : STATUS_BOUND);
    }

    // Getters and Setters
    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String licenseId) { this.licenseId = licenseId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getMaxActivationDays() { return maxActivationDays; }
    public void setMaxActivationDays(int maxActivationDays) { this.maxActivationDays = maxActivationDays; }

    public int getAnnualValidityDays() { return annualValidityDays; }
    public void setAnnualValidityDays(int annualValidityDays) { this.annualValidityDays = annualValidityDays; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getBoundHwid() { return boundHwid; }
    public void setBoundHwid(String boundHwid) { this.boundHwid = boundHwid; }

    public long getActivatedAt() { return activatedAt; }
    public void setActivatedAt(long activatedAt) { this.activatedAt = activatedAt; }

    public long getExpiresAt() { return expiresAt; }
    public void setExpiresAt(long expiresAt) { this.expiresAt = expiresAt; }

    public String getTier() { return tier; }
    public void setTier(String tier) { this.tier = tier; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    public String getIssuerSignature() { return issuerSignature; }
    public void setIssuerSignature(String issuerSignature) { this.issuerSignature = issuerSignature; }

    public String getBoundSignature() { return boundSignature; }
    public void setBoundSignature(String boundSignature) { this.boundSignature = boundSignature; }
}
