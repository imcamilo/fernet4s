# Fernet4s

![Scala](https://img.shields.io/badge/scala-2.13.14-red.svg)
![License](https://img.shields.io/badge/license-MIT-blue.svg)

A simple, elegant, and functional Scala library for symmetric encryption using [Fernet](https://github.com/fernet/spec/blob/master/Spec.md) specification.

## What is Fernet?

Fernet guarantees that a message encrypted using it cannot be manipulated or read without the key. It is built on top of:
- **AES 128** encryption in CBC mode
- **HMAC-SHA256** for authentication
- **Timestamp** for TTL (Time-To-Live) support

Fernet is ideal for encrypting data that needs to be stored or transmitted securely.

## Features

- Simple and intuitive functional API
- Type-safe operations with `Either` for error handling
- Support for TTL (time-to-live) validation
- Cats integration for functional programming
- Zero-dependency encryption (uses Java standard library)

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.imcamilo" %% "fernet4s" % "0.1.0"
```

## Quick Start

### Basic Usage

```scala
import com.github.imcamilo.fernet.Fernet

// Generate a new key
val key = Fernet.generateKey()

// Encrypt data
val encrypted: Either[String, String] = Fernet.encrypt("Hello, Fernet!", key)

// Decrypt data
val decrypted: Either[String, String] = encrypted.flatMap(token =>
  Fernet.decrypt(token, key)
)

println(decrypted) // Right("Hello, Fernet!")
```

### Using Syntax Extensions

For a more fluent API, use the syntax extensions:

```scala
import com.github.imcamilo.fernet.Fernet
import com.github.imcamilo.fernet.Fernet.syntax._

// Generate a new key
val key = Fernet.generateKey()

// Encrypt with fluent syntax
val encrypted: Either[String, String] = key.encrypt("Hello, Fernet!")

// Decrypt with fluent syntax
val decrypted: Either[String, String] = encrypted.flatMap(token =>
  key.decrypt(token)
)
```

### Working with Key Strings

```scala
import com.github.imcamilo.fernet.Fernet
import com.github.imcamilo.fernet.Fernet.syntax._

// Generate a key and export as Base64 string
val key = Fernet.generateKey()
val keyString: String = key.toBase64

// Later, import the key from string
val importedKey: Either[String, Key] = keyString.asFernetKey

// Use the imported key
importedKey match {
  case Right(key) =>
    val encrypted = key.encrypt("Secret message")
    println(encrypted)
  case Left(error) =>
    println(s"Error importing key: $error")
}
```

### Time-To-Live (TTL) Support

```scala
import com.github.imcamilo.fernet.Fernet

val key = Fernet.generateKey()

// Encrypt data
val token = Fernet.encrypt("Temporary data", key)

// Decrypt with 60 seconds TTL
val decrypted = token.flatMap(t =>
  Fernet.decrypt(t, key, ttlSeconds = Some(60))
)

// After 60 seconds, decryption will fail with "Token has expired"
```

### Encrypting Binary Data

```scala
import com.github.imcamilo.fernet.Fernet

val key = Fernet.generateKey()
val data: Array[Byte] = "Binary data".getBytes

// Encrypt bytes
val encrypted: Either[String, String] = Fernet.encryptBytes(data, key)

// Decrypt bytes
val decrypted: Either[String, Array[Byte]] = encrypted.flatMap(token =>
  Fernet.decryptBytes(token, key)
)
```

### Verify Token Without Decrypting

```scala
import com.github.imcamilo.fernet.Fernet

val key = Fernet.generateKey()
val token = Fernet.encrypt("Data", key)

// Verify token is valid without decrypting
val isValid: Either[String, Boolean] = token.flatMap(t =>
  Fernet.verify(t, key)
)

println(isValid) // Right(true)
```

## Advanced Usage

### Custom Key Creation

If you already have signing and encryption keys:

```scala
import com.github.imcamilo.fernet.Key
import com.github.imcamilo.fernet.Constants._

// Create keys (each must be 128 bits / 16 bytes)
val signingKey = Array.fill(signingKeyBytes)(0.toByte)
val encryptionKey = Array.fill(encryptionKeyBytes)(1.toByte)

// Create Key instance
val result = Key.creatingKeyInstance(signingKey, encryptionKey)

result match {
  case Success((sKey, eKey)) =>
    val key = new Key(sKey, eKey)
    println("Key created successfully")
  case Failure(exception) =>
    println(s"Error: ${exception.getMessage}")
}
```

### Custom Validators

For more control over validation, you can create custom validators:

```scala
import com.github.imcamilo.fernet.Token
import com.github.imcamilo.validators.StringValidator
import java.time.Duration

// Create a custom validator with 5-minute TTL
val validator = new StringValidator {
  override def getTimeToLive = Duration.ofMinutes(5)
}

// Use with Token directly
Token.fromString(tokenString).flatMap { token =>
  token.validateAndDecrypt(key, validator)
}
```

## Fernet Specification

This library implements the [Fernet specification](https://github.com/fernet/spec/blob/master/Spec.md):

### Token Format

```
Version (1 byte) || Timestamp (8 bytes) || IV (16 bytes) || Ciphertext (variable) || HMAC (32 bytes)
```

- **Version**: Always `0x80`
- **Timestamp**: Unix timestamp (seconds since epoch)
- **IV**: Initialization vector for AES
- **Ciphertext**: AES-128-CBC encrypted payload
- **HMAC**: HMAC-SHA256 signature of Version || Timestamp || IV || Ciphertext

### Security Properties

- **Confidentiality**: AES-128-CBC encryption
- **Authenticity**: HMAC-SHA256 signature
- **Freshness**: Timestamp with configurable TTL
- **Tamper-proof**: Any modification invalidates the token

## API Reference

### Fernet Object

| Method | Description |
|--------|-------------|
| `generateKey(): Key` | Generate a new random Fernet key |
| `keyFromString(keyString: String): Either[String, Key]` | Import key from Base64 string |
| `keyToString(key: Key): String` | Export key to Base64 string |
| `encrypt(plainText: String, key: Key): Either[String, String]` | Encrypt plaintext |
| `encryptBytes(payload: Array[Byte], key: Key): Either[String, String]` | Encrypt bytes |
| `decrypt(tokenString: String, key: Key, ttlSeconds: Option[Long]): Either[String, String]` | Decrypt token |
| `decryptBytes(tokenString: String, key: Key, ttlSeconds: Option[Long]): Either[String, Array[Byte]]` | Decrypt token to bytes |
| `verify(tokenString: String, key: Key, ttlSeconds: Option[Long]): Either[String, Boolean]` | Verify token validity |

### Syntax Extensions

```scala
import com.github.imcamilo.fernet.Fernet.syntax._

// Key operations
key.encrypt(plainText)
key.decrypt(token)
key.toBase64

// String operations
keyString.asFernetKey
```

## Error Handling

All operations return `Either[String, A]` where:
- `Left(error)` - Operation failed with error message
- `Right(value)` - Operation succeeded with value

```scala
Fernet.encrypt("data", key) match {
  case Right(token) => println(s"Encrypted: $token")
  case Left(error) => println(s"Encryption failed: $error")
}
```

## Testing

Run tests with:

```bash
sbt test
```

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'feat: add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Development Guidelines

- Write functional, immutable code
- Use `Either` for error handling
- Add tests for new features
- Follow Scala style guidelines
- Keep it simple and elegant

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## References

- [Fernet Specification](https://github.com/fernet/spec/blob/master/Spec.md)
- [Python Cryptography Fernet](https://cryptography.io/en/latest/fernet/)
- [Ruby Fernet](https://github.com/fernet/fernet-rb)

## Credits

Created by [imcamilo](https://github.com/imcamilo)

## Future Goals

- [ ] Use type classes for enhanced functional programming
- [ ] Support for Scala 3
- [ ] Multi-key encryption/decryption
- [ ] Streaming encryption for large files
- [ ] Cross-platform compatibility verification

---

Made with ❤️ using Scala
