package br.gov.hemobras.esocial.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.C14NMethodParameterSpec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * Assina um evento do eSocial com XMLDSig (enveloped).
 * - Carrega o .pfx (PKCS#12) a partir de application.yml (esocial.cert.path/password)
 * - Procura o elemento do evento que contenha atributo "Id" e o usa como referência
 * - Assina com RSA-SHA256 e C14N
 */
@Component
public class AssinadorXml {

    @Value("${esocial.cert.path}")
    private String certPath;

    @Value("${esocial.cert.password}")
    private String certPassword;

    /**
     * Assina o primeiro elemento do XML que contenha o atributo "Id".
     * Para eSocial, esse normalmente é o nó do evento (ex.: <evtInfoEmpregador Id="...">).
     *
     * @param xmlEvento XML do evento (sem assinatura)
     * @return XML do evento assinado (com <Signature>)
     */
    public String assinarEventoPorIdAuto(String xmlEvento) throws Exception {
        // 1) Parse seguro do XML
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new ByteArrayInputStream(xmlEvento.getBytes(StandardCharsets.UTF_8)));

        // 2) Localiza o elemento com atributo "Id"
        Element elementoAssinado = localizarElementoComId(doc.getDocumentElement());
        if (elementoAssinado == null) {
            throw new IllegalArgumentException("Não foi encontrado elemento com atributo 'Id' no XML.");
        }
        String id = elementoAssinado.getAttribute("Id");
        // Marca o atributo como do tipo ID para a engine de assinatura
        elementoAssinado.setIdAttribute("Id", true);

        // 3) Carrega a chave e o certificado do .pfx
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (java.io.FileInputStream fis = new java.io.FileInputStream(certPath)) {
            ks.load(fis, certPassword.toCharArray());
        }
        String alias = ks.aliases().nextElement();
        PrivateKey privateKey = (PrivateKey) ks.getKey(alias, certPassword.toCharArray());
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);

        // 4) Monta a assinatura XMLDSig (RSA-SHA256, enveloped)
        XMLSignatureFactory fac = XMLSignatureFactory.getInstance("DOM");

        // Referência ao elemento com URI = "#Id"
        Reference ref = fac.newReference(
                "#" + id,
                fac.newDigestMethod(DigestMethod.SHA256, null),
                java.util.List.of(
                        fac.newTransform(Transform.ENVELOPED, (javax.xml.crypto.dsig.spec.TransformParameterSpec) null),
                        fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null)
                ),
                null, // type
                null  // id
        );

        // SignedInfo
        SignedInfo si = fac.newSignedInfo(
                fac.newCanonicalizationMethod(CanonicalizationMethod.INCLUSIVE, (C14NMethodParameterSpec) null),
                fac.newSignatureMethod(SignatureMethod.RSA_SHA256, null),
                java.util.List.of(ref)
        );

        // KeyInfo com o certificado X509
        KeyInfoFactory kif = fac.getKeyInfoFactory();
        X509Data x509Data = kif.newX509Data(java.util.List.of(cert));
        KeyInfo ki = kif.newKeyInfo(java.util.List.of(x509Data));

        // 5) Contexto e assinatura
        DOMSignContext dsc = new DOMSignContext(privateKey, elementoAssinado);
        // Normalmente, a assinatura vai como último filho do nó assinado
        dsc.setNextSibling(elementoAssinado.getLastChild());

        XMLSignature signature = fac.newXMLSignature(si, ki);
        signature.sign(dsc);

        // 6) Serializa e devolve
        TransformerFactory tf = TransformerFactory.newInstance();
        tf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer trans = tf.newTransformer();
        trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        trans.setOutputProperty(OutputKeys.INDENT, "no");

        StringWriter sw = new StringWriter();
        trans.transform(new DOMSource(doc), new StreamResult(sw));
        return sw.toString();
    }

    /**
     * Desce recursivamente na árvore até achar o primeiro elemento com atributo "Id".
     */
    private Element localizarElementoComId(Element root) {
        if (root.hasAttribute("Id") && !root.getAttribute("Id").isBlank()) {
            return root;
        }
        var nl = root.getChildNodes();
        for (int i = 0; i < nl.getLength(); i++) {
            if (nl.item(i) instanceof Element el) {
                Element found = localizarElementoComId(el);
                if (found != null) return found;
            }
        }
        return null;
    }
}
