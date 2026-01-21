package examples;

import com.github.imcamilo.fernet.Fernet;
import com.github.imcamilo.fernet.Key;
import com.github.imcamilo.fernet.Result;

/**
 * Example of using Fernet4s from Java with functional Result API
 */
public class JavaExample {

    public static void main(String[] args) {
        // Example 1: Basic encryption/decryption
        System.out.println("=== Example 1: Basic Usage from Java ===");
        Key key = Fernet.generateKey();
        String message = "Hello from Java!";

        Result<String> encrypted = Fernet.encryptResult(message, key);
        if (encrypted.isSuccess()) {
            String token = encrypted.get();
            System.out.println("Encrypted: " + token);

            Result<String> decrypted = Fernet.decryptResult(token, key);
            if (decrypted.isSuccess()) {
                System.out.println("Decrypted: " + decrypted.get());
            } else {
                System.out.println("Decryption failed: " + decrypted.getError());
            }
        } else {
            System.out.println("Encryption failed: " + encrypted.getError());
        }

        // Example 2: Key serialization
        System.out.println("\n=== Example 2: Key Serialization ===");
        Key key2 = Fernet.generateKey();
        String keyString = Fernet.keyToString(key2);
        System.out.println("Key as Base64: " + keyString);

        Result<Key> imported = Fernet.keyFromStringResult(keyString);
        if (imported.isSuccess()) {
            System.out.println("Key successfully imported!");

            // Use imported key
            Result<String> testEncrypt = Fernet.encryptResult("Test message", imported.get());
            if (testEncrypt.isSuccess()) {
                System.out.println("Encrypted with imported key: " + testEncrypt.get());
            }
        } else {
            System.out.println("Import failed: " + imported.getError());
        }

        // Example 3: Binary data
        System.out.println("\n=== Example 3: Binary Data ===");
        Key key3 = Fernet.generateKey();
        byte[] binaryData = new byte[]{1, 2, 3, 4, 5};

        Result<String> encryptedBinary = Fernet.encryptBytesResult(binaryData, key3);
        if (encryptedBinary.isSuccess()) {
            String token = encryptedBinary.get();
            Result<byte[]> decryptedBinary = Fernet.decryptBytesResult(token, key3);
            if (decryptedBinary.isSuccess()) {
                byte[] data = decryptedBinary.get();
                System.out.print("Binary data: ");
                for (byte b : data) {
                    System.out.print(b + " ");
                }
                System.out.println();
            }
        }

        // Example 4: TTL (Time-To-Live)
        System.out.println("\n=== Example 4: TTL Support ===");
        Key key4 = Fernet.generateKey();
        Result<String> tokenWithTTL = Fernet.encryptResult("Temporary data", key4);

        if (tokenWithTTL.isSuccess()) {
            String token = tokenWithTTL.get();

            // Decrypt with 60 seconds TTL
            Result<String> decrypted = Fernet.decryptResult(token, key4, 60);
            if (decrypted.isSuccess()) {
                System.out.println("Valid token (within TTL): " + decrypted.get());
            } else {
                System.out.println("Token validation failed: " + decrypted.getError());
            }

            // Try with very short TTL (will fail)
            Result<String> expiredDecrypt = Fernet.decryptResult(token, key4, 0);
            if (!expiredDecrypt.isSuccess()) {
                System.out.println("Expected failure with TTL=0: " + expiredDecrypt.getError());
            }
        }

        // Example 5: Token verification
        System.out.println("\n=== Example 5: Token Verification ===");
        Key key5 = Fernet.generateKey();
        Result<String> token5 = Fernet.encryptResult("Verify me!", key5);

        if (token5.isSuccess()) {
            Result<Boolean> isValid = Fernet.verifyResult(token5.get(), key5);
            if (isValid.isSuccess()) {
                System.out.println("Token is valid: " + isValid.get());
            }

            // Verify with wrong key
            Key wrongKey = Fernet.generateKey();
            Result<Boolean> isInvalid = Fernet.verifyResult(token5.get(), wrongKey);
            if (!isInvalid.isSuccess()) {
                System.out.println("Verification error: " + isInvalid.getError());
            }
        }

        // Example 6: Error handling pattern
        System.out.println("\n=== Example 6: Error Handling Pattern ===");
        Key key6 = Fernet.generateKey();
        String invalidToken = "invalid-token-format";

        Result<String> result = Fernet.decryptResult(invalidToken, key6);
        if (result.isSuccess()) {
            System.out.println("Decrypted: " + result.get());
        } else {
            // Functional error handling
            System.err.println("ERROR: " + result.getError());
            System.out.println("Recovered gracefully without exceptions!");
        }

        // Example 7: Using getOrElse for defaults
        System.out.println("\n=== Example 7: Using getOrElse ===");
        Key key7 = Fernet.generateKey();

        Result<String> maybeToken = Fernet.encryptResult("Original message", key7);
        String token = maybeToken.getOrElse("default-token");
        System.out.println("Token: " + token);

        Result<String> maybeData = Fernet.decryptResult(token, key7);
        String plaintext = maybeData.getOrElse("default value");
        System.out.println("Plaintext: " + plaintext);

        // Example 8: Chaining with map
        System.out.println("\n=== Example 8: Functional Chaining ===");
        Key key8 = Fernet.generateKey();

        Result<String> finalResult = Fernet.encryptResult("test data", key8)
            .flatMap(token -> Fernet.decryptResult(token, key8))
            .map(String::toUpperCase);

        if (finalResult.isSuccess()) {
            System.out.println("Final result: " + finalResult.get());
        }
    }
}
