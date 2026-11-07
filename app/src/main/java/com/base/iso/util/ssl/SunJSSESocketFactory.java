package com.base.iso.util.ssl;

/**
 * Kept for source compatibility. Since 2.0.0 this is the same secure-by-default TLS factory as
 * {@link GenericSSLSocketFactory} (validating trust, host name verification, injected
 * {@code KeyStore}/{@code SSLContext}, no keystore file paths). Prefer GenericSSLSocketFactory
 * in new code.
 */
public class SunJSSESocketFactory extends GenericSSLSocketFactory {
}
