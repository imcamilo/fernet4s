# Fernet4s

This library provides simpler utilities for working with Fernet keys in Scala and Java.

## How to Create a Key

To create a Fernet key using this library, follow these steps:

1. **Import the necessary classes:**

 ```scala
import com.github.imcamilo.fernet.Key._

 ```

2. **Generate a key instance:**

 ```scala
val signingKey = Array.fill(signingKeyBytes)(0.toByte)
val encryptionKey = Array.fill(encryptionKeyBytes)(1.toByte)
val result = creatingKeyInstance(signingKey, encryptionKey)

result match {
  case Success((generatedSigningKey, generatedEncryptionKey)) =>
    println(s"Successfully generated Fernet key:")
    println(s"Signing Key: ${generatedSigningKey.toList}")
    println(s"Encryption Key: ${generatedEncryptionKey.toList}")
  case Failure(exception) =>
    println(s"Error creating Fernet key: ${exception.getMessage}")
}

```

3. **Handle possible exceptions:**

Make sure to handle exceptions that may occur during the key creation process. For example:

```scala
try {
  val result = creatingKeyInstance(signingKey, encryptionKey)
  // Handle the result
} catch {
  case e: InvalidKeyException =>
    println(s"Invalid key: ${e.getMessage}")
  case e: Exception =>
    println(s"An unexpected error occurred: ${e.getMessage}")
}
```

## Contributing

Contributions are welcome! If you have ideas for improvements, new features, or find any issues, please open an issue or
submit a pull request. I'd appreciate your feedback and collaboration.

### How to Contribute

1. **Fork the repository:** Duplicate the repository to your GitHub account.
2. **Create a new branch:** Make your changes in a new branch to keep your work isolated.
3. **Implement your changes:** Make your desired modifications and ensure tests pass.
4. **Commit your changes:** Save your changes with a clear and concise commit message.
5. **Push to your branch:** Upload your changes to your GitHub repository.
6. **Open a pull request:** Propose your changes to be merged into the main project.

We value and encourage collaborative development. Your contributions help make this library better for everyone.

In the future I'd like to use just type classes :c.