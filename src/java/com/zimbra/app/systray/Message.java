package com.zimbra.app.systray;

import com.zimbra.app.soap.messages.SearchResponse;

public class Message {

    public Message(Account account, SearchResponse.Message msg) {
        this.account = account;
        id = msg.id;
        fragment = msg.fragment;
        subject = msg.subject;
        fullName = msg.sender.fullName;
        emailAddress = msg.sender.emailAddress;
    }

    private Account account;
    public Account getAccount() { return account; }

    private int id;
    public int getId() { return id; }

    private String fragment;
    public String getFragment() { return fragment; }

    private String subject;
    public String getSubject() { return subject; }

    private String fullName;
    public String getSenderName() { return fullName; }

    private String emailAddress;
    public String getSenderAddress() { return emailAddress; }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object other) {
        boolean equals = false;
        if (other instanceof Message) {
            equals = ((Message) other).id == id;
        }
        return equals;
    }
}
