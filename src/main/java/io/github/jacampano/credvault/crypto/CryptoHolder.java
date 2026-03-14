package io.github.jacampano.credvault.crypto;

import org.springframework.stereotype.Component;

@Component
public class CryptoHolder {

    private static TextCrypto textCrypto;

    public CryptoHolder(TextCrypto textCrypto) {
        CryptoHolder.textCrypto = textCrypto;
    }

    public static TextCrypto getTextCrypto() {
        if (textCrypto == null) {
            throw new IllegalStateException("Servicio de cifrado no inicializado");
        }
        return textCrypto;
    }
}
