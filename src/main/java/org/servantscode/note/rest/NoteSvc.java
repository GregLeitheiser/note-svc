package org.servantscode.note.rest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.rest.PaginatedResponse;
import org.servantscode.commons.rest.SCServiceBase;
import org.servantscode.note.Note;
import org.servantscode.note.db.NoteDB;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.time.ZonedDateTime;
import java.util.List;

@Path("/note")
public class NoteSvc extends SCServiceBase {
    private static final Logger LOG = LogManager.getLogger(NoteSvc.class);

    private NoteDB db;

    public NoteSvc() {
        db = new NoteDB();
    }

    @GET @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<Note> getNotes(@QueryParam("start") @DefaultValue("0") int start,
                                            @QueryParam("count") @DefaultValue("10") int count,
                                            @QueryParam("created_time") @DefaultValue("id") String sortField,
                                            @QueryParam("search") @DefaultValue("") String search) {

        verifyUserAccess("note.list");
        boolean hasPrivateAccess = userHasAccess("private.note.list");

        try {
            LOG.trace(String.format("Retrieving notes (%s, %s, page: %d; %d, hasPrivateAccess: %b)", search, sortField, start, count, hasPrivateAccess));

            String[] bits = search.split(":");
            String type = bits[0];
            int id = Integer.parseInt(bits[1]);

            int totalNotes = db.getCount(type, id, hasPrivateAccess);

            List<Note> results = db.getNotes(type, id, hasPrivateAccess, sortField, start, count);

            return new PaginatedResponse<>(start, results.size(), totalNotes, results);
        } catch (Throwable t) {
            LOG.error("Retrieving people failed:", t);
            throw t;
        }
    }

    @GET @Path("/{id}") @Produces(MediaType.APPLICATION_JSON)
    public Note getNote(@PathParam("id") int id) {
        verifyUserAccess("note.read");

        try {
            Note note = db.getNote(id);
            if(note.isPrivate())
                verifyUserAccess("private.note.read");
            return note;
        } catch (Throwable t) {
            LOG.error("Retrieving note failed:", t);
            throw t;
        }
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Note createNote(Note note) {
        verifyUserAccess("note.create");
        try {
            db.create(note);
            LOG.info("Created note: " + note.getId());
            return note;
        } catch (Throwable t) {
            LOG.error("Creating note failed:", t);
            throw t;
        }
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON) @Produces(MediaType.APPLICATION_JSON)
    public Note updateNote(Note note) {
        verifyUserAccess("note.update");

        if(note.getId() <= 0)
            throw new NotFoundException();

        Note existingNote = db.getNote(note.getId());
        if(existingNote == null)
            throw new NotFoundException();

        if(existingNote.isPrivate())
            verifyUserAccess("private.note.delete");

        if(existingNote.getResourceId() != note.getResourceId() ||
           existingNote.getCreatorId() != note.getCreatorId() ||
           !existingNote.getResourceType().equals(note.getResourceType()))
            throw new BadRequestException("Illegal note update requested");

        try {
            db.update(note);
            LOG.info("Edited note: " + note.getId());
            return note;
        } catch (Throwable t) {
            LOG.error("Updating note failed:", t);
            throw t;
        }
    }

    @DELETE @Path("/{id}")
    public void deleteNote(@PathParam("id") int id) {
        verifyUserAccess("note.delete");
        if(id <= 0)
            throw new NotFoundException();
        try {
            Note note = db.getNote(id);
            if(note.isPrivate())
                verifyUserAccess("private.note.delete");

            if(note == null || db.delete(id))
                throw new NotFoundException();
            LOG.info("Deleted note: " + note.getId());
        } catch (Throwable t) {
            LOG.error("Deleting note failed:", t);
            throw t;
        }
    }
}
