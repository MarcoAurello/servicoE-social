package br.gov.hemobras.esocial.service;

import br.gov.hemobras.esocial.config.SSLConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

/**
 * Serviço responsável por enviar os XMLs assinados digitalmente
 * para o endpoint de Envio de Lote de Eventos do eSocial.
 */
@Service
public class EsocialService {

    private final SSLConfig sslConfig;

    @Value("${esocial.endpoints.envio}")
    private String envioUrl;

    public EsocialService(SSLConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    /**
     * Envia o XML assinado ao endpoint do eSocial via HTTPS mTLS.
     *
     * @param xmlAssinado XML completo (com assinatura XMLDSig)
     * @return Resposta XML do webservice do eSocial
     */
    public String enviarLote(String xmlAssinado) throws Exception {
        try (CloseableHttpClient client = sslConfig.createSSLClient()) {
            HttpPost post = new HttpPost(envioUrl);
            post.addHeader("Content-Type", "text/xml; charset=UTF-8");
            post.setEntity(new StringEntity(xmlAssinado, StandardCharsets.UTF_8));

            try (CloseableHttpResponse response = client.execute(post)) {
                return new String(
                        response.getEntity().getContent().readAllBytes(),
                        StandardCharsets.UTF_8
                );
            }
        }
    }
}
