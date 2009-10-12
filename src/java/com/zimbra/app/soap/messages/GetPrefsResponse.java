package com.zimbra.app.soap.messages;

import com.zimbra.app.soap.Element;

import java.util.ArrayList;

@Element(ns="urn:zimbraAccount")
public class GetPrefsResponse {

    @Element(name="pref", optional=false)
    public ArrayList<GetPrefsRequest.Pref> prefs =
            new ArrayList<GetPrefsRequest.Pref>();
}
