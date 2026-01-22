# Fernet4s Examples

Complete working examples for both Scala 3 and Java.

## Scala 3 Example

**Location:** `scala3/CompleteExample.scala`

**9 Examples:**
1. Basic Encryption/Decryption
2. Syntax Extensions
3. Key Serialization
4. TTL (Time-To-Live)
5. Binary Data
6. Token Verification
7. Error Handling
8. Key Rotation with MultiFernet
9. Result API

**Run:**
```bash
# From fernet4s root directory
sbt "runMain examples.completeExample"
```

**Expected Output:**
```
=== Fernet4s Scala 3 Complete Examples ===

=== Example 1: Basic Encryption/Decryption ===
✓ Decrypted: Hello Fernet from Scala 3!

=== Example 2: Syntax Extensions ===
Token: gAAAAABpcWL0wSKjKRY2YL8ALpB-ZTbESp5V...
Decrypted: Secret message

... (9 examples total)
```

## Java Example

**Location:** `java/Main.java`

**8 Examples:**
1. Basic Encryption/Decryption
2. Error Handling (Invalid Key)
3. Byte Array Encryption
4. MultiFernet (Key Rotation)
5. TTL Validation
6. Result API (Java-friendly)
7. Functional Composition with Result
8. Key Generation

**Run:**
```bash
# Compile
javac -cp "path/to/fernet4s_3-1.0.0.jar:path/to/scala3-library.jar" java/Main.java

# Run
java -cp ".:path/to/fernet4s_3-1.0.0.jar:path/to/scala3-library.jar" com.github.imcamilo.examples.Main
```

**Or with Gradle:**
```gradle
dependencies {
    implementation 'io.github.imcamilo:fernet4s_3:1.0.0'
    implementation 'org.scala-lang:scala3-library_3:3.3.3'
}
```

```bash
gradle run
```

**Expected Output:**
```
=== Fernet4s Java Examples ===

1. Basic Encryption/Decryption:
  Original: Hola Fernet desde Java!
  Decrypted: Hola Fernet desde Java!

2. Error Handling (Invalid Key):
  Decryption failed as expected: Decryption failed

... (8 examples total)
```

## Key Features Demonstrated

### Scala 3
- ✅ For-comprehension with Either
- ✅ Syntax extensions (`.encrypt`, `.decrypt`, `.toBase64`, `.asFernetKey`)
- ✅ Pattern matching
- ✅ Option/Either handling
- ✅ Functional composition

### Java
- ✅ Result API (Java-friendly)
- ✅ Functional chaining with `flatMap()` and `map()`
- ✅ Error handling with `isSuccess()` / `isFailure()`
- ✅ `getOrElseValue()` for default values
- ✅ Clean varargs API with `MultiFernet.create()`

## Common Patterns

### Encryption
```scala
// Scala
val result = Fernet.encrypt("data", key)

// Java
Result<String> result = Fernet.encryptResult("data", key);
```

### Decryption with TTL
```scala
// Scala - 60 seconds
key.decrypt(token, Some(60))

// Java - 3600 seconds (1 hour)
Fernet.decryptResult(token, key, 3600L);
```

### Key Rotation
```scala
// Scala
val multi = new MultiFernet(List(newKey, oldKey))
multi.decrypt(token)

// Java
MultiFernet multi = MultiFernet.create(newKey, oldKey);
multi.decryptResult(token);
```

## Testing Your Examples

Both examples have been tested and produce the output shown in the main README.

See the main [README.md](../README.md) for complete documentation.
