package org.servantscode.note.db;

import org.servantscode.commons.db.DBAccess;
import org.servantscode.commons.search.QueryBuilder;
import org.servantscode.note.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteDB extends DBAccess {

    public int getCount(String resourceType, int resourceId, boolean includePrivate) {
        QueryBuilder query = count().from("notes")
                .where("resource_type=?", resourceType)
                .where("resource_id=?", resourceId);
//        String sql = "SELECT count(1) FROM notes WHERE resource_type=? AND resource_id=?";
        if(!includePrivate) query.where("private=false");
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn);
            ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next())
                return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find notes for " + resourceType + ":" + resourceId, e);
        }

        return 0;
    }

    private QueryBuilder baseQuery(boolean includePrivate) {
        QueryBuilder query = select("n.*", "p.name").from("notes n")
                .join("LEFT JOIN people p ON n.creator_id=p.id");
        if(!includePrivate) query.where("private=false");
        return query;
    }

    public List<Note> getNotes(String resourceType, int resourceId, boolean includePrivate, String sortField, int start, int count) {
        QueryBuilder query = baseQuery(includePrivate)
                .where("resource_type=?", resourceType)
                .where("resource_id=?", resourceId);
        query.sort(sortField + (sortField.equals("created_time")? " DESC": "")).limit(count).offset(start);
//        String sql = "SELECT n.*, p.name FROM notes n LEFT JOIN people p ON n.creator_id=p.id WHERE resource_type=? AND resource_id=?";
//        sql += includePrivate? "": " AND private=false";
//        sql += String.format(" ORDER BY %s %s LIMIT ? OFFSET ?", sortField, (sortField.equals("created_time")? "DESC": ""));
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)
        ) {
            return processResults(stmt);
        } catch (SQLException e) {
            throw new RuntimeException("Could not find notes for " + resourceType + ":" + resourceId, e);
        }
    }

    public Note getNote(int id) {
        QueryBuilder query = baseQuery(true).where("n.id=?", id);
        try (Connection conn = getConnection();
             PreparedStatement stmt = query.prepareStatement(conn)
        ) {
            return firstOrNull(processResults(stmt));
        } catch (SQLException e) {
            throw new RuntimeException("Could not find note by id " + id, e);
        }
    }

    public void create(Note note) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("INSERT INTO notes(creator_id, created_time, private, resource_type, resource_id, note) VALUES (?, ?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)
        ) {
            stmt.setInt(1, note.getCreatorId());
            stmt.setTimestamp(2, convert(note.getCreatedTime()));
            stmt.setBoolean(3, note.isPrivate());
            stmt.setString(4, note.getResourceType());
            stmt.setInt(5, note.getResourceId());
            stmt.setString(6, note.getNote());

            if (stmt.executeUpdate() == 0) {
                throw new RuntimeException("Could not create note for " + note.getResourceType() + ":" + note.getResourceId());
            }

            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    note.setId(rs.getInt(1));
                    note.setCreator(getPersonName(note.getCreatorId()));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Could not create note for : " + note.getResourceType() + ":" + note.getResourceId(), e);
        }
    }

    public void update(Note note, boolean edited) {
        String sql = "UPDATE notes SET private=?, note=?";
        sql += edited? ", edited=true": "";
        sql += " WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setBoolean(1, note.isPrivate());
            stmt.setString(2, note.getNote());
            stmt.setInt(3, note.getId());

            if (stmt.executeUpdate() == 0)
                throw new RuntimeException("Could not update note: " + note.getId());

        } catch (SQLException e) {
            throw new RuntimeException("Could not update note: " + note.getId(), e);
        }
    }

    public boolean delete(int id) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM notes WHERE id=?")
        ) {
            stmt.setInt(1, id);

            return stmt.executeUpdate() != 0;
        } catch (SQLException e) {
            throw new RuntimeException("Could not delete note: " + id, e);
        }
    }

    // ----- Private -----
    private List<Note> processResults(PreparedStatement stmt) throws SQLException {
        try (ResultSet rs = stmt.executeQuery()){
            List<Note> notes = new ArrayList<>();
            while(rs.next()) {
                Note note = new Note();
                note.setId(rs.getInt("id"));
                note.setCreator(rs.getString("name"));
                note.setCreatorId(rs.getInt("creator_id"));
                note.setCreatedTime(convert(rs.getTimestamp("created_time")));
                note.setEdited(rs.getBoolean("edited"));
                note.setPrivate(rs.getBoolean("private"));
                note.setResourceType(rs.getString("resource_type"));
                note.setResourceId(rs.getInt("resource_id"));
                note.setNote(rs.getString("note"));
                notes.add(note);
            }
            return notes;
        }
    }

    private String getPersonName(int creatorId) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT name FROM people WHERE id=?")) {
            stmt.setInt(1, creatorId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("name");
                }
            }
        }
        return null;
    }
}
