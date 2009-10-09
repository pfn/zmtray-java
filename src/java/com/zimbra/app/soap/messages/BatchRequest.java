package com.zimbra.app.soap.messages;

import java.util.ArrayList;

import com.zimbra.app.soap.Element;
import com.zimbra.app.soap.Type;

@Element(ns="urn:zimbra")
public class BatchRequest {

    @Element(name="SearchRequest")
    public ArrayList<SearchRequest> searchRequests =
            new ArrayList<SearchRequest>();

    @Element(name="GetInfoRequest")
    public GetInfoRequest infoRequest;

    @Element(name="GetFolderRequest")
    public GetFolderRequest folderRequest;

    @Element(name="GetPrefsRequest")
    public GetPrefsRequest prefsRequest;

}
