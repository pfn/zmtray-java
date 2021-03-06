package com.zimbra.app.soap.messages;

import java.util.ArrayList;

import com.zimbra.app.soap.Element;

@Element(ns="urn:zimbra")
public class BatchResponse {

    @Element(name="SearchResponse", ns="urn:zimbraMail")
    public ArrayList<SearchResponse> searchResponses =
            new ArrayList<SearchResponse>();

    @Element(name="GetInfoResponse", ns="urn:zimbraAccount")
    public GetInfoResponse infoResponse;

    @Element(name="GetFolderResponse", ns="urn:zimbraMail")
    public GetFolderResponse folderResponse;

    @Element(name="GetPrefsResponse", ns="urn:zimbraAccount")
    public GetPrefsResponse prefsResponse;

}
