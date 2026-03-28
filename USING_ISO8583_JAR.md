# Using `app/build/libs/iso8583.jar`

This document explains how to use [`iso8583.jar`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/build/libs/iso8583.jar) based on the actual consumer code in [`appiso`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso) and the library implementation in [`app`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app).

## Reference implementation in this repository

The clearest example of how this jar is intended to be used is the runnable app in [`appiso`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso):

- [`appiso/build.gradle`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/build.gradle) depends on `project(":app")`
- [`appiso/src/main/java/com/base/app/App.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/src/main/java/com/base/app/App.java) creates a `GenericJSONPackager`
- It loads field definitions from `cfg/iso87binary.json`
- It opens a TCP connection with `NCCConnector`
- It builds and sends an `0800` network-management message

That means the practical usage pattern for `iso8583.jar` in this repository is:

1. Add the jar as a library
2. Load a JSON field definition file
3. Create an `ISOMsg`
4. Set MTI and required fields
5. Use a connector such as `NCCConnector` to send and receive messages

## What the jar contains

The jar is a Java library for:

- Building ISO 8583 messages with `com.base.iso.core.ISOMsg`
- Packing and unpacking messages with predefined packagers such as `ISO87BPackager` and `ISO93BPackager`
- Creating custom field layouts with `GenericJSONPackager`
- Sending and receiving ISO 8583 messages over TCP with connectors such as `BCDConnector`, `NACConnector`, `NCCConnector`, and `HEXConnector`
- Utility functions such as hex conversion via `com.base.iso.util.ISOUtil`

Main classes to start with:

- `com.base.iso.core.ISOMsg`
- `com.base.iso.packager.ISO87BPackager`
- `com.base.iso.packager.ISO93BPackager`
- `com.base.iso.packager.GenericJSONPackager`
- `com.base.iso.connector.BCDConnector`
- `com.base.iso.util.ISOUtil`

## Java version

The library is built for Java 11.

Use Java 11 or newer when compiling and running code against this jar.

## Important packaging note

[`iso8583.jar`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/build/libs/iso8583.jar) is a plain library jar. It is not a fat jar, and `appiso` confirms that it is expected to be consumed by another module/application.

That means:

- If your code only uses the core packager/message/connector classes shown below, the jar may be enough.
- If you use features that rely on external libraries, you must also add those dependencies to your runtime.
- In particular, `GenericJSONPackager` depends on Jackson.

The `app/build.gradle` declares these external libraries:

- `com.fasterxml.jackson.core:jackson-core:2.17.1`
- `com.fasterxml.jackson.core:jackson-databind:2.17.1`
- `com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.17.1`
- `com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.1`
- `com.sleepycat:je:18.3.12`
- `jdbm:jdbm:1.0`
- `org.yaml:snakeyaml:2.0`
- `org.bouncycastle:bcprov-jdk15on:1.50`
- `org.bouncycastle:bcpg-jdk15on:1.50`

## How `appiso` uses the jar

`appiso` is a sample client application for the jar.

Its build file:

```gradle
dependencies {
    implementation project(":app")
    implementation "com.google.guava:guava:32.1.2-jre"
}
```

Its main flow in [`App.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/src/main/java/com/base/app/App.java) is:

```java
GenericJSONPackager packager = new GenericJSONPackager();
packager.loadFromJsonFile("cfg/iso87binary.json");

NCCConnector connector = new NCCConnector(
        "62.146.233.40", 8000, packager, null);

connector.connect();

ISOMsg m = new ISOMsg();
m.setMTI("0800");
m.set(11, "000001");
m.set(41, "00000001");
m.set(70, "301");
m.recalcBitMap();
connector.send(m);
ISOMsg r = connector.receive();

connector.disconnect();
```

This is the usage model to follow if you want behavior consistent with the repository.

## Add the jar to your project

## Option 1: compile and run from the command line

```powershell
javac -cp "C:\Git\thebase\Iso8583-lib\Iso8583-lib\app\build\libs\iso8583.jar" .\src\Main.java
java -cp ".;C:\Git\thebase\Iso8583-lib\Iso8583-lib\app\build\libs\iso8583.jar" Main
```

On Linux or macOS, replace `;` with `:`.

## Option 2: Gradle local jar dependency

```gradle
dependencies {
    implementation files("C:/Git/thebase/Iso8583-lib/Iso8583-lib/app/build/libs/iso8583.jar")
}
```

If you also use `GenericJSONPackager`, add Jackson explicitly:

```gradle
dependencies {
    implementation files("C:/Git/thebase/Iso8583-lib/Iso8583-lib/app/build/libs/iso8583.jar")
    implementation "com.fasterxml.jackson.core:jackson-core:2.17.1"
    implementation "com.fasterxml.jackson.core:jackson-databind:2.17.1"
}
```

If you want to mirror `appiso`, your dependencies will typically include:

```gradle
dependencies {
    implementation files("C:/Git/thebase/Iso8583-lib/Iso8583-lib/app/build/libs/iso8583.jar")
    implementation "com.fasterxml.jackson.core:jackson-core:2.17.1"
    implementation "com.fasterxml.jackson.core:jackson-databind:2.17.1"
}
```

## Build and run the repository example

Because this is a multi-project Gradle build declared in [`settings.gradle`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/settings.gradle), the repository already contains a working consumer module.

Build the jar:

```powershell
.\gradlew.bat :app:jar
```

Run the sample application:

```powershell
.\gradlew.bat :appiso:run
```

Important runtime detail:

- [`App.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/src/main/java/com/base/app/App.java) loads `cfg/iso87binary.json` using a filesystem path
- The file exists at [`appiso/cfg/iso87binary.json`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/cfg/iso87binary.json)
- So the working directory must allow that relative path to resolve, or you must change the code to use an absolute path or classpath resource

The app also prints:

```java
System.out.println("Working directory: " + System.getProperty("user.dir"));
```

That line exists specifically to help verify the config file path being used at runtime.

## Basic usage: create, pack, and unpack an ISO 8583 message

This is the smallest useful flow:

1. Create an `ISOMsg`
2. Assign a packager
3. Set MTI and fields
4. Pack to `byte[]`
5. Unpack into another `ISOMsg`

Example:

```java
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.util.ISOUtil;

public class Main {
    public static void main(String[] args) throws Exception {
        ISO87BPackager packager = new ISO87BPackager();

        ISOMsg request = new ISOMsg();
        request.setPackager(packager);
        request.setMTI("0200");
        request.set(2, "4761739001010010");
        request.set(3, "000000");
        request.set(4, "000000010000");
        request.set(7, "0310123456");
        request.set(11, "123456");
        request.set(41, "TERMID01");
        request.set(49, "704");

        byte[] packed = request.pack();

        System.out.println("Packed HEX:");
        System.out.println(ISOUtil.hexString(packed));

        ISOMsg parsed = new ISOMsg();
        parsed.setPackager(packager);
        parsed.unpack(packed);

        System.out.println("MTI  : " + parsed.getMTI());
        System.out.println("F2   : " + parsed.getString(2));
        System.out.println("F3   : " + parsed.getString(3));
        System.out.println("F4   : " + parsed.getString(4));
        System.out.println("F11  : " + parsed.getString(11));
        System.out.println("F41  : " + parsed.getString(41));
        System.out.println("F49  : " + parsed.getString(49));
    }
}
```

## Choosing a packager

Use the packager that matches the host specification:

- `ISO87BPackager`: ISO 8583:1987 binary layout
- `ISO93BPackager`: ISO 8583:1993 binary layout
- `GenericJSONPackager`: custom layout from JSON

If the remote host says a field is fixed numeric, LLVAR, LLLVAR, binary, MAC, or BCD, your packager must match that exact definition. Otherwise the packed bytes will be wrong even if the Java code compiles.

## Binary fields

`ISOMsg.set(int, String)` checks the active packager. If the field is defined as a binary field, the library interprets the string as hex and converts it to bytes.

For example, for PIN or MAC fields:

```java
msg.set(52, "1234567890ABCDEF");
msg.set(64, "0011223344556677");
```

You can also set raw bytes directly:

```java
msg.set(52, new byte[] { 0x12, 0x34, 0x56, 0x78 });
```

## Use a custom JSON packager

`GenericJSONPackager` starts from the built-in `ISO87BPackager` field map and then replaces it with definitions loaded from JSON.

There are 2 supported ways to load definitions:

- `setFieldsFromJson(String jsonString)`
- `loadFromJsonFile(String filePath)`

The repository contains JSON field maps in both of these locations:

- [`app/src/main/resources/com/base/packager/iso87binary.json`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/resources/com/base/packager/iso87binary.json)
- [`appiso/cfg/iso87binary.json`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/cfg/iso87binary.json)

The `appiso` sample uses the second one through a filesystem path:

```java
packager.loadFromJsonFile("cfg/iso87binary.json");
```

Example:

```java
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.GenericJSONPackager;

public class Main {
    public static void main(String[] args) throws Exception {
        GenericJSONPackager packager = new GenericJSONPackager();
        packager.loadFromJsonFile("config/iso87binary.json");

        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI("0800");
        msg.set(3, "990000");
        msg.set(11, "000001");
        msg.set(70, "001");

        byte[] data = msg.pack();
        System.out.println(data.length);
    }
}
```

JSON format expected by `GenericJSONPackager`:

```json
{
  "isofields": [
    {
      "id": "3",
      "length": "6",
      "name": "PROCESSING CODE",
      "class": "IFB_NUMERIC",
      "pad": "true"
    }
  ]
}
```

Notes:

- `class` is the short class name under package `com.base.iso.fields`
- Example: `IFB_NUMERIC`, `IFB_LLNUM`, `IFB_LLLCHAR`, `IFB_BINARY`
- `loadFromJsonFile` reads from a filesystem path, not a classpath resource name

## Send a message over TCP with `NCCConnector`

This is the transport actually used by `appiso`.

`NCCConnector` works with:

- 2-byte BCD message length
- optional TPDU/header
- packed ISO message

Repository-style example:

```java
import com.base.iso.connector.NCCConnector;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.log.iLogSource;
import com.base.iso.packager.GenericJSONPackager;
import com.base.iso.util.log.Logger;
import com.base.iso.util.log.SimpleLogListener;

public class Main {
    public static void main(String[] args) throws Exception {
        Logger logger = new Logger();
        logger.addListener(new SimpleLogListener(System.out));

        GenericJSONPackager packager = new GenericJSONPackager();
        packager.loadFromJsonFile("cfg/iso87binary.json");

        NCCConnector connector = new NCCConnector("62.146.233.40", 8000, packager, null);
        ((iLogSource) connector).setLogger(logger, "test-channel");

        connector.connect();
        try {
            ISOMsg m = new ISOMsg();
            m.setMTI("0800");
            m.set(11, "000001");
            m.set(41, "00000001");
            m.set(70, "301");
            m.recalcBitMap();

            connector.send(m);
            ISOMsg response = connector.receive();

            System.out.println("Response MTI: " + response.getMTI());
            System.out.println("Field 39   : " + response.getString(39));
        } finally {
            connector.disconnect();
        }
    }
}
```

Notes:

- This example matches [`appiso/src/main/java/com/base/app/App.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/src/main/java/com/base/app/App.java)
- `SimpleLogListener` writes connector logs to standard output
- `NCCConnector` sends the length using BCD
- If a 5-byte TPDU is present and `tpduSwap` is enabled, the connector swaps source/destination address bytes before sending

## Alternative: `BCDConnector`

`BCDConnector` is also available in the jar for a very similar framed transport:

- 2-byte BCD length
- optional TPDU/header
- packed ISO message

Example client:

```java
import com.base.iso.connector.BCDConnector;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;

public class Main {
    public static void main(String[] args) throws Exception {
        ISO87BPackager packager = new ISO87BPackager();

        byte[] tpdu = new byte[] {
            0x60, 0x00, 0x00, 0x00, 0x00
        };

        BCDConnector channel = new BCDConnector("127.0.0.1", 5000, packager, tpdu);
        channel.connect();

        try {
            ISOMsg request = new ISOMsg();
            request.setPackager(packager);
            request.setMTI("0800");
            request.set(3, "990000");
            request.set(11, "000001");
            request.set(41, "TERMID01");
            request.set(70, "001");

            channel.send(request);

            ISOMsg response = channel.receive();
            System.out.println("Response MTI: " + response.getMTI());
            System.out.println("Field 39   : " + response.getString(39));
        } finally {
            channel.disconnect();
        }
    }
}
```

Notes:

- `channel.send(request)` packs the message automatically using the assigned packager
- `channel.receive()` returns an unpacked `ISOMsg`
- `BCDConnector` writes the message length as 2-byte BCD
- The connector can also send a pre-packed body using `sendBcd(byte[] b, byte[] TPDU)`

## Working with headers

You can attach a header directly to the message:

```java
msg.setHeader(new byte[] { 0x60, 0x00, 0x00, 0x00, 0x00 });
```

Or configure the connector header:

```java
channel.setHeader(new byte[] { 0x60, 0x00, 0x00, 0x00, 0x00 });
```

For `BCDConnector`, if a message header exists and has length 5, the implementation swaps source and destination address bytes before sending. That behavior is built into `sendMessageHeader`.

## Useful helper methods

`ISOUtil` is the main utility class:

- `ISOUtil.hexString(byte[])`: bytes to hex
- `ISOUtil.hex2byte(String)`: hex to bytes
- `ISOUtil.hexdump(byte[])`: formatted dump
- `ISOUtil.str2bcd(String, boolean)`: string to BCD
- `ISOUtil.bcd2str(byte[], int, int, boolean)`: BCD to string

Example:

```java
System.out.println(ISOUtil.hexString(msg.pack()));
System.out.println(ISOUtil.hexdump(msg.pack()));
```

## Common mistakes

- No packager assigned before `set(...)` or `pack()`
- Wrong packager for the host specification
- Using the jar alone when your code also needs Jackson-backed `GenericJSONPackager`
- Running code from the wrong working directory when using `loadFromJsonFile("cfg/iso87binary.json")`
- Sending a message with one field definition and parsing it with another
- Treating binary fields as plain text instead of hex or raw bytes
- Forgetting that TCP transport framing is separate from ISO 8583 field packing

## Minimal checklist

1. Use Java 11+
2. Add [`iso8583.jar`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/build/libs/iso8583.jar) to your classpath
3. Choose the correct packager
4. Create `ISOMsg`, set packager, MTI, and fields
5. Call `pack()` for raw ISO bytes or use a connector such as `BCDConnector`
6. Add external dependencies if you use features that require them

## Source references

The guidance above is based on these project files:

- [`settings.gradle`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/settings.gradle)
- [`app/build.gradle`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/build.gradle)
- [`appiso/build.gradle`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/build.gradle)
- [`appiso/src/main/java/com/base/app/App.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/src/main/java/com/base/app/App.java)
- [`appiso/cfg/iso87binary.json`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/appiso/cfg/iso87binary.json)
- [`app/src/main/java/com/base/iso/core/ISOMsg.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/core/ISOMsg.java)
- [`app/src/main/java/com/base/iso/packager/ISO87BPackager.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/packager/ISO87BPackager.java)
- [`app/src/main/java/com/base/iso/packager/ISO93BPackager.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/packager/ISO93BPackager.java)
- [`app/src/main/java/com/base/iso/packager/GenericJSONPackager.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/packager/GenericJSONPackager.java)
- [`app/src/main/java/com/base/iso/packager/aBasePackager.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/packager/aBasePackager.java)
- [`app/src/main/java/com/base/iso/connector/BaseConnector.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/connector/BaseConnector.java)
- [`app/src/main/java/com/base/iso/connector/NCCConnector.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/connector/NCCConnector.java)
- [`app/src/main/java/com/base/iso/connector/BCDConnector.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/connector/BCDConnector.java)
- [`app/src/main/java/com/base/iso/util/ISOUtil.java`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/java/com/base/iso/util/ISOUtil.java)
- [`app/src/main/resources/com/base/packager/iso87binary.json`](/c:/Git/thebase/Iso8583-lib/Iso8583-lib/app/src/main/resources/com/base/packager/iso87binary.json)
