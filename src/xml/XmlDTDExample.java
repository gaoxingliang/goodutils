package xml;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

/**
 * 这里展示了DTD标签会导致URLConnecttion被建立
 */
public class XmlDTDExample {
    public static void main(String[] args) throws Exception {

        String t = "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict" +
                ".dtd\"><html xmlns=\"http://www.w3.org/1999/xhtml\">\\u000d\\u000a<head>HelloWorld</head>\\u000d\\u000a</html>\n";
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setValidating(false);
        dbf.setNamespaceAware(false);

        /**
         * 注释下面的2行会发现  会卡住, 而且是卡在socket read
         */
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
        dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

        InputSource inStream = new InputSource();
        inStream.setCharacterStream(new StringReader(t));
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(inStream);
        System.out.println(doc.getElementsByTagName("head").item(0).getChildNodes().item(0).getNodeValue());

    }
}
