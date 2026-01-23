# fernet4s

[![Maven Central](https://img.shields.io/maven-central/v/io.github.imcamilo/fernet4s_3.svg?label=maven%20central)](https://central.sonatype.com/artifact/io.github.imcamilo/fernet4s_3)
[![Maven Central Downloads](https://img.shields.io/maven-central/dt/io.github.imcamilo/fernet4s_3.svg?label=downloads)](https://central.sonatype.com/artifact/io.github.imcamilo/fernet4s_3)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](https://opensource.org/licenses/MIT)
[![Scala Versions](https://img.shields.io/badge/scala-2.13%20%7C%203.3-red.svg)](https://www.scala-lang.org)
[![CI](https://github.com/imcamilo/fernet4s/workflows/CI/badge.svg)](https://github.com/imcamilo/fernet4s/actions)

> Symmetric encryption that makes sure your data cannot be manipulated or read without the key.

**fernet4s** is a Scala implementation of the [Fernet specification](https://github.com/fernet/spec/blob/master/Spec.md), providing secure, authenticated encryption with built-in TTL (time-to-live) support. It's fully compatible with Fernet implementations in Python, Ruby, Go, and other languages.

## Features

- ✅ **100% spec compliant** - Fully validated against the [official Fernet specification](https://github.com/fernet/spec)
- ✅ **Type-safe API** - Uses Either/Result types instead of exceptions
- ✅ **Zero dependencies** - Only uses JVM standard library + SLF4J for logging
- ✅ **Cross-language compatible** - Tokens work with Python, Ruby, Go, and other Fernet implementations
- ✅ **AES-128-CBC encryption** - Strong symmetric encryption
- ✅ **HMAC-SHA256 authentication** - Ensures integrity and authenticity
- ✅ **TTL support** - Time-limited tokens for security (configurable time-to-live)
- ✅ **Key rotation** - Seamless key migration with MultiFernet
- ✅ **Java/Kotlin friendly** - Result API for Java interoperability
- ✅ **Scala 3 extensions** - Fluent syntax for idiomatic Scala code
- ✅ **Functional** - No uncontrolled exceptions, pure functions
- ✅ **Cross-compiled** - Scala 2.13 and Scala 3.3 support

## Adding this to your project

This library is available in [Maven Central](https://central.sonatype.com/artifact/io.github.imcamilo/fernet4s_3).

**Requirements:** Java 11 or higher

### Scala (sbt)

```scala
// Scala 3
libraryDependencies += "io.github.imcamilo" %% "fernet4s" % "1.0.0"

// Scala 2.13
libraryDependencies += "io.github.imcamilo" %% "fernet4s" % "1.0.0"
```

### Java/Kotlin (Gradle)

```gradle
dependencies {
    implementation 'io.github.imcamilo:fernet4s_3:1.0.0'
    implementation 'org.scala-lang:scala3-library_3:3.3.3'
}
```

### Java/Kotlin (Maven)

```xml
<dependency>
    <groupId>io.github.imcamilo</groupId>
    <artifactId>fernet4s_3</artifactId>
    <version>1.0.0</version>
</dependency>
<dependency>
    <groupId>org.scala-lang</groupId>
    <artifactId>scala3-library_3</artifactId>
    <version>3.3.3</version>
</dependency>
```

For more details, see [Maven Central Repository](https://central.sonatype.com/artifact/io.github.imcamilo/fernet4s_3).

Alternatively, you can download the latest JAR from [Maven Central](https://repo1.maven.org/maven2/io/github/imcamilo/fernet4s_3/1.0.0/) and add it to your classpath.

## Quick Start

### Scala 3

```scala
import com.github.imcamilo.fernet.Fernet

// Generate a key
val key = Fernet.generateKey()

// Encrypt
val result = for
  token <- Fernet.encrypt("Hello, Fernet!", key)
  plain <- Fernet.decrypt(token, key)
yield plain

result match
  case Right(text) => println(s"Decrypted: $text")
  case Left(error) => println(s"Error: $error")
```

### Scala 3 with Syntax Extensions

```scala
import com.github.imcamilo.fernet.Fernet
import com.github.imcamilo.fernet.Fernet.syntax.*

val key = Fernet.generateKey()
val token = key.encrypt("Secret message").getOrElse("")
val plain = key.decrypt(token).getOrElse("Error")
```

### Java

```java
import com.github.imcamilo.fernet.Fernet;
import com.github.imcamilo.fernet.Key;
import com.github.imcamilo.fernet.Result;

// Generate a key
Key key = Fernet.generateKey();

// Encrypt and decrypt
Result<String> result = Fernet.encryptResult("Hello, Fernet!", key)
    .flatMap(token -> Fernet.decryptResult(token, key));

if (result.isSuccess()) {
    System.out.println("Decrypted: " + result.get());
}
```

### Key Persistence

**Save and reload keys securely:**

```scala
// Scala - Generate and save
val key = Fernet.generateKey()
val keyString = Fernet.keyToString(key)
// Save to .env, secrets manager, etc.
// FERNET_KEY=OM4BvY-hz2MwEfcC1OMUibxwGfVXMFGJQETfIgHb23Y=

// Later, load from environment
val loadedKey = Fernet.keyFromString(sys.env("FERNET_KEY"))
loadedKey match
  case Right(key) => // Use key
  case Left(error) => // Handle error
```

```java
// Java - Generate and save
Key key = Fernet.generateKey();
String keyString = Fernet.keyToString(key);
// Save to environment or secrets manager
// FERNET_KEY=OM4BvY-hz2MwEfcC1OMUibxwGfVXMFGJQETfIgHb23Y=

// Later, load from environment
String envKey = System.getenv("FERNET_KEY");
Result<Key> loadedKey = Fernet.keyFromString(envKey);
if (loadedKey.isSuccess()) {
    Key key = loadedKey.get();
    // Use key
}
```

## Common Use Cases

### Create a new key

```scala
val key = Fernet.generateKey()
```

```java
Key key = Fernet.generateKey();
```

### Deserialize an existing key

```scala
val key = Fernet.keyFromString("cw_0x689RpI-jtRR7oE8h_eQsKImvJapLeSbXpwF4e4=")
```

```java
Result<Key> key = Fernet.keyFromString("cw_0x689RpI-jtRR7oE8h_eQsKImvJapLeSbXpwF4e4=");
```

### Create a token

```scala
val token = Fernet.encrypt("secret message", key)
```

```java
Result<String> token = Fernet.encryptResult("secret message", key);
```

### Deserialize and validate a token

```scala
val token = Token.fromString("gAAAAAAdwJ6w...")
val plaintext = Fernet.decrypt(token.toOption.get, key)
```

```java
Result<String> plaintext = Fernet.decryptResult("gAAAAAAdwJ6w...", key);
```

### Validate with custom TTL

```scala
// Token valid for 4 hours
val plaintext = Fernet.decrypt(token, key, Some(Duration.ofHours(4).toSeconds))
```

```java
// Token valid for 4 hours
Result<String> plaintext = Fernet.decryptResult(token, key, 4 * 3600L);
```

## Complete Examples

### Scala 3 - 9 Examples

```scala
import com.github.imcamilo.fernet.Fernet
import com.github.imcamilo.fernet.Fernet.syntax.*

@main
def main(): Unit =

  // Example 1: Basic encryption/decryption
  println("=== Example 1: Encrypt and Decrypt ===")
  val key = Fernet.generateKey()

  val result = for
    token <- Fernet.encrypt("Hello Fernet from Scala 3!", key)
    plain <- Fernet.decrypt(token, key)
  yield plain

  result match
    case Right(text) => println(s"✓ Decrypted: $text")
    case Left(error) => println(s"✗ Error: $error")

  // Example 2: Syntax extensions
  println("\n=== Example 2: Syntax Extensions ===")
  val token = key.encrypt("Secret message").getOrElse("")
  println(s"Token: $token")

  val decrypted = key.decrypt(token).getOrElse("Error")
  println(s"Decrypted: $decrypted")

  // Example 3: Key serialization
  println("\n=== Example 3: Key Serialization ===")
  val keyString = key.toBase64
  println(s"Key as Base64: $keyString")

  keyString.asFernetKey match
    case Right(importedKey) =>
      println("✓ Key imported successfully")
      importedKey.encrypt("Test with imported key") match
        case Right(t) => println(s"Token generated: ${t.take(20)}...")
        case Left(e) => println(s"Error: $e")
    case Left(error) =>
      println(s"✗ Import error: $error")

  // Example 4: TTL (Time-To-Live)
  println("\n=== Example 4: TTL (60 seconds) ===")
  val key4 = Fernet.generateKey()

  for
    token <- key4.encrypt("Temporary data")
    // Valid for 60 seconds
    plain <- key4.decrypt(token, Some(60))
  yield println(s"✓ Token valid: $plain")

  // Example 5: Binary data
  println("\n=== Example 5: Binary Data ===")
  val binaryData = Array[Byte](1, 2, 3, 4, 5)

  for
    token <- Fernet.encryptBytes(binaryData, key)
    bytes <- Fernet.decryptBytes(token, key)
  yield println(s"✓ Bytes recovered: ${bytes.mkString(", ")}")

  // Example 6: Verify token without decrypting
  println("\n=== Example 6: Verification ===")
  val tokenToVerify = key.encrypt("Verify me!").toOption.get

  Fernet.verify(tokenToVerify, key) match
    case Right(true) => println("✓ Token valid")
    case Left(error) => println(s"✗ Token invalid: $error")
    case _ => println("✗ Verification failed")

  // Example 7: Error handling
  println("\n=== Example 7: Error Handling ===")
  val invalidToken = "invalid-token"

  key.decrypt(invalidToken) match
    case Right(plain) => println(s"Decrypted: $plain")
    case Left(error) => println(s"✗ Expected error: $error")

  // Example 8: Key rotation
  println("\n=== Example 8: Key Rotation ===")
  val oldKey = Fernet.generateKey()
  val newKey = Fernet.generateKey()

  // Encrypt with old key
  val oldToken = oldKey.encrypt("Important data").toOption.get

  // MultiFernet can decrypt with any key
  import com.github.imcamilo.fernet.MultiFernet
  val multi = new MultiFernet(List(newKey, oldKey))

  multi.decrypt(oldToken) match
    case Right(data) =>
      println(s"✓ Decrypted with MultiFernet: $data")
      // Rotate: re-encrypt with the new key
      multi.rotate(oldToken) match
        case Right(newToken) => println("✓ Token rotated to new key")
        case Left(e) => println(s"Error: $e")
    case Left(error) => println(s"Error: $error")

  // Example 9: Result API
  println("\n=== Example 9: Result API ===")
  import com.github.imcamilo.fernet.Result

  val encResult: Result[String] = Fernet.encryptResult("Data", key)
  if encResult.isSuccess then
    println(s"✓ Encrypted: ${encResult.get.take(20)}...")

    // Functional chaining
    val finalResult = encResult
      .flatMap(token => Fernet.decryptResult(token, key))
      .map(_.toUpperCase)

    println(s"✓ Final: ${finalResult.getOrElse("error")}")
```

**Output:**
```
=== Example 1: Encrypt and Decrypt ===
✓ Decrypted: Hello Fernet from Scala 3!

=== Example 2: Syntax Extensions ===
Token: gAAAAABpcWL0wSKjKRY2YL8ALpB-ZTbESp5V3CwcThuL-piwCpBqS9kTkWrrGUrhclkXmswH3P8ybu5IjqhwySW9cMEeDqvYBA==
Decrypted: Secret message

=== Example 3: Key Serialization ===
Key as Base64: OM4BvY-hz2MwEfcC1OMUibxwGfVXMFGJQETfIgHb23Y=
✓ Key imported successfully
Token generated: gAAAAABpcWL04QpcMlUL...

=== Example 4: TTL (60 seconds) ===
✓ Token valid: Temporary data

=== Example 5: Binary Data ===
✓ Bytes recovered: 1, 2, 3, 4, 5

=== Example 6: Verification ===
✓ Token valid

=== Example 7: Error Handling ===
✗ Expected error: Invalid token format

=== Example 8: Key Rotation ===
✓ Decrypted with MultiFernet: Important data
✓ Token rotated to new key

=== Example 9: Result API ===
✓ Encrypted: gAAAAABpcWL0JNusiZdU...
✓ Final: DATA
```

### Java - 8 Examples

```java
import com.github.imcamilo.fernet.Fernet;
import com.github.imcamilo.fernet.Key;
import com.github.imcamilo.fernet.MultiFernet;
import com.github.imcamilo.fernet.Result;

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

        Result<String> encryptResult = Fernet.encryptResult("Hello Fernet from Java!", key);
        Result<String> decryptResult = encryptResult.flatMap(token ->
                Fernet.decryptResult(token, key)
        );

        String decrypted = decryptResult.getOrElseValue("Error");
        System.out.println("  Original: Hello Fernet from Java!");
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
        byte[] data = new byte[] {1, 2, 3, 4, 5};

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
        // First key is primary, then it tries oldKey
        MultiFernet multiFernet = MultiFernet.create(newKey, oldKey);

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
            if (i < bytes.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
}
```

**Output:**
```
=== Fernet4s Java Examples ===

1. Basic Encryption/Decryption:
  Original: Hello Fernet from Java!
  Decrypted: Hello Fernet from Java!

2. Error Handling (Invalid Key):
  Decryption failed as expected: Decryption failed

3. Byte Array Encryption:
  Original bytes: [1, 2, 3, 4, 5]
  Decrypted: [1, 2, 3, 4, 5]

4. MultiFernet (Key Rotation):
  Encrypted with old key
  Decrypted with MultiFernet: Data for rotation

5. TTL Validation:
  Encrypted: gAAAAABpcWJ9S5U57Y1nxHKbSSLCwTbPutdD58_lhz_V21XuKgJG2Acui_d-XuWo30Twl17fJGPughH_vwsuBEPB6mxoAU2AwcwBRltNDW8_-MR-fd_Lg5Y=
  Decrypted with 1h TTL: Time-sensitive data

6. Result API (Java-friendly):
  Result API success: Hello from Result API

7. Functional Composition with Result:
  Composed result: FUNCTIONAL JAVA!

8. Key Generation:
  Generated key 1: eePqMake_aUhz6m2goofw8SGypu7VS9bDmn0fHCRnPg=
  Generated key 2: H8JL1YCdzGeeSjQQ3q2bQNctzHrBqF5Lx971BQlkpg8=
  Keys are different: true
```

## API Overview

### Core Operations

#### Scala
```scala
// Generate key
val key = Fernet.generateKey()

// Encrypt/decrypt
val encrypted: Either[String, String] = Fernet.encrypt("data", key)
val decrypted: Either[String, String] = Fernet.decrypt(token, key)

// With TTL (60 seconds)
val result: Either[String, String] = Fernet.decrypt(token, key, Some(60))

// Binary data
val encBytes: Either[String, String] = Fernet.encryptBytes(bytes, key)
val decBytes: Either[String, Array[Byte]] = Fernet.decryptBytes(token, key)

// Verify without decrypting
val valid: Either[String, Boolean] = Fernet.verify(token, key)
```

#### Java
```java
// Generate key
Key key = Fernet.generateKey();

// Encrypt/decrypt
Result<String> encrypted = Fernet.encryptResult("data", key);
Result<String> decrypted = Fernet.decryptResult(token, key);

// With TTL (60 seconds)
Result<String> result = Fernet.decryptResult(token, key, 60L);

// Binary data
Result<String> encBytes = Fernet.encryptBytesResult(bytes, key);
Result<byte[]> decBytes = Fernet.decryptBytesResult(token, key);
```

### Key Management

```scala
// Save key
val keyString: String = Fernet.keyToString(key)

// Load key
val key: Either[String, Key] = Fernet.keyFromString(keyString)
```

### MultiFernet (Key Rotation)

```scala
import com.github.imcamilo.fernet.MultiFernet

val oldKey = Fernet.generateKey()
val newKey = Fernet.generateKey()

val multi = MultiFernet(newKey, oldKey) // First key is primary

// Encrypts with newKey
val encrypted = multi.encrypt("data")

// Tries each key in order
val decrypted = multi.decrypt(token)

// Rotate: re-encrypt with primary key
val rotated = multi.rotate(oldToken)
```

## Security

- **Confidentiality**: AES-128-CBC encryption
- **Integrity**: HMAC-SHA256 authentication
- **Authenticity**: Only holders of the key can create valid tokens
- **Freshness**: Optional TTL prevents replay attacks

### Best Practices

1. **Generate keys securely**: Use `Fernet.generateKey()`
2. **Store keys safely**: Environment variables, secret managers, etc.
3. **Never hardcode keys**: In source code or version control
4. **Rotate keys**: Use MultiFernet for gradual migration
5. **Use TTL**: For time-sensitive data (sessions, reset tokens)

## Storing Sensitive Data

Fernet is ideal for securely storing sensitive data on the client side (e.g., browser cookies, local storage) or transmitting data between services.

**Example: Secure Session Token**

```scala
// Server side - Create session token
val sessionData = s"""{"userId": "$userId", "role": "$role"}"""
val key = Fernet.keyFromString(sys.env("SESSION_KEY")).toOption.get
val token = Fernet.encrypt(sessionData, key, Some(3600)) // 1 hour TTL

// Client stores token (cookie, localStorage, etc.)

// Later - Validate and extract session
Fernet.decrypt(token, key, Some(3600)) match
  case Right(data) =>
    // Parse JSON and restore session
  case Left(error) =>
    // Token expired or invalid
```

**Example: Encrypted API Key**

```java
// Store encrypted API key
String apiKey = "sk_live_abc123...";
Key fernetKey = Fernet.generateKey();
Result<String> encrypted = Fernet.encryptResult(apiKey, fernetKey);

// Save encrypted token to database
database.save(userId, encrypted.get());

// Later, decrypt when needed
Result<String> decrypted = Fernet.decryptResult(encryptedToken, fernetKey);
String apiKey = decrypted.get();
```

## Use Cases

- 🔐 **Session tokens** - Short-lived authentication tokens
- 🔑 **API keys** - Encrypted API credentials
- 📧 **Password reset** - Time-limited reset tokens
- 💾 **Database encryption** - Sensitive configuration data
- 🔄 **Service-to-service** - Encrypted messages between microservices
- 📁 **File encryption** - Secure file storage
- 🍪 **Secure cookies** - Tamper-proof client-side data

## Cross-Language Compatibility

fernet4s is fully compatible with Fernet implementations in other languages:

```python
# Python (cryptography library)
from cryptography.fernet import Fernet

key = b'OM4BvY-hz2MwEfcC1OMUibxwGfVXMFGJQETfIgHb23Y='
f = Fernet(key)
token = f.encrypt(b"Hello from Python")

# This token can be decrypted by fernet4s!
```

```scala
// Scala
val key = Fernet.keyFromString("OM4BvY-hz2MwEfcC1OMUibxwGfVXMFGJQETfIgHb23Y=")
val decrypted = Fernet.decrypt(pythonToken, key.toOption.get)
```

## Specification Compliance

✅ **100% compliant** with the [official Fernet specification](https://github.com/fernet/spec)

All test vectors from the official spec repository pass:
- ✅ **generate.json** - Token generation with known parameters
- ✅ **verify.json** - Valid token verification
- ✅ **invalid.json** - 8 invalid token cases (incorrect MAC, too short, invalid base64, padding errors, expired TTL, clock skew, etc.)

## Testing

```bash
# Run all tests
sbt test

# Test both Scala versions
sbt +test
```

**73 tests** covering:
- Encryption/decryption
- Key generation and serialization
- TTL validation
- MultiFernet key rotation
- Cross-language compatibility
- Error handling
- Edge cases
- **Official Fernet spec test vectors**

## Dependencies

- **Scala standard library**: 2.13.14 or 3.3.3
- **SLF4J**: 2.0.12 (logging interface)

No other dependencies. Uses JVM built-in crypto (`javax.crypto`).

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

**Development:**
```bash
# Compile
sbt compile

# Run tests
sbt test

# Cross-compile for Scala 2.13 and 3.3
sbt +test

# Run examples
cd examples/scala3 && scala CompleteExample.scala
cd examples/java && javac Main.java && java Main
```

See the [examples](examples/) directory for complete working examples.

## Prior Art

- [fernet-java](https://github.com/l0s/fernet-java8) - Java 8 implementation by Carlos Macasaet
- [cryptography](https://cryptography.io/en/latest/fernet/) - Official Python implementation

## License

MIT License

Copyright (c) 2026 Camilo Jorquera

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

## Resources

- [Fernet Specification](https://github.com/fernet/spec/blob/master/Spec.md)
- [Python cryptography library](https://cryptography.io/en/latest/fernet/)
- [Scala 3 Documentation](https://docs.scala-lang.org/scala3/)
- [Maven Central](https://central.sonatype.com/artifact/io.github.imcamilo/fernet4s_3)

---

Made with ❤️ by [@imcamilo](https://github.com/imcamilo)
