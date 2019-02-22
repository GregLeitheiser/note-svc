package org.servantscode.note;

import java.time.ZonedDateTime;

public class Note {
    private int id;
    private int creatorId;
    private String creator;
    private ZonedDateTime createdTime;
    private boolean edited;
    private boolean isPrivate;
    private String resourceType;
    private int resourceId;
    private String note;

    // ----- Accessors -----
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getCreatorId() { return creatorId; }
    public void setCreatorId(int creatorId) { this.creatorId = creatorId; }

    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }

    public ZonedDateTime getCreatedTime() { return createdTime; }
    public void setCreatedTime(ZonedDateTime createdTime) { this.createdTime = createdTime; }

    public boolean isEdited() { return edited; }
    public void setEdited(boolean edited) { this.edited = edited; }

    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean aPrivate) { isPrivate = aPrivate; }

    public String getResourceType() { return resourceType; }
    public void setResourceType(String resourceType) { this.resourceType = resourceType; }

    public int getResourceId() { return resourceId; }
    public void setResourceId(int resourceId) { this.resourceId = resourceId; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
