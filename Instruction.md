# Instruction for Implementing `iso8583.jar`

## 1. About the library

`iso8583.jar` is a Java library for building, packing, unpacking, sending, and receiving ISO 8583 messages.

The library supports:

- Creating ISO 8583 messages
- Defining message structure with built-in or JSON-based packagers
- Packing messages into byte arrays
- Unpacking byte arrays into ISO 8583 fields
- Sending and receiving messages over TCP connectors
- Working with binary fields, TPDU headers, and transport framing

## 2. Library architecture

The library is included 5 layers:

- Message layer:  `ISOMsg`, `ISOField`, `BinaryFieldComponent`
- Packager layer: `ISO87BPackager`, `ISO93BPackager`, `GenericJSONPackager`
- Connector layer: `BCDConnector`, `NCCConnector`, `NACConnector`, `HEXConnector`
- Utility and logging layer: `ISOUtil`, `Logger`, `SimpleLogListener`
- Field-definition layer classes such as `IFB_NUMERIC`, `IFB_LLNUM`, `IFB_LLLCHAR`, `IFB_BINARY`

For most integrations, users only work directly with the first 4 layers.

## 3. Main entities and classes

This section explains the main classes a user needs to understand in order to implement the jar correctly.

## 3.1 Core message classes

### `com.base.iso.core.ISOMsg`

Purpose:

- The main ISO 8583 message object
- Holds MTI, bitmap, and fields
- Packs and unpacks using an assigned packager

What users do with it:

- Create a message
- Set MTI
- Set ISO fields
- Pack to bytes
- Unpack response bytes

Main methods:

- `setMTI(String)`
- `set(int, String)`
- `set(int, byte[])`
- `setPackager(iPackager)`
- `pack()`
- `unpack(byte[])`
- `getString(int)`
- `hasField(int)`

Example:

```java
ISOMsg msg = new ISOMsg();
msg.setPackager(packager);
msg.setMTI("0200");
msg.set(3, "000000");
msg.set(11, "123456");
```

### `com.base.iso.core.ISOField`

Purpose:

- Represents a normal field containing a text value

Typical use:

- Usually not created directly by application code
- Created internally when `ISOMsg.set(fieldNo, value)` is called

### `com.base.iso.core.BinaryFieldComponent`

Purpose:

- Represents a binary field such as PIN, MAC, EMV, or raw binary content

Typical use:

- Usually handled through `ISOMsg.set(int, byte[])`
- Can also be triggered by `ISOMsg.set(int, String)` when the packager marks that field as binary

## 3.2 Packager classes

### `com.base.iso.packager.ISO87BPackager`

Purpose:

- Built-in ISO 8583:1987 binary packager

Use when:

- Your host uses the ISO 8583:1987 binary field definition

### `com.base.iso.packager.ISO93BPackager`

Purpose:

- Built-in ISO 8583:1993 binary packager

Use when:

- Your host uses the ISO 8583:1993 binary field definition

### `com.base.iso.packager.GenericJSONPackager`

Purpose:

- Dynamic packager that loads field definitions from JSON

Use when:

- Your host has a custom ISO 8583 field definition
- You want to manage field layouts outside Java code

Main methods:

- `loadFromJsonFile(String filePath)`
- `setFieldsFromJson(String jsonString)`
- `printFields()`

### `com.base.iso.packager.aBasePackager`

Purpose:

- Base implementation used by all concrete packagers

Typical use:

- Not used directly by business code
- Important because it provides the actual `pack()` and `unpack()` behavior behind all packagers

## 3.3 Connector classes

Connector classes are responsible for TCP transport. They do not define the ISO fields. They define how packed bytes are framed and sent.

### `com.base.iso.connector.NCCConnector`

Purpose:

- Sends and receives ISO messages using:
  - 2-byte BCD length
  - optional TPDU/header
  - packed ISO payload

Main methods:

- `connect()`
- `send(ISOMsg)`
- `receive()`
- `disconnect()`

### `com.base.iso.connector.BCDConnector`

Purpose:

- Similar to `NCCConnector`
- Uses BCD-style message framing

Use when:

- Your host expects BCD length framing and optional TPDU

### `com.base.iso.connector.NACConnector`

Purpose:

- Alternative connector for another framing style

Use when:

- The host specification explicitly requires NAC transport

### `com.base.iso.connector.HEXConnector`

Purpose:

- Alternative connector for hosts that frame data differently

Use when:

- Your host specification requires HEX-oriented framing

### `com.base.iso.connector.BaseConnector`

Purpose:

- Base connector implementation
- Handles socket lifecycle, send, receive, timeouts, header handling, and filters

Typical use:

- Not used directly in most business implementations
- Important as the transport base class for all connectors or make your owned connector

## 3.4 Utility and logging classes

### `com.base.iso.util.ISOUtil`

Purpose:

- Utility helper for hex, BCD, byte arrays, dumps, and formatting

Useful methods:

- `hexString(byte[])`
- `hex2byte(String)`
- `hexdump(byte[])`
- `str2bcd(String, boolean)`
- `bcd2str(byte[], int, int, boolean)`

### `com.base.iso.util.log.Logger`

Purpose:

- Central logger for connector and packager events


## 3.5 Field-definition classes

These classes define how a single ISO field is encoded.

Examples:

- `IFB_NUMERIC`
- `IFB_LLNUM`
- `IFB_LLLNUM`
- `IFB_LLCHAR`
- `IFB_LLLCHAR`
- `IFB_BINARY`
- `IFB_BITMAP`
- `IF_CHAR`

What they mean:

- `NUMERIC`: fixed-length numeric field
- `LLNUM`: variable numeric field with 2-digit length prefix
- `LLLNUM`: variable numeric field with 3-digit length prefix
- `LLCHAR`: variable character field with 2-digit length prefix
- `LLLCHAR`: variable character field with 3-digit length prefix
- `BINARY`: binary field
- `BITMAP`: bitmap field

Typical use:

- Most application teams do not instantiate these classes directly
- They are usually declared by built-in packagers or loaded through JSON with `GenericJSONPackager`

## 4. How to use the jar file

## 4.1 Prerequisites

- Java 11 or later
- If using `GenericJSONPackager`, add Jackson dependencies

## 4.2 Add the jar to your project 

### Gradle

```gradle
dependencies {
    implementation files("./iso8583.jar")
    implementation "com.fasterxml.jackson.core:jackson-core:2.17.1"
    implementation "com.fasterxml.jackson.core:jackson-databind:2.17.1"
}
```

## 4.3 Implementation steps

A normal integration should follow these steps:

1. Add the jar to the project
2. Choose a packager
3. Create an `ISOMsg`
4. Set MTI and required fields
5. Connect to the host using the required connector
6. Send the request
7. Receive and parse the response
8. Handle logging, response code, and errors

## 5. Example code

## 5.1 Example 1: pack and unpack locally

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
        System.out.println("HEX: " + ISOUtil.hexString(packed));

        ISOMsg response = new ISOMsg();
        response.setPackager(packager);
        response.unpack(packed);

        System.out.println("MTI: " + response.getMTI());
        System.out.println("F11: " + response.getString(11));
    }
}
```

## 5.2 Example 2: use `GenericJSONPackager`

```java
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.GenericJSONPackager;

public class Main {
    public static void main(String[] args) throws Exception {
        GenericJSONPackager packager = new GenericJSONPackager();
        packager.loadFromJsonFile("iso87binary.json");

        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI("0800");
        msg.set(11, "000001");
        msg.set(41, "00000001");
        msg.set(70, "301");

        byte[] packed = msg.pack();
        System.out.println(packed.length);
    }
}
```