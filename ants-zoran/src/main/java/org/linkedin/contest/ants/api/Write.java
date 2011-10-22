package org.linkedin.contest.ants.api;
/*
 * Write - Writes a given long to the square the ant is currently on
 * If the argument is null, the ant will erase anything currently on the square
 * If the argument is non-null, the ant will write the new value over any previous value
 *
 */

public class Write implements Action {

    private Long writing;

    public Write(Long l) {
        writing = l;
    }

    public Long getWriting() {
        return writing;
    }

    public void setWriting(Long write) {
        writing = write;
    }
}
