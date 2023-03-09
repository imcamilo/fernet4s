# fernet4s

A minimal, idiomatic Scala interface for Fernet Specification.

### Why?

I was looking for a simple implementation of Fernet Spec in Java or Scala, but I didn't find it. I found a very good
one, but, is currently available with Java8 <<here>>.

### Examples

##### Create a new key

````scala
val key = Key.generateKey();
````

or

````scala
val key = Key.generateKey(customRandom);
````

##### Deserialize an existing key:

````scala
val key = new Key.deserialize("cw_0x689RpI-jtRR7oE8h_eQsKImvJapLeSbXpwF4e4=");
````
Create a token:

````scala
val token = Token.generate(key, "secret message");
````
or

````scala
val token = Token.generate(customRandom, key, "secret message");
````

##### Deserialize an existing token:

````scala
val token = Token.fromString("gAAAAAAdwJ6wAAECAwQFBgcICQoLDA0ODy021cpGVWKZ_eEwCGM4BLLF_5CV9dOPmrhuVUPgJobwOz7JcbmrR64jVmpU4IwqDA==");
````

Validate the token, by default fernet4s use an String validator located in:

````scala 
val  payload = token.validateAndDecrypt(key, checker);
````

When validating, an exception is thrown if the token is not valid. In this example, the payload is just the decrypted
cipher text portion of the token. If you choose to store structured data in the token (e.g. JSON), or a pointer to a
domain object (e.g. a username), you can implement your own Validator<T> that returns the type of POJO your application
expects.

##### Use a custom time-to-live:

````scala
val simpleChecker: Checker[String] = new StringChecker {
  override def timeToLive: TemporalAmount = Duration.ofSeconds(60)
}
````

The default time-to-live is 30 minutes, but in this example, it's overridden to 1 hour.