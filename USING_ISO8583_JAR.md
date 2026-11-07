# Building and shipping `iso8583-<version>.jar`

This page covers building the jar, what it contains, the sample client, and how to migrate from 1.x. For using the API (messages, packagers, connectors, TLS, logging, threading), see [Instruction.md](Instruction.md).

## Build

Requirements: a JDK 17+ to run Gradle 8.14. A Java 11 toolchain is used to compile, and the foojay plugin downloads it if it's missing.

```sh
./gradlew :app:build        # compile, test, API/dependency checks, jars
```

Outputs in `app/build/libs/`:

| File | Purpose |
|---|---|
| `iso8583-2.0.0.jar` | The library. **The only file consumers need.** |
| `iso8583-2.0.0-sources.jar` | Sources, for IDE navigation and on-device debugging |

Optional internal Maven publishing: `./gradlew :app:publishToMavenLocal` (coordinates `com.base.iso:iso8583:2.0.0`; the POM lists no dependencies).

## What the jar guarantees

`./gradlew :app:check` enforces each of these and fails the build on a violation:

- **No third-party runtime dependencies** (`verifySelfContained`). The jar runs with JDK or Android framework classes only. If a dependency ever becomes unavoidable, it must be shaded and relocated under `com.base.iso.shaded.*`. See plan T21 in `.agents/planning/`.
- **Only Android-available JDK modules** (`jdepsCheck`): `java.base` and `java.sql`, which pulls in `java.xml`.
- **Only Android API 24 APIs** (`animalsnifferMain`). This rejects `java.nio.file`, `java.time`, `java.net.http`, and Java 9+ method overloads such as `ByteBuffer.position(int)`.
- **Bytecode for Java 11** (class file major 55), for JVM 11+ and Android D8.
- **R8 keep rules inside the jar** at `META-INF/proguard/iso8583.pro`.

Jar contents: `com/base/iso/**` and `META-INF/**` (manifest with `Automatic-Module-Name: com.base.iso`, plus the R8 rules).

## Add it to a consumer

```gradle
dependencies {
    implementation files('libs/iso8583-2.0.0.jar')   // nothing else
}
```

Release smoke checks, run with **only** the jar on the classpath:

1. **JVM:** compile and run a small client that builds a layout with `ISO87BPackager` and with `GenericJSONPackager.setFieldsFromJson(<string>)`, then does an `NCCConnector` loopback send/receive.
2. **Android:** in an app with `minSdk 24` and `minifyEnabled true`:
   - run `assembleRelease`;
   - on an Android 7 device or emulator, read a layout from the app's own asset into a `String`;
   - call `setFieldsFromJson` and pack a 0800.

## Sample client (`appiso`)

`appiso/src/main/java/com/base/app/App.java` sends an 0800 echo test over `NCCConnector` using `ISO87BPackager`, with `ProtectLogListener` chained before `SimpleLogListener`. It reads no files.

```sh
./gradlew :appiso:run --args="<host> <port>"   # defaults: localhost 8000 — use a test host only
```

## Migrating from 1.x to 2.0.0

### Removed APIs

| Removed | Replacement |
|---|---|
| `GenericJSONPackager.loadFromJsonFile(String path)` | Read the file or asset yourself, then call `setFieldsFromJson(content)` |
| `new SimpleConfiguration(String filename)`, `SimpleConfiguration.load(String)` | Load `Properties` yourself, then `new SimpleConfiguration(props)` |
| `GenericSSLSocketFactory` / `SunJSSESocketFactory`: `setKeyStore(String path)`, `setPassword`, `setKeyPassword`, `getKeyStore()`, and the config keys `keystore`, `storepassword`, `keypassword` | `setKeyStore(KeyStore, char[])`, `setTrustStore(KeyStore)` or `setSSLContext(SSLContext)` |
| Bundled resource `com/base/packager/iso87binary.json` | `ISO87BPackager`, or your own JSON string |
| Jackson on the classpath | Nothing: the JSON layout is parsed internally |

### Behaviour changes

- **TLS validates by default.** In 1.x the default trusted any certificate. Hosts with a self-signed or private-CA certificate need `setTrustStore(...)`. `setServerAuthNeeded(...)` is deprecated and ignored. Trust-all needs `insecure-trust-all=true`.
- **TLS checks the host name** against the certificate SANs. If you connect by IP to a host whose certificate names a DNS host, call `setServerName("dns.name")`.
- **`setFieldsFromJson` throws** `IllegalArgumentException` on an invalid layout or unknown class, and keeps the previous layout. In 1.x it printed a stack trace and continued.
- **Non-hex values for binary fields throw** `BaseException` in `ISOMsg.set(int, String)`.
- **A corrupt length prefix closes the socket** in `receive()`.
- **Packager logs omit field values by default.** Use `setLogFieldValues(true)` to debug.
- `connect()` already threw `IOException("Unable to connect")` on a refused connection in 1.x. The extra `ConnectException` path now rethrows too.

Unchanged: the wire format. Golden-master tests pin ISO87B/ISO93B packing and the framing of every connector.
