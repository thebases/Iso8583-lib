# iso8583 library — integration guide

`iso8583-<version>.jar` is a Java library for **ISO 8583 message handling**:

- building messages (`ISOMsg`);
- packing and unpacking them with a packager (`ISO87BPackager`, `ISO93BPackager`, or a JSON-defined `GenericJSONPackager`);
- framing them over TCP with a connector (`NCCConnector`, `BCDConnector`, `NACConnector`, `HEXConnector`).

The library **never reads files**. Your app or service reads its own configuration (Android asset, config service, secret store) and passes the library a `String`, `Properties`, `KeyStore` or `SSLContext`.

The same jar serves two kinds of consumer:

| | Android EDC app | JVM service |
|---|---|---|
| Runtime | minSdk 24 (Android 7.x), D8/R8 | Java 11+ |
| Traffic | one terminal, one connection | many threads, many connections |
| Notes | R8 keep rules ship inside the jar | see [Threading](#7-threading-jvm-services) |

How to obtain or build the jar, the sample app, and the 1.x → 2.0 migration notes are in [USING_ISO8583_JAR.md](USING_ISO8583_JAR.md).

---

## 1. Add the jar

The jar is self-contained: it has **no third-party runtime dependencies** (JDK / Android framework classes only). Copy the one file into your project:

```gradle
dependencies {
    implementation files('libs/iso8583-2.0.0.jar')
}
```

Do not add Jackson or anything else for this library. Since 2.0.0 the JSON layout is parsed by an internal reader.

## 2. Build a message

```java
ISO87BPackager packager = new ISO87BPackager();

ISOMsg msg = new ISOMsg();
msg.setPackager(packager);          // FIRST — see below
msg.setMTI("0200");
msg.set(2, pan);
msg.set(3, "000000");
msg.set(4, "000000010000");
msg.set(11, stan);
msg.set(41, terminalId);
msg.set(52, pinBlockHex);           // binary field: hex string → bytes
```

Rules:

- **Set the packager before the values.** `set(int, String)` asks the packager whether the field is binary. With the packager set, F52/F55-style binary fields are parsed from hex into bytes. Without it, the value is stored as text and packs wrongly.
- Binary values must be valid hex. Since 2.0.0, a non-hex character throws `BaseException("invalid hex for field n …")`. In 1.x it silently produced a corrupt PIN block. Odd-length hex is left-padded with `0` (`"ABC"` → `0x0A 0xBC`).
- `set(n, (String) null)` removes field *n*. `set(int, byte[])` always stores raw bytes.
- Use **one `ISOMsg` per transaction**. Don't share a message object between threads.

## 3. Choose a packager

| Packager | Use when |
|---|---|
| `ISO87BPackager` | Host uses the ISO 8583:1987 binary layout |
| `ISO93BPackager` | Host uses the ISO 8583:1993 binary layout |
| `GenericJSONPackager` | Host has a custom layout you keep as data |

### JSON layout (`GenericJSONPackager`)

Your code reads the layout text itself, then passes it in:

```java
// Android: from your own asset
String json;
try (InputStream in = context.getAssets().open("iso_layout.json")) {
    json = readUtf8(in);            // your helper
}
GenericJSONPackager packager = new GenericJSONPackager();
packager.setFieldsFromJson(json);   // throws IllegalArgumentException if the layout is invalid
```

Schema:

```json
{ "isofields": [
  { "id": 0,  "length": 4,  "name": "MTI",      "class": "IFB_NUMERIC", "pad": true },
  { "id": 1,  "length": 16, "name": "BIT MAP",  "class": "IFB_BITMAP" },
  { "id": 2,  "length": 19, "name": "PAN",      "class": "IFB_LLNUM",   "pad": false }
]}
```

- `class` is the simple name of a codec in `com.base.iso.fields` (`IFB_NUMERIC`, `IFB_LLNUM`, `IFB_LLLCHAR`, `IFB_BINARY`, `IF_CHAR`, ...).
- `id` and `length` may be numbers or numeric strings. `pad` may be `true`/`false` or `"true"`/`"false"`. `name` is optional.
- For `IFB_NUMERIC`, `pad: true` means left-padded BCD, which is what `ISO87BPackager` uses. Copy that when you port a code-defined layout.
- **Invalid layouts fail loudly.** These all throw `IllegalArgumentException` with the entry index or class name:
  - malformed JSON;
  - a missing `isofields` array;
  - a missing `id`, `length` or `class`;
  - an `id` outside 0..128, or a duplicate `id`;
  - an unknown class.

  On failure the previous layout stays active. (1.x printed a stack trace and carried on.)
- A new `GenericJSONPackager()` starts with the ISO87B layout.

## 4. Pack and unpack

```java
byte[] packed = msg.pack();

ISOMsg resp = new ISOMsg();
resp.setPackager(packager);
resp.unpack(responseBytes);
String rc = resp.getString(39);
```

If unpacking fails, the `BaseException` message names the field and the reason. Example: `field packager '3' is null (-) unpacking field=3, consumed=10`.

## 5. Send over TCP (connectors)

A connector frames the packed message as **[length prefix][header/TPDU][packed message]**:

| Connector | Length prefix | Header |
|---|---|---|
| `NCCConnector` | 2-byte BCD (max 9999) | TPDU; src/dest swapped when the message carries a 5-byte header (`tpdu-swap`, default on) |
| `BCDConnector` | 2-byte BCD | TPDU; src/dest always swapped for a 5-byte message header |
| `NACConnector` | 2-byte binary, big-endian | TPDU; swap like NCC |
| `HEXConnector` | 4 ASCII hex characters (max 0xFFFF) | optional |

```java
byte[] tpdu = ISOUtil.hex2byte("6000010000");
NCCConnector conn = new NCCConnector(host, port, packager, tpdu);
conn.connect();                     // throws IOException if refused / unreachable
try {
    conn.send(msg);                 // uses the connector's packager
    ISOMsg resp = conn.receive();
} finally {
    conn.disconnect();
}
```

- Retries are safe: re-sending the **same** `ISOMsg` (a retry, or a reversal re-sent after a timeout) puts the same TPDU on the wire. Since 2.0.0 the swap works on a copy of the header.
- If the length prefix is corrupt (not BCD/hex), `receive()` throws `BaseException("invalid length prefix: …")` **and closes the socket**. Reconnect before the next exchange.
- The connector does **not** match responses to requests. Match them yourself by F11 (STAN) / F37 (RRN).
- `sendraw` (NCC), `sendBcd` (BCD) and `sendAscii` (NAC) send pre-packed bytes. Their framing **differs** from the connector's own `send(ISOMsg)` in two cases:
  - `NCCConnector.sendraw` uses a binary length;
  - `NACConnector.sendAscii` uses a 4-digit ASCII length.

  Check the host spec before you use them.

### TLS

`GenericSSLSocketFactory` (and the legacy name `SunJSSESocketFactory`) is **secure by default**:

- It validates the server certificate, against the trust store you inject or else the platform CAs.
- It verifies the host name, honouring SANs.
- It uses a `TLS` context.

```java
GenericSSLSocketFactory tls = new GenericSSLSocketFactory();
tls.setTrustStore(myTrustStore);            // KeyStore you loaded (Android: BKS/PKCS12 asset or AndroidKeyStore)
tls.setKeyStore(myClientKeys, keyPassword); // only for mutual TLS
tls.setServerName("host.example.com");      // when connecting by IP to a host whose cert names a DNS host
conn.setSocketFactory(tls);
```

Alternatively, pass a fully built context with `setSSLContext(SSLContext)`.

Trust-all (`setInsecureTrustAll(true)` or config `insecure-trust-all=true`) disables validation and logs a warning. Use it in test labs only.

Keystore paths are no longer accepted: config key `keystore` throws `ConfigurationException`.

## 6. Logging without leaking card data

Attach a `Logger` to packagers and connectors. Always add **`ProtectLogListener` first**:

```java
Logger logger = new Logger();
logger.addListener(new ProtectLogListener());          // masks before anything is written
logger.addListener(new SimpleLogListener(System.out)); // or your own listener (logcat, log shipper)
conn.setLogger(logger, "host-a");
packager.setLogger(logger, "host-a");
```

- `ProtectLogListener` replaces each `ISOMsg` in a log event with a masked clone. Your message is not changed.
  - F2, F35 and F45 are masked with `ISOUtil.protect`, keeping the first 6 and last 4 digits.
  - F52 and F55 are wiped.
  - Raw `byte[]` items are replaced by `[n bytes]`.
- By default packagers log only field numbers, codecs and lengths, never values or raw hex. `packager.setLogFieldValues(true)` turns values on for local debugging **with test cards only**.
- Connector receive errors log the frame size, not a hexdump.

## 7. Threading (JVM services)

- **One connector per socket.** Use one sender thread and one receiver thread per connector, or lock externally. `send` and `receive` are internally serialized per direction.
- **Packagers are shareable** across threads once configured. pack/unpack keep no per-call state.
- **Reloading a JSON layout:** don't call `setFieldsFromJson` on a packager other threads are using. Build a new `GenericJSONPackager`, then swap the reference (e.g. a `volatile` field or `AtomicReference`).
- **One `ISOMsg` per transaction.** Match responses by F11/F37.
- `BaseConnector.setName(n)` registers the connector for `BaseConnector.getChannel(n)`. The registry is thread-safe, and renaming drops the old entry. Call `unregister()` when you retire a named connector.

## 8. Android specifics

- minSdk 24 is supported. The build fails if library code uses an API missing on Android 7 (`java.nio.file`, `java.time`, Java 9+ methods). This is checked by animal-sniffer against the API 24 signature.
- **R8:** keep rules ship inside the jar at `META-INF/proguard/iso8583.pro`, and R8 applies them automatically. They keep:
  - the field codecs that JSON layouts name;
  - the packagers that `ISOMsg` deserialization instantiates.

  If you see `IllegalArgumentException: Unknown field class '…'` only in release builds, check that the jar's rules weren't filtered out.
- Do network I/O (`connect`, `send`, `receive`) off the main thread.

## 9. Key classes

| Class | Role |
|---|---|
| `core.ISOMsg` | The message: MTI, bitmap, fields, header |
| `core.ISOField` / `core.BinaryFieldComponent` | Text / binary field values (usually created by `ISOMsg.set`) |
| `packager.aBasePackager` | The real pack/unpack logic behind every packager |
| `fields.*` | Field codecs: `IFB_*` binary/BCD, `IFA_*` ASCII, `IF_CHAR` fixed text, `IFAE_*` EBCDIC, `IFA_TT*` tagged |
| `connector.BaseConnector` | Socket lifecycle, send/receive, filters, timeouts |
| `util.ISOUtil` | Hex/BCD helpers: `hexString`, `hex2byte`, `str2bcd`, `bcd2str`, `protect` |
| `util.log.Logger`, `ProtectLogListener`, `SimpleLogListener` | Logging |
| `config.SimpleConfiguration` | `Properties`-backed config for `setConfiguration(...)` |

Codec name decoding: `LL`/`LLL` = 2-/3-digit length prefix; `NUM` = numeric; `CHAR` = text; `BINARY` = raw bytes; `IFB_` = binary/BCD encoding; `IFA_` = ASCII; `LLH`/`LLLH` = binary length prefix instead of BCD.
