package br.gov.hemobras.esocial.service;

import br.gov.hemobras.esocial.config.SSLConfig;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.nio.charset.StandardCharsets;

@Service
public class ConsultaLoteService {

    private final SSLConfig sslConfig;

    @Value("${esocial.endpoints.consulta:https://webservices.producaorestrita.esocial.gov.br/servicos/empregador/consultarloteeventos/WsConsultarLoteEventos.svc}")
    private String endpointConsulta;

    public ConsultaLoteService(SSLConfig sslConfig) {
        this.sslConfig = sslConfig;
    }

    public String consultarLote(String protocolo) throws Exception {
        // 1️⃣ Recupera o contexto SSL configurado no SSLConfig
        SSLContext sslContext = sslConfig.sslContext();

        // 2️⃣ Cria um novo cliente HTTPS com o contexto SSL
        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslContext)
                .build()) {

            // 3️⃣ Monta o XML de consulta
            String xml = """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <eSocial xmlns="http://www.esocial.gov.br/schema/lote/eventos/envio/v1_1_1">
                      <consultaLoteEventos>
                        <protocoloEnvio>%s</protocoloEnvio>
                      </consultaLoteEventos>
                    </eSocial>
                    """.formatted(protocolo);

            HttpPost post = new HttpPost(endpointConsulta);
            post.setHeader("Content-Type", "application/xml; charset=UTF-8");
            post.setEntity(new StringEntity(xml, StandardCharsets.UTF_8));

            return httpClient.execute(post, response ->
                    new String(response.getEntity().getContent().readAllBytes(), StandardCharsets.UTF_8)
            );
        }
    }
}
