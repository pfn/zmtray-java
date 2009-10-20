package com.zimbra.app.soap;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Iterator;
import java.beans.PropertyEditorManager;
import java.beans.PropertyEditor;

import javax.xml.namespace.QName;
import javax.xml.soap.SOAPElement;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPMessage;
import javax.xml.soap.SOAPException;

public class Marshaller {

    @SuppressWarnings("unchecked")
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
                Object value = f.get(o);
                if (value == null) {
                    if (anno.optional()) continue;
                    throw new IllegalArgumentException(String.format(
                            "%s.%s is not optional",
                            c.getSimpleName(), f.getName()));
                }
                if (String.class == type || type.isPrimitive()) {
                    if (anno.type() == Type.TEXT && "".equals(anno.name())) {
                        child.addTextNode(value.toString());
                    } else if (anno.type() == Type.ATTRIBUTE) {
                        child.setAttribute("".equals(anno.name()) ?
                                f.getName() : anno.name(),
                                value.toString());
                    } else {
                        SOAPElement ce = child.addChildElement(
                                "".equals(anno.name()) ?
                                        f.getName() : anno.name(), "");
                        ce.addTextNode(value.toString());
                    }
                } else if (Collection.class.isAssignableFrom(type)) {
                    ParameterizedType t = (ParameterizedType)
                                f.getGenericType();
                    Class<?> oType = (Class)
                            t.getActualTypeArguments()[0];
                    Collection col = (Collection) value;
                    for (Object obj : col) {
                        if (oType == String.class) {
                            SOAPElement ce = child.addChildElement(
                                    "".equals(anno.name()) ?
                                            f.getName() : anno.name(), "");
                            ce.addTextNode(value.toString());
                        } else if (Number.class.isAssignableFrom(oType) ||
                            oType == Character.class) {
                            throw new UnsupportedOperationException("" + oType);
                        } else {
                            marshal(child, obj);
                        }
                    }
                } else {
                    marshal(child, value);
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
        if (anno == null)
            throw new IllegalArgumentException(
                    clz + " is not annotated by @Element");
        String ns = anno.ns();

        Iterator<SOAPElement> i = (Iterator<SOAPElement>)
                body.getChildElements();

        if (!i.hasNext()) throw new IllegalStateException("No request body");

        SOAPElement e = i.next();

        if (i.hasNext())
            throw new IllegalStateException(
                    "More than one response part: " + i.next());

        String eName = e.getElementName().getLocalName();
        if ("Fault".equals(eName) && SOAPFaultException.class != clz) {
            throw unmarshal(SOAPFaultException.class, m);
        }
        if (!eName.equals(anno.name()) && !eName.equals(clz.getSimpleName()))
            throw new IllegalStateException("Unrecognized name: " + eName);

        return unmarshal(clz, ns, e);
    }

    @SuppressWarnings("unchecked")
    private static <T> T unmarshal(Class<T> clz, String ns, SOAPElement e)
    throws SOAPException {
        try {
            Element anno = null;
            T t = clz.newInstance();

            Field[] fields = clz.getDeclaredFields();
            for (Field f : fields) {
                Class<?> fType = f.getType();
                anno = f.getAnnotation(Element.class);
                if (!"".equals(anno.ns())) {
                    ns = anno.ns();
                }


                if (anno == null) continue;
                String name = "".equals(anno.name()) ?
                        f.getName() : anno.name();
                if (anno.type() == Type.ATTRIBUTE) {
                    if (!e.hasAttribute(name)) {
                        if (!anno.optional()) {
                            throw new IllegalArgumentException(String.format(
                                    "Missing required attribute %s.@%s",
                                    e.getElementName().getLocalName(), name));
                        }
                        continue;
                    }
                    String value = e.getAttribute(name);
                    if (fType.isPrimitive()) {
                        PropertyEditor editor =
                                PropertyEditorManager.findEditor(fType);
                        editor.setAsText(value);
                        f.set(t, editor.getValue());
                    } else {
                        f.set(t, value);
                    }
                } else {
                    if (anno.samenode() && anno.type() == Type.TEXT) {
                        String value = e.getValue();
                        if (value == null && !anno.optional()) {
                            throw new IllegalArgumentException(
                                    "missing string value: " + e);
                        }
                        f.set(t, value);
                        continue;
                    }
                    Iterator<SOAPElement> children = (Iterator<SOAPElement>)
                            e.getChildElements(new QName(ns, name));
                    if (!children.hasNext()) {
                        if (!anno.optional())
                            throw new IllegalArgumentException("missing: " +
                                    name + ": " +
                                    e.getElementName().getLocalName());
                        continue;
                    }
                    if (fType.isPrimitive()) {
                        PropertyEditor editor =
                                PropertyEditorManager.findEditor(fType);
                        editor.setAsText(children.next().getValue());
                        f.set(t, editor.getValue());
                    } else if (Collection.class.isAssignableFrom(fType)) {
                        ParameterizedType type = (ParameterizedType)
                                f.getGenericType();
                        Class<?> oType = (Class)
                                type.getActualTypeArguments()[0];
                        Collection c = (Collection) f.get(t);
                        while (children.hasNext()) {
                            if (oType == String.class) {
                                c.add(children.next().getValue());
                            } else {
                                c.add(unmarshal(oType, ns, children.next()));
                            }
                        }
                    } else if (fType == String.class) {
                        f.set(t, children.next().getValue());
                    } else {
                        f.set(t, unmarshal(fType, ns, children.next()));
                    }
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
