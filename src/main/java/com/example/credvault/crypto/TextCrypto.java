package com.example.credvault.crypto;

public interface TextCrypto {
    String encrypt(String plaintext);

    String decrypt(String ciphertext);
}
