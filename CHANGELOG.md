# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] - 2026-01-21

### Added
- Initial release of fernet4s
- Core Fernet encryption/decryption functionality
- AES-128-CBC encryption
- HMAC-SHA256 signing
- TTL (Time-To-Live) support for tokens
- Functional API with `Either` error handling
- Syntax extensions for fluent API
- Key generation and serialization
- Binary data encryption support
- Token verification without decryption
- Comprehensive test suite (57 tests)
- Integration tests with real-world use cases
- Interoperability tests (Fernet spec compliance)
- Interactive demo application
- Examples for Scala and Java
- Complete documentation

### Features
- **Simple API**: One key for everything
- **Type-safe**: `Either[String, A]` for all operations
- **Functional**: Chainable operations with for-comprehensions
- **Multi-language**: Works from Scala, Java, and Kotlin
- **Spec-compliant**: Compatible with Python, Ruby, Go implementations
- **Well-tested**: 57 tests covering edge cases and real scenarios

### Security
- AES-128 encryption in CBC mode
- HMAC-SHA256 for authentication
- Secure random key generation
- Timestamp-based token expiration
- Tamper detection via HMAC

### Documentation
- Comprehensive README with examples
- API reference
- Integration test examples
- Interactive demo application
- Use case documentation

[Unreleased]: https://github.com/imcamilo/fernet4s/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/imcamilo/fernet4s/releases/tag/v0.1.0
