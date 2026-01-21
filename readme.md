# fernet4s 🔐

> Cifrado simétrico simple y seguro para Scala

Fernet es como una caja fuerte para tus datos. Pones algo adentro con una llave, y solo esa llave puede abrirla después. Además, la caja tiene un reloj: si pasa mucho tiempo, ya no se puede abrir (TTL).

## ¿Por qué Fernet?

- ✅ **Simple**: Una sola llave para todo
- ✅ **Seguro**: AES-128 + HMAC-SHA256
- ✅ **Con timestamp**: Soporta expiración (TTL)
- ✅ **Estándar**: Compatible con Python, Ruby, Go, etc.
- ✅ **Sin sorpresas**: Detecta cualquier alteración

## Instalación

```scala
libraryDependencies += "io.github.imcamilo" %% "fernet4s" % "0.1.0"
```

## Uso Rápido

### Cifrar y descifrar

```scala
import com.github.imcamilo.fernet.Fernet

// Generar una llave
val key = Fernet.generateKey()

// Cifrar
val encrypted = Fernet.encrypt("Hola Fernet!", key)
// Right("gAAAAABh...")

// Descifrar
val decrypted = Fernet.decrypt(encrypted.right.get, key)
// Right("Hola Fernet!")
```

### Con sintaxis fluida

```scala
import com.github.imcamilo.fernet.Fernet.syntax._

val key = Fernet.generateKey()

// Más natural y encadenado
val result = for {
  token <- key.encrypt("Mensaje secreto")
  plain <- key.decrypt(token)
} yield plain

println(result) // Right("Mensaje secreto")
```

### Guardar y cargar llaves

```scala
// Convertir llave a texto
val keyString = key.toBase64
// "wz5hami-yvr3zHyzVEiOYFvN9kTzXRW3dP7NcUr9Nvs="

// Guardar en variable de entorno, archivo, etc.

// Cargar después
val importedKey = keyString.asFernetKey
```

### Tokens con expiración (TTL)

```scala
val key = Fernet.generateKey()
val token = key.encrypt("Dato temporal").right.get

// Solo válido por 60 segundos
val decrypted = key.decrypt(token, ttlSeconds = Some(60))

// Después de 60 segundos → Left("Token has expired")
```

### Desde Java

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

## Ejemplos

Revisa el directorio [`examples/`](examples/) para más casos de uso.

## ¿Cómo funciona?

Fernet usa:
- **AES-128-CBC** para cifrar
- **HMAC-SHA256** para firmar
- **Timestamp** para TTL
- **Base64 URL** para el token final

El token tiene este formato:
```
Version | Timestamp | IV | Ciphertext | HMAC
1 byte  | 8 bytes   | 16 | Variable   | 32 bytes
```

## Casos de uso

- 🔑 Tokens de sesión
- 💾 Cifrar datos en DB
- 🔐 API keys y secrets
- 📨 Mensajes seguros entre servicios
- 🎫 Tokens de verificación

## Tests

```bash
sbt test
```

## Compatibilidad

- ✅ Scala 2.13
- ✅ Java 8+
- ✅ Kotlin (vía interop Java)
- 📦 Compatible con [Fernet spec](https://github.com/fernet/spec)

## API

| Método | Descripción |
|--------|-------------|
| `generateKey()` | Genera una llave aleatoria |
| `keyToString(key)` | Exporta llave como Base64 |
| `keyFromString(str)` | Importa llave desde Base64 |
| `encrypt(text, key)` | Cifra texto |
| `decrypt(token, key)` | Descifra token |
| `verify(token, key)` | Verifica sin descifrar |

### Extensiones de sintaxis

```scala
import Fernet.syntax._

key.encrypt("texto")
key.decrypt(token)
key.toBase64
"base64string".asFernetKey
```

## Errores comunes

```scala
// ❌ No hacer esto
val result = Fernet.decrypt(token, wrongKey)
// Left("Signature validation failed.")

// ✅ Hacer esto
result match {
  case Right(data) => println(s"Éxito: $data")
  case Left(error) => println(s"Error: $error")
}
```

## Contribuir

Pull requests bienvenidos!

```bash
# Fork, clona, crea branch
git checkout -b mi-feature

# Haz cambios, agrega tests
sbt test

# Commit y push
git commit -m "feat: mi feature"
git push origin mi-feature
```

## Licencia

MIT

## Referencias

- [Fernet Spec](https://github.com/fernet/spec)
- [Python Cryptography](https://cryptography.io/en/latest/fernet/)
- [AES](https://en.wikipedia.org/wiki/Advanced_Encryption_Standard)
- [HMAC](https://en.wikipedia.org/wiki/HMAC)

---

Hecho con ❤️ por [@imcamilo](https://github.com/imcamilo)
