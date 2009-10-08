package com.zimbra.app.soap;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

public class Marshaller {

    public static void marshal(SOAPElement parent, Object o)
    throws SOAPException {
        if (o == null) return;
        Class<?> c = o.getClass();
        Element anno = c.getAnnotation(Element.class);
        if (anno == null) return;

        SOAPElement child = null;
        if (!"".equals(anno.ns())) {
            child = parent.addChildElement(
                    "".equals(anno.name()) ? c.getSimpleName() : anno.name(),
                    "", anno.ns());
        } else {
            child = parent.addChildElement("".equals(anno.name()) ?
                    c.getSimpleName() : anno.name(), "");
        }

        Field[] fields = c.getDeclaredFields();
        for (Field f : fields) {
            anno = f.getAnnotation(Element.class);
            if (anno == null) continue;
            f.setAccessible(true);

            try {
                Class<?> type = f.getType();
                if (String.class == type || type.isPrimitive()) {
                    if (anno.type() == Type.TEXT && "".equals(anno.name())) {
                        child.addTextNode(f.get(o).toString());
                    } else if (anno.type() == Type.ATTRIBUTE) {
                        child.setAttribute("".equals(anno.name()) ?
                                f.getName() : anno.name(),
                                f.get(o).toString());
                    } else {
                        SOAPElement ce = child.addChildElement(
                                "".equals(anno.name()) ?
                                        f.getName() : anno.name(), "");
                        ce.addTextNode(f.get(o).toString());
                    }
                } else if (type.isAssignableFrom(Collection.class)) {
                    // TODO handle collection types,
                    //     child elements can either be TEXT or a class
                    // text is  <name>text1</name><name>text2</name>
                    // class is <name><data/></name><name><data2/></name>
                } else {
                    marshal(child, f.get(o));
                }
            }
            catch (IllegalAccessException e) { } // ignore
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unmarshal(Class<T> clz, SOAPMessage m)
    throws SOAPException {
        SOAPBody body = m.getSOAPPart().getEnvelope().getBody();

        Element anno = clz.getAnnotation(Element.class);
        if (anno == null) return null;
        String ns = anno.ns();

        Iterator<SOAPElement> i = (Iterator<SOAPElement>)
                body.getChildElements();

        if (!i.hasNext()) throw new IllegalStateException("No request body");

        SOAPElement e = i.next();

        if (i.hasNext())
            throw new IllegalStateException(
                    "More than one response part: " + i.next());

        String eName = e.getElementName().getLocalName();
        if (!eName.equals(anno.name()) && !eName.equals(clz.getSimpleName()))
            throw new IllegalStateException("Unrecognized name: " + eName);

        try {
            T t = clz.newInstance();

            Field[] fields = clz.getDeclaredFields();
            for (Field f : fields) {
                anno = f.getAnnotation(Element.class);
                if (anno == null) continue;
                String name = "".equals(anno.name()) ?
                        f.getName() : anno.name();
                Iterator<SOAPElement> children = (Iterator<SOAPElement>)
                        e.getChildElements(new QName(ns, name));
                if (!children.hasNext()) {
                    throw new IllegalStateException("missing: " + name);
                }
                SOAPElement elem = children.next();
                if (f.getType().isPrimitive()) {
                } else if (f.getType().isAssignableFrom(Collection.class)) {
                } else if (f.getType() == String.class) {
                    f.set(t, elem.getValue());
                } else { // TODO an object
                }
            }
            return t;
        }
        catch (InstantiationException ex) {
            throw new IllegalStateException(ex);
        }
        catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
