package org.servantscode.note.db;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.servantscode.commons.db.AbstractDBUpgrade;

import java.sql.SQLException;

public class DBUpgrade extends AbstractDBUpgrade {
    private static final Logger LOG = LogManager.getLogger(DBUpgrade.class);

    @Override
    public void doUpgrade() throws SQLException {
        LOG.info("Verifying database structures.");

        if(!tableExists("notes")) {
            LOG.info("-- Creating notes table");
            runSql("CREATE TABLE notes (id SERIAL PRIMARY KEY, " +
                                       "creator_id INTEGER REFERENCES people(id), " +
                                       "created_time TIMESTAMP WITH TIME ZONE, " +
                                       "edited BOOLEAN, " +
                                       "private BOOLEAN, " +
                                       "resource_type TEXT, " +
                                       "resource_id INTEGER, " +
                                       "note TEXT, " +
                                       "org_id INTEGER references organizations(id) ON DELETE CASCADE)");
        }
    }
}
