# fernet4s 🔐

> Simple and secure symmetric encryption for Scala

Fernet is like a safe for your data. You put something inside with a key, and only that key can open it later. Plus, the safe has a clock: if too much time passes, it can't be opened anymore (TTL).

## Why Fernet?

- ✅ **Simple**: One key for everything
- ✅ **Secure**: AES-128 + HMAC-SHA256
- ✅ **With timestamp**: Supports expiration (TTL)
- ✅ **Standard**: Compatible with Python, Ruby, Go, etc.
- ✅ **No surprises**: Detects any tampering

## Installation

```scala
libraryDependencies += "io.github.imcamilo" %% "fernet4s" % "0.1.0"
```

## Quick Start

### Encrypt and decrypt

```scala
import com.github.imcamilo.fernet.Fernet

// Generate a key
val key = Fernet.generateKey()

// Encrypt
val encrypted = Fernet.encrypt("Hello Fernet!", key)
// Right("gAAAAABh...")

// Decrypt
val decrypted = Fernet.decrypt(encrypted.right.get, key)
// Right("Hello Fernet!")
```

### With fluent syntax

```scala
import com.github.imcamilo.fernet.Fernet.syntax._

val key = Fernet.generateKey()

// More natural and chainable
val result = for {
  token <- key.encrypt("Secret message")
  plain <- key.decrypt(token)
} yield plain

println(result) // Right("Secret message")
```

### Save and load keys

```scala
// Convert key to text
val keyString = key.toBase64
// "wz5hami-yvr3zHyzVEiOYFvN9kTzXRW3dP7NcUr9Nvs="

// Save to environment variable, file, etc.

// Load later
val importedKey = keyString.asFernetKey
```

### Tokens with expiration (TTL)

```scala
val key = Fernet.generateKey()
val token = key.encrypt("Temporary data").right.get

// Only valid for 60 seconds
val decrypted = key.decrypt(token, ttlSeconds = Some(60))

// After 60 seconds → Left("Token has expired")
```

### From Java

```java
import com.github.imcamilo.fernet.Fernet;
import com.github.imcamilo.fernet.Key;
import scala.util.Either;

Key key = Fernet.generateKey();
Either<String, String> encrypted = Fernet.encrypt("Hello!", key);

if (encrypted.isRight()) {
    String token = encrypted.right().get();
    // ...
}
```

## Examples

Check out the [`examples/`](examples/) directory for more use cases.

## How it works

Fernet uses:
- **AES-128-CBC** for encryption
- **HMAC-SHA256** for signing
- **Timestamp** for TTL
- **Base64 URL** for the final token

The token has this format:
```
Version | Timestamp | IV | Ciphertext | HMAC
1 byte  | 8 bytes   | 16 | Variable   | 32 bytes
```

## Use cases

- 🔑 Session tokens
- 💾 Encrypt database data
- 🔐 API keys and secrets
- 📨 Secure messages between services
- 🎫 Verification tokens

## Tests

```bash
sbt test
```

## Compatibility

- ✅ Scala 2.13
- ✅ Java 8+
- ✅ Kotlin (via Java interop)
- 📦 Compatible with [Fernet spec](https://github.com/fernet/spec)

## API

| Method | Description |
|--------|-------------|
| `generateKey()` | Generate a random key |
| `keyToString(key)` | Export key as Base64 |
| `keyFromString(str)` | Import key from Base64 |
| `encrypt(text, key)` | Encrypt text |
| `decrypt(token, key)` | Decrypt token |
| `verify(token, key)` | Verify without decrypting |

### Syntax extensions

```scala
import Fernet.syntax._

key.encrypt("text")
key.decrypt(token)
key.toBase64
"base64string".asFernetKey
```

## Common errors

```scala
// ❌ Don't do this
val result = Fernet.decrypt(token, wrongKey)
// Left("Signature validation failed.")

// ✅ Do this
result match {
  case Right(data) => println(s"Success: $data")
  case Left(error) => println(s"Error: $error")
}
```

## Contributing

Pull requests welcome!

```bash
# Fork, clone, create branch
git checkout -b my-feature

# Make changes, add tests
sbt test

# Commit and push
git commit -m "feat: my feature"
git push origin my-feature
```

## License

MIT

## References

- [Fernet Spec](https://github.com/fernet/spec)
- [Python Cryptography](https://cryptography.io/en/latest/fernet/)
- [AES](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)
- [HMAC](https://en.wikipedia.org/wiki/HMAC)

---

Made with ❤️ by [@imcamilo](https://github.com/imcamilo)
