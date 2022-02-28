package dag;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Vertex {
    private final String id;
    private String value;
    Set<Vertex> inboundEdges;
    Set<Vertex> outboundEdges;
    Set<Vertex> predecessorsCache;
    Set<Vertex> successorsCache;

    final ReadWriteLock rwLock;

    Vertex(String id, String value) {
        this.id = id;
        this.value = value;
        this.inboundEdges = new HashSet<>();
        this.outboundEdges = new HashSet<>();
        this.predecessorsCache = new HashSet<>();
        this.successorsCache = new HashSet<>();
        this.rwLock = new ReentrantReadWriteLock();
    }

    public String getID() { return id; }

    public String getValue() {
        return value;
    }

    public void setValue(String value) { this.value = value; }
}

class VertexNullException extends Exception {
    VertexNullException() {
        super("Null Vertex");
    }
}

class VertexUnknownException extends Exception {
    VertexUnknownException() {
        super("Unknown Vertex");
    }
}

class VertexDuplicateException extends Exception {
    VertexDuplicateException(Vertex vertex) {
        super("vertex: " + vertex.getID() + " already exists");
    }
}

class VertexEmptyIDException extends Exception {
    VertexEmptyIDException() {
        super("Empty ID Vertex");
    }
}

class EdgeUnknownException extends Exception {
    EdgeUnknownException() {
        super("Unknown Edge");
    }
}

class EdgeSelfLoopException extends Exception {
    EdgeSelfLoopException(String id) {
        super("vertex: " + id + " makes self edge");
    }
}

class EdgeLoopException extends Exception {
    EdgeLoopException(String fromID, String toID) {
        super("Edge from " + fromID + " to " + toID + " will create loop");
    }
}

class EdgeDuplicateException extends Exception {
    EdgeDuplicateException(String fromID, String toID) {
        super("Edge from " + fromID + " to " + toID + " already exists");
    }
}
