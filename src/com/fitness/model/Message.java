package com.fitness.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Message {
    private int id;
    private int senderId;
    private String senderName;
    private String senderRole;
    private int receiverId;
    private String receiverName;
    private String receiverRole;
    private String messageText;
    private String sentAt;
    private int isRead;

    public Message() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSenderId() { return senderId; }
    public void setSenderId(int senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getSenderRole() { return senderRole; }
    public void setSenderRole(String senderRole) { this.senderRole = senderRole; }

    public int getReceiverId() { return receiverId; }
    public void setReceiverId(int receiverId) { this.receiverId = receiverId; }

    public String getReceiverName() { return receiverName; }
    public void setReceiverName(String receiverName) { this.receiverName = receiverName; }

    public String getReceiverRole() { return receiverRole; }
    public void setReceiverRole(String receiverRole) { this.receiverRole = receiverRole; }

    public String getMessageText() { return messageText; }
    public void setMessageText(String messageText) { this.messageText = messageText; }

    public String getSentAt() { return sentAt; }
    public void setSentAt(String sentAt) { this.sentAt = sentAt; }

    public int getIsRead() { return isRead; }
    public void setIsRead(int isRead) { this.isRead = isRead; }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", id);
        map.put("sender_id", senderId);
        map.put("sender_name", senderName);
        map.put("sender_role", senderRole);
        map.put("receiver_id", receiverId);
        map.put("receiver_name", receiverName);
        map.put("receiver_role", receiverRole);
        map.put("message_text", messageText);
        map.put("sent_at", sentAt);
        map.put("is_read", isRead);
        return map;
    }
}
