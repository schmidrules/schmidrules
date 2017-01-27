package org.schmidrules.configuration;

import javax.xml.bind.Unmarshaller.Listener;
import javax.xml.stream.XMLStreamReader;

import org.schmidrules.dependency.Location;

/**
 * Don't try this at home. Will fail horribly if different Threads parse. Will leak memory if someone forgets to clear.
 */
public class LocationListener extends Listener {

    private static ThreadLocal<XMLStreamReader> tlxsr = new ThreadLocal<>();

    public LocationListener(XMLStreamReader xsr) {
        tlxsr.set(xsr);
    }

    @Override
    public void beforeUnmarshal(Object target, Object parent) {
        // do nothing
    }

    public static Location getLocation(int length) {
        XMLStreamReader xsr = tlxsr.get();

        if (xsr == null) {
            return null; // may stop working at any time
        }

        return XmlUtil.toLocation(xsr.getLocation(), length);
    }

    public void clear() {
        tlxsr.remove();
    }
}