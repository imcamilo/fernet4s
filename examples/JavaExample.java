package examples;

import com.github.imcamilo.fernet.Fernet;
import com.github.imcamilo.fernet.Key;
import scala.util.Either;

/**
 * Example of using Fernet4s from Java
 */
public class JavaExample {

    public static void main(String[] args) {
        // Example 1: Basic encryption/decryption
        System.out.println("=== Example 1: Basic Usage from Java ===");
        Key key = Fernet.generateKey();
        String message = "Hello from Java!";

        Either<String, String> encrypted = Fernet.encrypt(message, key);
        if (encrypted.isRight()) {
            String token = encrypted.right().get();
            System.out.println("Encrypted: " + token);

            Either<String, String> decrypted = Fernet.decrypt(token, key, scala.Option.empty());
            if (decrypted.isRight()) {
                System.out.println("Decrypted: " + decrypted.right().get());
            } else {
                System.out.println("Decryption failed: " + decrypted.left().get());
            }
        } else {
            System.out.println("Encryption failed: " + encrypted.left().get());
        }

        // Example 2: Key serialization
        System.out.println("\n=== Example 2: Key Serialization ===");
        Key key2 = Fernet.generateKey();
        String keyString = Fernet.keyToString(key2);
        System.out.println("Key as Base64: " + keyString);

        Either<String, Key> imported = Fernet.keyFromString(keyString);
        if (imported.isRight()) {
            System.out.println("Key successfully imported!");
        } else {
            System.out.println("Import failed: " + imported.left().get());
        }

        // Example 3: Binary data
        System.out.println("\n=== Example 3: Binary Data ===");
        Key key3 = Fernet.generateKey();
        byte[] binaryData = new byte[]{1, 2, 3, 4, 5};

        Either<String, String> encryptedBinary = Fernet.encryptBytes(binaryData, key3);
        if (encryptedBinary.isRight()) {
            String token = encryptedBinary.right().get();
            Either<String, byte[]> decryptedBinary = Fernet.decryptBytes(token, key3, scala.Option.empty());
            if (decryptedBinary.isRight()) {
                byte[] data = decryptedBinary.right().get();
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
        Either<String, String> tokenWithTTL = Fernet.encrypt("Temporary data", key4);

        if (tokenWithTTL.isRight()) {
            String token = tokenWithTTL.right().get();
            // Decrypt with 60 seconds TTL
            Either<String, String> decrypted = Fernet.decrypt(token, key4, scala.Option.apply(60L));
            if (decrypted.isRight()) {
                System.out.println("Valid token: " + decrypted.right().get());
            } else {
                System.out.println("Token validation failed: " + decrypted.left().get());
            }
        }
    }
}
