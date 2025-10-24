package br.gov.hemobras.esocial.controller;

import br.gov.hemobras.esocial.service.AssinadorXml;
import br.gov.hemobras.esocial.service.ConsultaLoteService;
import br.gov.hemobras.esocial.service.EsocialService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class EsocialController {

       private final EsocialService esocialService;
    private final AssinadorXml assinadorXml;
    private final ConsultaLoteService consultaLoteService; // 👈 novo

    public EsocialController(EsocialService esocialService, AssinadorXml assinadorXml, ConsultaLoteService consultaLoteService) {
        this.esocialService = esocialService;
        this.assinadorXml = assinadorXml;
        this.consultaLoteService = consultaLoteService;
    }

    /** Verifica se o backend está ativo **/
    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of("ok", true);
    }

    /** Envia um XML de lote já assinado ao eSocial **/
    @PostMapping(value = "/enviar-lote", consumes = {"application/xml", "text/xml"})
    public ResponseEntity<String> enviarLote(@RequestBody String xml) throws Exception {
        String resp = esocialService.enviarLote(xml);
        return ResponseEntity.ok(resp);
    }

    /** Assina apenas o XML do evento (ex: S-1000) e retorna o evento assinado **/
    @PostMapping(value = "/assinar", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<String> assinar(@RequestBody String xmlEvento) throws Exception {
        String xmlAssinado = assinadorXml.assinarEventoPorIdAuto(xmlEvento);
        return ResponseEntity.ok(xmlAssinado);
    }

    /** Assina o evento e envia o lote automaticamente para o eSocial **/
    @PostMapping(value = "/assinar-e-enviar", consumes = {MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_XML_VALUE})
    public ResponseEntity<String> assinarEEnviar(@RequestBody String xmlEvento) throws Exception {
        // 1️⃣ Assina o evento
        String xmlAssinado = assinadorXml.assinarEventoPorIdAuto(xmlEvento);

        // 2️⃣ Monta o XML do lote (estrutura mínima)
        String lote = """
                <?xml version="1.0" encoding="UTF-8"?>
                <eSocial xmlns="http://www.esocial.gov.br/schema/lote/eventos/envio/v1_1_1">
                  <envioLoteEventos grupo="1">
                    <ideEmpregador>
                      <tpInsc>1</tpInsc>
                      <nrInsc>07607851000146</nrInsc>
                    </ideEmpregador>
                    <ideTransmissor>
                      <tpInsc>1</tpInsc>
                      <nrInsc>07607851000146</nrInsc>
                    </ideTransmissor>
                    <eventos>
                      %s
                    </eventos>
                  </envioLoteEventos>
                </eSocial>
                """.formatted(xmlAssinado);

        // 3️⃣ Envia ao eSocial e retorna a resposta
        String resposta = esocialService.enviarLote(lote);
        return ResponseEntity.ok(resposta);
    }
    
     @GetMapping("/consultar-lote")
    public ResponseEntity<String> consultarLote(@RequestParam String protocolo) throws Exception {
        String retorno = consultaLoteService.consultarLote(protocolo);
        return ResponseEntity.ok(retorno);
    }
}
