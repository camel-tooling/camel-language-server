package com.github.cameltooling.lsp.internal.parser;

import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class NoopErrorHandler implements ErrorHandler {

    @Override
    public void warning(SAXParseException exception) throws SAXException {
        // do not print errors to std err, invalid xml is expected
    }

    @Override
    public void error(SAXParseException exception) throws SAXException {
        // do not print errors to std err, invalid xml is expected
    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {
        // do not print errors to std err, invalid xml is expected
    }
}
