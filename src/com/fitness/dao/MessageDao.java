package com.fitness.dao;

import com.fitness.db.DatabaseManager;
import com.fitness.model.Message;

import java.util.*;

public class MessageDao {
    private final DatabaseManager db = DatabaseManager.getInstance();

    public long sendMessage(int senderId, int receiverId, String messageText) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, message_text, sent_at, is_read) " +
                "VALUES (?, ?, ?, CURRENT_TIMESTAMP, 0);";
        return db.executeInsert(sql, Arrays.asList(senderId, receiverId, messageText));
    }

    public List<Message> getConversation(int user1Id, int user2Id) {
        String sql = "SELECT m.*, " +
                "s.name as sender_name, s.role as sender_role, " +
                "r.name as receiver_name, r.role as receiver_role " +
                "FROM messages m " +
                "JOIN users s ON m.sender_id = s.id " +
                "JOIN users r ON m.receiver_id = r.id " +
                "WHERE (m.sender_id = ? AND m.receiver_id = ?) " +
                "   OR (m.sender_id = ? AND m.receiver_id = ?) " +
                "ORDER BY m.sent_at ASC, m.id ASC;";
        List<Map<String, Object>> rows = db.query(sql, Arrays.asList(user1Id, user2Id, user2Id, user1Id));
        List<Message> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            list.add(mapMessage(r));
        }

        // Mark incoming messages as read
        markConversationRead(user1Id, user2Id);
        return list;
    }

    public void markConversationRead(int readerId, int senderId) {
        String sql = "UPDATE messages SET is_read = 1 WHERE receiver_id = ? AND sender_id = ?;";
        db.executeUpdate(sql, Arrays.asList(readerId, senderId));
    }

    public List<Map<String, Object>> getRecentInteractions(int userId) {
        String sql = "SELECT " +
                "u.id as contact_id, u.name as contact_name, u.email as contact_email, u.role as contact_role, " +
                "m.message_text as last_message, m.sent_at as last_message_time, " +
                "(SELECT COUNT(*) FROM messages unread WHERE unread.receiver_id = ? AND unread.sender_id = u.id AND unread.is_read = 0) as unread_count " +
                "FROM users u " +
                "JOIN (" +
                "    SELECT MAX(id) as max_id, " +
                "           CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END as other_id " +
                "    FROM messages " +
                "    WHERE sender_id = ? OR receiver_id = ? " +
                "    GROUP BY other_id " +
                ") latest ON u.id = latest.other_id " +
                "JOIN messages m ON m.id = latest.max_id " +
                "ORDER BY m.sent_at DESC;";
        return db.query(sql, Arrays.asList(userId, userId, userId, userId));
    }

    public List<Map<String, Object>> getCoachInteractionHistory(int coachId) {
        String sql = "SELECT m.id, m.sent_at, m.message_text, " +
                "m.sender_id, s.name as sender_name, s.role as sender_role, " +
                "m.receiver_id, r.name as receiver_name, r.role as receiver_role " +
                "FROM messages m " +
                "JOIN users s ON m.sender_id = s.id " +
                "JOIN users r ON m.receiver_id = r.id " +
                "WHERE m.sender_id = ? OR m.receiver_id = ? " +
                "ORDER BY m.sent_at DESC LIMIT 50;";
        return db.query(sql, Arrays.asList(coachId, coachId));
    }

    private Message mapMessage(Map<String, Object> r) {
        Message m = new Message();
        if (r.get("id") instanceof Number) m.setId(((Number) r.get("id")).intValue());
        if (r.get("sender_id") instanceof Number) m.setSenderId(((Number) r.get("sender_id")).intValue());
        m.setSenderName(r.get("sender_name") != null ? r.get("sender_name").toString() : "");
        m.setSenderRole(r.get("sender_role") != null ? r.get("sender_role").toString() : "");
        if (r.get("receiver_id") instanceof Number) m.setReceiverId(((Number) r.get("receiver_id")).intValue());
        m.setReceiverName(r.get("receiver_name") != null ? r.get("receiver_name").toString() : "");
        m.setReceiverRole(r.get("receiver_role") != null ? r.get("receiver_role").toString() : "");
        m.setMessageText(r.get("message_text") != null ? r.get("message_text").toString() : "");
        m.setSentAt(r.get("sent_at") != null ? r.get("sent_at").toString() : "");
        if (r.get("is_read") instanceof Number) m.setIsRead(((Number) r.get("is_read")).intValue());
        return m;
    }
}
