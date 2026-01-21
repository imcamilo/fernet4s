package com.github.imcamilo.fernet

import com.github.imcamilo.fernet.Fernet.syntax._
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.concurrent.duration._

/** Integration tests with real-world use cases */
class IntegrationSpec extends AnyWordSpec with Matchers {

  "Fernet integration" when {

    "used for session tokens" should {

      case class UserSession(userId: String, username: String, role: String) {
        def serialize: String = s"$userId|$username|$role"
      }

      object UserSession {
        def deserialize(data: String): Option[UserSession] = {
          data.split("\\|").toList match {
            case userId :: username :: role :: Nil =>
              Some(UserSession(userId, username, role))
            case _ => None
          }
        }
      }

      "create and validate session tokens" in {
        val key = Fernet.generateKey()
        val session = UserSession("123", "john.doe", "admin")

        // Create session token
        val tokenResult = key.encrypt(session.serialize)
        tokenResult shouldBe a[Right[_, _]]

        // Validate and extract session
        val sessionResult = tokenResult.flatMap { token =>
          key.decrypt(token).flatMap { data =>
            UserSession.deserialize(data)
              .toRight("Invalid session data")
          }
        }

        sessionResult match {
          case Right(recovered) =>
            recovered.userId shouldEqual "123"
            recovered.username shouldEqual "john.doe"
            recovered.role shouldEqual "admin"
          case Left(error) =>
            fail(s"Session validation failed: $error")
        }
      }

      "reject expired session tokens" in {
        val key = Fernet.generateKey()
        val session = UserSession("456", "jane.doe", "user")

        val tokenResult = key.encrypt(session.serialize)

        // Simulate old token by setting TTL to 0 seconds
        // (In real scenario, you'd wait for expiration)
        val decryptResult = tokenResult.flatMap { token =>
          // Try to decrypt with very short TTL
          Thread.sleep(1000) // Wait 1 second
          key.decrypt(token, ttlSeconds = Some(0))
        }

        decryptResult shouldBe a[Left[_, _]]
      }
    }

    "used for API keys" should {

      case class ApiKey(
          clientId: String,
          scopes: List[String],
          createdAt: Long = System.currentTimeMillis()
      ) {
        def serialize: String = s"$clientId:${scopes.mkString(",")}:$createdAt"
      }

      object ApiKey {
        def deserialize(data: String): Option[ApiKey] = {
          data.split(":").toList match {
            case clientId :: scopesStr :: timestamp :: Nil =>
              val scopes = if (scopesStr.isEmpty) List.empty else scopesStr.split(",").toList
              Some(ApiKey(clientId, scopes, timestamp.toLong))
            case _ => None
          }
        }
      }

      "generate and validate API keys" in {
        val masterKey = Fernet.generateKey()
        val apiKey = ApiKey("client-001", List("read", "write", "delete"))

        // Generate encrypted API key
        val encryptedKey = masterKey.encrypt(apiKey.serialize)
        encryptedKey shouldBe a[Right[_, _]]

        // Validate API key
        val validationResult = encryptedKey.flatMap { token =>
          masterKey.decrypt(token).flatMap { data =>
            ApiKey.deserialize(data)
              .toRight("Invalid API key format")
          }
        }

        validationResult match {
          case Right(recovered) =>
            recovered.clientId shouldEqual "client-001"
            recovered.scopes should contain allOf ("read", "write", "delete")
          case Left(error) =>
            fail(s"API key validation failed: $error")
        }
      }

      "store API key and retrieve it later" in {
        val masterKey = Fernet.generateKey()
        val keyString = masterKey.toBase64

        val apiKey = ApiKey("client-002", List("read"))
        val token = masterKey.encrypt(apiKey.serialize).toOption.get

        // Simulate storing key in env var or config
        val storedKeyString = keyString
        val storedToken = token

        // Simulate loading from storage
        val loadedKey = storedKeyString.asFernetKey
        val result = loadedKey.flatMap(_.decrypt(storedToken))

        result shouldBe a[Right[_, _]]
        result.map(ApiKey.deserialize) shouldEqual Right(Some(apiKey))
      }
    }

    "used for encrypted configuration" should {

      case class DatabaseConfig(
          host: String,
          port: Int,
          username: String,
          password: String
      ) {
        def serialize: String =
          s"""{"host":"$host","port":$port,"username":"$username","password":"$password"}"""
      }

      "encrypt sensitive configuration" in {
        val configKey = Fernet.generateKey()
        val dbConfig = DatabaseConfig(
          "db.example.com",
          5432,
          "admin",
          "super-secret-password"
        )

        // Encrypt config
        val encryptedConfig = configKey.encrypt(dbConfig.serialize)
        encryptedConfig shouldBe a[Right[_, _]]

        // Config should not contain plaintext password
        encryptedConfig.foreach { token =>
          token should not include "super-secret-password"
        }

        // Decrypt and use config
        val decryptedConfig = encryptedConfig.flatMap(configKey.decrypt(_))
        decryptedConfig shouldBe a[Right[_, _]]
        decryptedConfig.foreach { config =>
          config should include("super-secret-password")
        }
      }
    }

    "used for secure tokens between services" should {

      case class ServiceMessage(
          from: String,
          to: String,
          payload: String,
          timestamp: Long = System.currentTimeMillis()
      ) {
        def serialize: String = s"$from>>$to>>$payload>>$timestamp"
      }

      object ServiceMessage {
        def deserialize(data: String): Option[ServiceMessage] = {
          data.split(">>").toList match {
            case from :: to :: payload :: timestamp :: Nil =>
              Some(ServiceMessage(from, to, payload, timestamp.toLong))
            case _ => None
          }
        }
      }

      "encrypt messages between microservices" in {
        val sharedKey = Fernet.generateKey()

        // Service A sends message to Service B
        val message = ServiceMessage(
          from = "user-service",
          to = "payment-service",
          payload = "process-payment:amount=100.00"
        )

        val encryptedMessage = sharedKey.encrypt(message.serialize)
        encryptedMessage shouldBe a[Right[_, _]]

        // Service B receives and decrypts
        val receivedMessage = encryptedMessage.flatMap { token =>
          sharedKey.decrypt(token, ttlSeconds = Some(300)) // 5 min TTL
        }

        receivedMessage shouldBe a[Right[_, _]]
        val parsed = receivedMessage.flatMap(data =>
          ServiceMessage.deserialize(data).toRight("Parse error")
        )

        parsed match {
          case Right(msg) =>
            msg.from shouldEqual "user-service"
            msg.to shouldEqual "payment-service"
            msg.payload should include("process-payment")
          case Left(error) =>
            fail(s"Message decryption failed: $error")
        }
      }
    }

    "used for password reset tokens" should {

      case class ResetToken(
          userId: String,
          email: String,
          nonce: String = java.util.UUID.randomUUID().toString
      ) {
        def serialize: String = s"$userId:$email:$nonce"
      }

      object ResetToken {
        def deserialize(data: String): Option[ResetToken] = {
          data.split(":").toList match {
            case userId :: email :: nonce :: Nil =>
              Some(ResetToken(userId, email, nonce))
            case _ => None
          }
        }
      }

      "generate time-limited reset tokens" in {
        val appKey = Fernet.generateKey()
        val resetToken = ResetToken("user-789", "user@example.com")

        // Generate reset token
        val token = appKey.encrypt(resetToken.serialize)
        token shouldBe a[Right[_, _]]

        // Verify token within time limit
        val verification = token.flatMap { t =>
          appKey.decrypt(t, ttlSeconds = Some(3600)) // 1 hour
        }

        verification shouldBe a[Right[_, _]]

        // Parse reset data
        val parsed = verification.flatMap(data =>
          ResetToken.deserialize(data).toRight("Invalid token")
        )

        parsed match {
          case Right(reset) =>
            reset.userId shouldEqual "user-789"
            reset.email shouldEqual "user@example.com"
            reset.nonce should not be empty
          case Left(error) =>
            fail(s"Token verification failed: $error")
        }
      }
    }

    "used with binary data" should {

      "encrypt and decrypt files" in {
        val fileKey = Fernet.generateKey()

        // Simulate file contents
        val fileContents = "This is a secret file\nWith multiple lines\nAnd sensitive data".getBytes

        // Encrypt file
        val encryptedFile = fileKey.encryptBytes(fileContents)
        encryptedFile shouldBe a[Right[_, _]]

        // Decrypt file
        val decryptedFile = encryptedFile.flatMap(token =>
          fileKey.decryptBytes(token)
        )

        decryptedFile shouldBe a[Right[_, _]]
        decryptedFile.map(new String(_)) shouldEqual Right(new String(fileContents))
      }
    }

    "demonstrate key rotation" should {

      "re-encrypt data with new key" in {
        val oldKey = Fernet.generateKey()
        val newKey = Fernet.generateKey()

        val sensitiveData = "user-sensitive-information"

        // Encrypt with old key
        val oldToken = oldKey.encrypt(sensitiveData).toOption.get

        // Decrypt with old key and re-encrypt with new key
        val rotationResult = for {
          decrypted <- oldKey.decrypt(oldToken)
          newToken <- newKey.encrypt(decrypted)
        } yield newToken

        rotationResult shouldBe a[Right[_, _]]

        // Verify new token works
        val verification = rotationResult.flatMap(newKey.decrypt(_))
        verification shouldEqual Right(sensitiveData)

        // Verify old key can't decrypt new token
        val oldKeyTry = rotationResult.flatMap(oldKey.decrypt(_))
        oldKeyTry shouldBe a[Left[_, _]]
      }
    }
  }
}
