package com.github.imcamilo.examples;

import com.github.imcamilo.fernet.Fernet;
import com.github.imcamilo.fernet.Key;
import com.github.imcamilo.fernet.MultiFernet;
import com.github.imcamilo.fernet.Result;
import scala.collection.immutable.List;
import scala.collection.immutable.List$;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Fernet4s Java Examples ===\n");

        example1BasicEncryption();
        example2ErrorHandling();
        example3ByteArrays();
        example4MultiFernet();
        example5TTL();
        example6ResultAPI();
        example7FunctionalComposition();
        example8KeyGeneration();
    }

    private static void example1BasicEncryption() {
        System.out.println("1. Basic Encryption/Decryption:");
        Key key = Fernet.generateKey();

        Result<String> encryptResult = Fernet.encryptResult("Hola Fernet desde Java!", key);
        Result<String> decryptResult = encryptResult.flatMap(token ->
            Fernet.decryptResult(token, key)
        );

        String decrypted = decryptResult.getOrElseValue("Error");
        System.out.println("  Original: Hola Fernet desde Java!");
        System.out.println("  Decrypted: " + decrypted);
        System.out.println();
    }

    private static void example2ErrorHandling() {
        System.out.println("2. Error Handling (Invalid Key):");
        Key key = Fernet.generateKey();
        String encrypted = Fernet.encryptResult("Secret message", key).getOrElseValue("");

        Key wrongKey = Fernet.generateKey();
        Result<String> result = Fernet.decryptResult(encrypted, wrongKey);

        if (result.isFailure()) {
            System.out.println("  Decryption failed as expected: " + result.getError());
        }
        System.out.println();
    }

    private static void example3ByteArrays() {
        System.out.println("3. Byte Array Encryption:");
        Key key = Fernet.generateKey();
        byte[] data = new byte[]{1, 2, 3, 4, 5};

        Result<String> encryptResult = Fernet.encryptBytesResult(data, key);
        Result<byte[]> decryptResult = encryptResult.flatMap(token ->
            Fernet.decryptBytesResult(token, key)
        );

        byte[] decrypted = decryptResult.getOrElseValue(new byte[0]);
        System.out.println("  Original bytes: [1, 2, 3, 4, 5]");
        System.out.println("  Decrypted: " + bytesToString(decrypted));
        System.out.println();
    }

    private static void example4MultiFernet() {
        System.out.println("4. MultiFernet (Key Rotation):");
        Key oldKey = Fernet.generateKey();
        Key newKey = Fernet.generateKey();

        // Encrypt with old key
        String encrypted = Fernet.encryptResult("Data for rotation", oldKey).getOrElseValue("");

        // Decrypt with MultiFernet (tries both keys)
        List<Key> keys = List$.MODULE$.<Key>empty().$colon$colon(newKey).$colon$colon(oldKey);
        MultiFernet multiFernet = new MultiFernet(keys);

        Result<String> decryptResult = multiFernet.decryptResult(encrypted);
        String decrypted = decryptResult.getOrElseValue("Error");

        System.out.println("  Encrypted with old key");
        System.out.println("  Decrypted with MultiFernet: " + decrypted);
        System.out.println();
    }

    private static void example5TTL() {
        System.out.println("5. TTL Validation:");
        Key key = Fernet.generateKey();
        String encrypted = Fernet.encryptResult("Time-sensitive data", key).getOrElseValue("");

        // Decrypt with 1 hour TTL (3600 seconds)
        Result<String> decryptResult = Fernet.decryptResult(encrypted, key, 3600L);
        String decrypted = decryptResult.getOrElseValue("Error");

        System.out.println("  Encrypted: " + encrypted);
        System.out.println("  Decrypted with 1h TTL: " + decrypted);
        System.out.println();
    }

    private static void example6ResultAPI() {
        System.out.println("6. Result API (Java-friendly):");
        Key key = Fernet.generateKey();

        Result<String> result = Fernet.encryptResult("Hello from Result API", key)
            .flatMap(token -> Fernet.decryptResult(token, key));

        if (result.isSuccess()) {
            System.out.println("  Result API success: " + result.get());
        }
        System.out.println();
    }

    private static void example7FunctionalComposition() {
        System.out.println("7. Functional Composition with Result:");
        Key key = Fernet.generateKey();

        Result<String> result = Fernet.encryptResult("Functional Java!", key)
            .flatMap(token -> Fernet.decryptResult(token, key))
            .map(s -> s.toUpperCase());

        String finalResult = result.getOrElseValue("Error");
        System.out.println("  Composed result: " + finalResult);
        System.out.println();
    }

    private static void example8KeyGeneration() {
        System.out.println("8. Key Generation:");
        Key key1 = Fernet.generateKey();
        Key key2 = Fernet.generateKey();
        String key1Str = Fernet.keyToString(key1);
        String key2Str = Fernet.keyToString(key2);

        System.out.println("  Generated key 1: " + key1Str);
        System.out.println("  Generated key 2: " + key2Str);
        System.out.println("  Keys are different: " + (!key1Str.equals(key2Str)));
        System.out.println();
    }

    private static String bytesToString(byte[] bytes) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bytes.length; i++) {
            sb.append(bytes[i]);
            if (i < bytes.length - 1) sb.append(", ");
        }
        sb.append("]");
        return sb.toString();
    }
}
