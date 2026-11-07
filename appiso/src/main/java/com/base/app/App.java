package com.base.app;

import com.base.iso.connector.NCCConnector;
import com.base.iso.core.ISOMsg;
import com.base.iso.packager.ISO87BPackager;
import com.base.iso.util.log.Logger;
import com.base.iso.util.log.ProtectLogListener;
import com.base.iso.util.log.SimpleLogListener;

/**
 * Sample client: sends a network-management 0800 (echo test) over NCC framing and prints the reply.
 *
 * Usage: {@code App [host] [port]} — defaults to localhost 8000. Point it only at a test host.
 *
 * No files are read: the layout is the code-defined ISO87BPackager. To use a JSON layout instead,
 * read it yourself and call {@code new GenericJSONPackager().setFieldsFromJson(jsonString)}.
 */
public class App {
    public static void main(String[] args) throws Exception {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 8000;

        // ProtectLogListener FIRST so card data is masked before anything is printed.
        Logger logger = new Logger();
        logger.addListener(new ProtectLogListener());
        logger.addListener(new SimpleLogListener(System.out));

        ISO87BPackager packager = new ISO87BPackager();
        NCCConnector connector = new NCCConnector(host, port, packager, null);
        connector.setLogger(logger, "test-channel");
        connector.connect(); // throws if the host refuses the connection

        try {
            // Set the packager BEFORE values so binary fields are parsed as hex.
            ISOMsg m = new ISOMsg();
            m.setPackager(packager);
            m.setMTI("0800");
            m.set(11, "000001");
            m.set(41, "00000001");
            m.set(70, "301");
            connector.send(m);

            ISOMsg r = connector.receive();
            System.out.println("reply MTI=" + r.getMTI() + " F39=" + r.getString(39));
        } finally {
            connector.disconnect();
        }
    }
}
