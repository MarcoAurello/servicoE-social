package br.gov.hemobras.esocial.config;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.socket.PlainConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.config.Registry;
import org.apache.hc.core5.http.config.RegistryBuilder;
import org.apache.hc.core5.ssl.SSLContexts;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.security.KeyStore;

/**
 * Configuração SSL para criar cliente HTTP com certificado A1 (.pfx)
 * e autenticação mútua (mTLS) com os webservices do eSocial.
 */
@Configuration
public class SSLConfig {

    @Value("${esocial.cert.path}")
    private String certPath;

    @Value("${esocial.cert.password}")
    private String certPassword;

    /**
     * Cria e retorna um cliente HTTP configurado com o certificado A1 (.pfx)
     * para realizar requisições seguras ao eSocial.
     */
    public CloseableHttpClient createSSLClient() throws Exception {
        // Carrega o PKCS#12 (.pfx)
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(certPath)) {
            keyStore.load(fis, certPassword.toCharArray());
        }

        // Cria o contexto SSL com o certificado
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, certPassword.toCharArray())
                .build();

        // Cria o socket factory para HTTPS
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

        // Registra HTTP e HTTPS
        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory)
                .build();

        // Gerenciador de conexões com pool
        PoolingHttpClientConnectionManager connectionManager =
                new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setMaxTotal(50);
        connectionManager.setDefaultMaxPerRoute(10);

        // Cria o cliente HTTP final
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(
                        org.apache.hc.client5.http.config.RequestConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(10))
                                .setResponseTimeout(Timeout.ofSeconds(30))
                                .build()
                )
                .build();
    }
}
