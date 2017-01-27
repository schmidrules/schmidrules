package org.schmidrules.configuration;

import java.io.Reader;
import java.io.StringWriter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.schmidrules.dependency.Location;

final class XmlUtil {

    private XmlUtil() {
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(final Reader reader, final Class<T> dtoClass) {
        LocationListener listener = null;
        try {
            Unmarshaller unmarshaller = XmlUtil.createUnmarshaller(dtoClass);
            XMLInputFactory xif = XMLInputFactory.newFactory();
            XMLStreamReader xsr = xif.createXMLStreamReader(reader);
            listener = new LocationListener(xsr);
            unmarshaller.setListener(listener);
            return (T) unmarshaller.unmarshal(xsr);
        } catch (Exception e) {
            throw new IllegalStateException("Error unmarshalling xml type " + dtoClass.getName(), e);
        } finally {
            if (listener != null) {
                listener.clear();
            }
        }
    }

    private static Unmarshaller createUnmarshaller(final Class<?> dtoClass) {
        try {
            JAXBContext context = JAXBContext.newInstance(dtoClass);
            Unmarshaller u = context.createUnmarshaller();
            u.setEventHandler(new DefaultValidationEventHandler());
            return u;
        } catch (JAXBException e) {
            throw new IllegalStateException("Error creating JAXB unmarshaller", e);
        }
    }

    public static <T> String marshal(final T dtoObject) {
        Marshaller marshaller = XmlUtil.createMarshaller(dtoObject.getClass());
        try {
            StringWriter writer = new StringWriter();
            marshaller.marshal(dtoObject, writer);
            return writer.toString();
        } catch (JAXBException e) {
            throw new IllegalArgumentException("Error marshalling xml type " + dtoObject.getClass().getName(), e);
        }
    }

    private static Marshaller createMarshaller(final Class<?> dtoClass) {
        try {
            JAXBContext context = JAXBContext.newInstance(dtoClass);
            Marshaller m = context.createMarshaller();
            m.setProperty("jaxb.encoding", "UTF-8"); // actually implicit
            m.setProperty("jaxb.formatted.output", true);
            return m;
        } catch (JAXBException e) {
            throw new IllegalStateException("Error creating JAXB marshaller", e);
        }
    }

    public static Location toLocation(javax.xml.stream.Location l, int length) {
        if (l == null) {
            return null;
        }

        return new Location(l.getLineNumber(), l.getCharacterOffset() - length, 0, length);
    }
}
