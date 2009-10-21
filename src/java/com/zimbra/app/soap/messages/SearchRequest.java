package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbraMail")
public class SearchRequest {

    @Element(name="calExpandInstStart", type=Type.ATTRIBUTE, ignore="-1")
    public long calendarSearchStartTime = -1;

    @Element(name="calExpandInstEnd", type=Type.ATTRIBUTE, ignore="-1")
    public long calendarSearchEndTime = -1;

    /**
     * appointment or message
     */
    @Element(name="types", optional=false, type=Type.ATTRIBUTE)
    public String type;

    /**
     * dateAsc, dateDesc, etc.
     */
    @Element(type=Type.ATTRIBUTE)
    public String sortBy;

    @Element(name="query", type=Type.TEXT)
    public String query;
}
