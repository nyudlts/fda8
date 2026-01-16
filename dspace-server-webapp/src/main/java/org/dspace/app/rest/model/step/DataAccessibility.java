package org.dspace.app.rest.model.step;

/**
 * Model class representing accessibility acknowledgment data.
 * Used to transfer the granted status between backend and frontend.
 */
public class DataAccessibility {

    private boolean granted;

    public DataAccessibility() {
        this.granted = false;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }
}
