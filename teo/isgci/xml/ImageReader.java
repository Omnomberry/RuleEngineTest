package teo.isgci.xml;

import java.util.TreeMap;
import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

public class ImageReader extends DefaultHandler {

    private Locator locator;
    private TreeMap<String, String> imageMap;

    public ImageReader() {
        super();
        imageMap = new TreeMap<String, String>();
    }

    // //////////////////////////////////////////////////////////////////
    // Event handlers.
    // //////////////////////////////////////////////////////////////////

    public void setDocumentLocator(Locator l) {
        locator = l;
    }

    public void startDocument() {
    }

    public void endDocument() {
    }

    public TreeMap<String, String> getImageMap() {
        return imageMap;
    }

    /** ContentHandler Interface */
    public void startElement(String uri, String locName, String qName,
            Attributes atts) throws SAXException {
        try {
            // gets imagename for given smallgraph
            if (qName.equals(SmallGraphTags.LINK)) {
                String name = atts.getValue("name");
                String img = atts.getValue("img");
                if (img != null) {
                    img = img.replace("/images/", "");

                    if (!imageMap.containsKey(name))
                        imageMap.put(name, img);
                }
            }
        } catch (Exception e) {
            String s = "Line " + Integer.toString(locator.getLineNumber())
                    + "\nColumn "
                    + Integer.toString(locator.getColumnNumber()) + "\nId "
                    + qName + "\n" + e.toString();
            throw new SAXException(s);
        }
    }

    /** ContentHandler Interface */
    public void endElement(String uri, String locName, String qName)
            throws SAXException {
    }

}

/* EOF */
