package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;

@Element(ns="urn:zimbraMail")
public class CreateTagResponse {

    @Element(name="tag", optional=false)
    public GetTagResponse.Tag tag;
}
