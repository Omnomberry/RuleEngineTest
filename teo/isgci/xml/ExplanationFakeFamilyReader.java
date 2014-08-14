package teo.isgci.xml;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xml.sax.Locator;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import org.xml.sax.Attributes;

public class ExplanationFakeFamilyReader extends DefaultHandler {

    private Locator locator;
    private TreeMap<String, String> explMap;
    private TreeMap<String, Set<String>> fakeMap;
    private String current;
    private String currentCompl;
    private String currentExpl;
    private Pattern pattern;
    private Matcher matcher;
    private StringBuilder contents;
    private Set<String> alias;

    public ExplanationFakeFamilyReader() {
        super();
        explMap = new TreeMap<String, String>();
        fakeMap = new TreeMap<String, Set<String>>();
        current = null;
        currentCompl = null;
        currentExpl = null;
        alias = new HashSet<String>();
        //used to get rid of <xfig>...</xfig>
        pattern = Pattern.compile("<p>(.*?)</p>", Pattern.DOTALL);
    }

    // //////////////////////////////////////////////////////////////////
    // Event handlers.
    // //////////////////////////////////////////////////////////////////

    public void setDocumentLocator(Locator l) {
        locator = l;
    }

    public void startDocument() {
        contents = new StringBuilder();
    }

    public void endDocument() {
    }

    public TreeMap<String, String> getExplMap() {
        return explMap;
    }
    
    public TreeMap<String, Set<String>> getFakeMap() {
        return fakeMap;
    }

    /** ContentHandler Interface */
    public void startElement(String uri, String locName, String qName,
            Attributes atts) throws SAXException {
        try {
            if(qName.equals(SmallGraphTags.ALIAS)){
                alias.add(atts.getValue("name"));
            }
            // gets name of smallgraph
            if (qName.equals(SmallGraphTags.SIMPLE)
                    || qName.equals(SmallGraphTags.CONFIGURATION)
                    || qName.equals(SmallGraphTags.FAMILY)
                    || qName.equals("fakefamily")) {
                current = atts.getValue(SmallGraphTags.NAME);
            }
            // gets name of smallgraph
            if (qName.equals(SmallGraphTags.COMPLEMENT)) {
                currentCompl = atts.getValue(SmallGraphTags.NAME);
                if (currentExpl != null) 
                    explMap.put(current, refactorNote(currentExpl));
                currentExpl = null;
            }
            // if <expl> tag starts read in all characters
            if (qName.equals(SmallGraphTags.EXPL)) {
                contents.setLength(0);
            }

        } catch (Exception e) {
            String s = "Line " + Integer.toString(locator.getLineNumber())
                    + "\nColumn "
                    + Integer.toString(locator.getColumnNumber()) + "\nId "
                    + qName + "\n" + e.toString();
            throw new SAXException(s);
        }
    }

    public void characters(char[] ch, int start, int len) {
        contents.append(ch, start, len);
    }

    /** ContentHandler Interface */
    public void endElement(String uri, String locName, String qName)
            throws SAXException {
        
        //On close add current Fakefamily to FakeMap
        if (qName.equals("fakefamily")){
            fakeMap.put(current, alias);
        }
        
        // On close map explanation to smallgraph
        if (qName.equals(SmallGraphTags.SIMPLE)
                || qName.equals(SmallGraphTags.CONFIGURATION)
                || qName.equals(SmallGraphTags.FAMILY)
                || qName.equals("fakefamily")) {
            if (currentExpl != null)
                explMap.put(current, refactorNote(currentExpl));
            current = null;
            currentExpl = null;
            alias = new HashSet<String>();
        }
       
        // On close map explanation to smallgraph
        if (qName.equals(SmallGraphTags.COMPLEMENT)) {
            if (currentExpl != null)
                explMap.put(currentCompl, refactorNote(currentExpl));
            currentCompl = null;
            currentExpl = null;
        }

        // On <Expl> close write contents to Explanation
        if (qName.equals(SmallGraphTags.EXPL)) {
            matcher = pattern.matcher(contents.toString());
            while(matcher.find()){
                if (currentExpl == null)
                    currentExpl = matcher.group();
                else
                    currentExpl += matcher.group();
            }
                
        }

    }

    /**
     * Removes all <smallgraph> and </smallgraph> from the String. Also replaces
     * gcIDs with their names, if we have one for them.
     * 
     * @param input
     * @return
     */
    private String refactorNote(String input) {

        String output = input;

        if (output.contains("<smallgraph>")) {
            ArrayList<Integer> begin = new ArrayList<Integer>();
            ArrayList<Integer> end = new ArrayList<Integer>();
            // find all occurrences forward
            for (int i = -1; (i = output.indexOf("<smallgraph>", i + 1)) != -1;) {
                begin.add(i);
            }
            for (int i = -1; (i = output.indexOf("</smallgraph>", i + 1)) != -1;) {
                end.add(i);
            }
            // we iterate backwards because otherwise we mess up the values of
            // begin and end arrayLists
            for (int i = begin.size() - 1; i >= 0; i--) {
                // get the front part without <smallgraph>
                String frontTemp = output.substring(0, begin.get(i));
                // get the back part without </smallgraph>
                String backTemp = output.substring(end.get(i)
                        + "</smallgraph>".length(), output.length());
                // get the SGname
                String sgIDTemp = output.substring(begin.get(i)
                        + "<smallgraph>".length(), end.get(i));
                // We remove all $ so we can generally add them in the next
                // step
                sgIDTemp = sgIDTemp.replace("$", "");
                sgIDTemp = "[[Smallgraphs#" + sgIDTemp + " | $" + sgIDTemp
                        + "$]]";

                output = frontTemp + sgIDTemp + backTemp;

            }
        }

        // Reformat all the references
        output = output.replace("<ref>ref_", "[ [[Reference:");
        output = output.replace("</ref>", " |]] ]");

        // replace <br> </br> \n or any whitespace with a single space
        output = output.replace("<br>", " ");
        output = output.replace("</br>", " ");
        output = output.replace("\n", " ");
        output = output.replaceAll("\\s+", " ");

        return output;
    }
}

/* EOF */
