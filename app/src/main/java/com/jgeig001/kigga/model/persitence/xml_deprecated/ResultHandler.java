package com.jgeig001.kigga.model.persitence.xml_deprecated;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

@Deprecated()
public class ResultHandler extends DefaultHandler {

    public ResultHandler() {
    }

    public void startElement(String uri, String localName,
                             String qName, Attributes attributes)
            throws SAXException {
        // no op
        // >>> "MatchIsFinished" <<<
    }

    public void characters(char ch[], int start, int length)
            throws SAXException {
        // no op
    }

    public void endElement(String uri, String localName, String qName)
            throws SAXException {
        // no op
    }

}
