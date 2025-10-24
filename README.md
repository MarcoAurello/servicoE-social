# eSocial Java (Spring Boot)

Ambiente mínimo em Java para enviar XMLs do eSocial (Produção Restrita) via **mTLS** com **certificado A1 (.pfx)**.

## Como rodar
1. Instale o Java 17 e Maven.
2. Ajuste o caminho do certificado no `src/main/resources/application.yml`:
   ```yaml
   esocial:
     cert:
       path: "C:/certificados/empresa_a1.pfx"
       password: "SUA_SENHA"
   ```
3. Rode o app:
   ```bash
   mvn spring-boot:run
   ```
4. Teste:
   - Health: `http://localhost:8080/api/health` → `{"ok": true}`
   - Enviar (via Postman/curl): `POST http://localhost:8080/api/enviar-lote` (Content-Type: `text/xml`), corpo = XML assinado.

> Observação: este projeto **não assina** o XML; espera o **XML já assinado** (XMLDSig). A assinatura pode ser adicionada com `XMLSignatureFactory` (JCP) em um próximo passo.
