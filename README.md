# iso8583

Self-contained Java 11 / Android API 24 library for building, packing and sending ISO 8583 messages.

- **One jar, zero runtime dependencies.** JSON and XML layouts are parsed internally, with no Jackson and no `javax.xml`.
- **Runs on Android 7+ and JVM 11+.** The build fails on any API that is missing on Android API 24. R8 keep rules ship inside the jar.
- **Secure defaults.** TLS validates certificates and host names; `ProtectLogListener` masks PAN/track data and wipes PIN blocks and EMV data; packagers don't log field values.
- **Never reads files.** Your app passes layouts, key stores and settings in as `String`, `KeyStore` or setters.

## Build

```sh
./gradlew :app:build          # JDK 17+ to run Gradle; compiles with a Java 11 toolchain
```

Output: `app/build/libs/iso8583-2.0.0.jar`, the only file consumers need.

## Quick start

```gradle
dependencies {
    implementation files('libs/iso8583-2.0.0.jar')
}
```

```java
ISO87BPackager packager = new ISO87BPackager();

ISOMsg msg = new ISOMsg();
msg.setPackager(packager);            // set the packager before the values
msg.setMTI("0800");
msg.set(11, "000001");
msg.set(41, "00000001");
msg.set(70, "301");

NCCConnector conn = new NCCConnector(host, port, packager, ISOUtil.hex2byte("6000010000"));
conn.setConnectTimeout(10_000);
conn.connect();
try {
    conn.setTimeout(30_000);
    conn.send(msg);
    ISOMsg resp = conn.receive();     // match by F11 / F37
} finally {
    conn.disconnect();
}
```

On Android, run network calls off the main thread (`Dispatchers.IO`).

## Project layout

| Path | Contents |
|---|---|
| `app/` | The library (`com.base.iso.*`) and its tests |
| `appiso/` | Sample 0800 echo client: `./gradlew :appiso:run --args="<host> <port>"` |

## Documentation

[Instruction.md](Instruction.md) is the full guide. It covers:

- building the jar and what it guarantees;
- adding it to an Android or JVM project;
- messages and packagers, including JSON/XML layouts and sub-fields;
- connectors, TLS and logging;
- threading, and Android EDC integration with a Kotlin/Hilt example;
- migrating from 1.x to 2.0.0.

## License

See [LICENSE](LICENSE).
