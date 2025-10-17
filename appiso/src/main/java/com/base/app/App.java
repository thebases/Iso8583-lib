package com.base.app;

import java.io.IOException;

import com.base.iso.connector.NCCConnector;
import com.base.iso.core.ISOMsg;
import com.base.iso.interfaces.log.iLogSource;
import com.base.iso.packager.GenericJSONPackager;
import com.base.iso.util.log.Logger;
import com.base.iso.util.log.SimpleLogListener;

public class App {
    public static void main(String[] args) throws IOException, Exception {

        Logger logger = new Logger();
        logger.addListener(new SimpleLogListener(System.out));
        System.out.println("Working directory: " + System.getProperty("user.dir"));

        GenericJSONPackager packager = new GenericJSONPackager();
        packager.loadFromJsonFile("cfg/iso87binary.json");
        NCCConnector connector = new NCCConnector(
                "62.146.233.40", 8000, packager, null);

        ((iLogSource) connector).setLogger(logger, "test-channel");
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

    }
}
