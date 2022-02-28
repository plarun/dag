package dag;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class DAG {
    private final ReadWriteLock rwLock;
    private final ReadWriteLock rwLockCache;

    private final Map<Vertex, String> vertexToID;
    private final Map<String, Vertex> idToVertex;

    DAG() {
        this.rwLock = new ReentrantReadWriteLock();
        this.rwLockCache = new ReentrantReadWriteLock();
        this.vertexToID = new HashMap<>();
        this.idToVertex = new HashMap<>();
    }

    private void checkVertexID(String id) throws Exception {
        if (Objects.equals(id, ""))
            throw new VertexNullException();
        if (idToVertex.get(id) == null)
            throw new VertexUnknownException();
    }

    private void checkEdgeIDs(String fromID, String toID) throws Exception {
        checkVertexID(fromID);
        checkVertexID(toID);
        if (Objects.equals(fromID, toID))
            throw new EdgeSelfLoopException(fromID);
    }

    public void addVertex(Vertex vertex) throws Exception {
        rwLock.writeLock().lock();

        if (vertex == null) {
            throw new VertexNullException();
        }
        if (vertexToID.get(vertex) != null) {
            throw new VertexDuplicateException(vertex);
        }

        addVertexHelper(vertex);

        rwLock.writeLock().unlock();
    }

    private void addVertexHelper(Vertex vertex) throws Exception {
        if (Objects.equals(vertex.getID(), "")) {
            throw new VertexEmptyIDException();
        }

        vertexToID.put(vertex, vertex.getID());
        idToVertex.put(vertex.getID(), vertex);
    }

    public Vertex getVertex(String id) throws Exception {
        rwLock.readLock().lock();

        if (Objects.equals(id, "")) {
            throw new VertexEmptyIDException();
        }
        Vertex vertex = idToVertex.get(id);

        rwLock.readLock().unlock();
        return vertex;
    }

    public void deleteVertex(String id) throws Exception {
        rwLock.writeLock().lock();

        checkVertexID(id);
        Vertex delVertex = idToVertex.get(id);

        for (Vertex predecessor : delVertex.inboundEdges)
            predecessor.outboundEdges.remove(delVertex);

        for (Vertex successor : delVertex.outboundEdges)
            successor.inboundEdges.remove(delVertex);

        delVertex.inboundEdges.clear();
        delVertex.outboundEdges.clear();

        /* Cache data */

        Set<Vertex> predecessors = getPredecessors(delVertex);
        Set<Vertex> successors = getSuccessors(delVertex);

        for (Vertex predecessor : predecessors)
            predecessor.predecessorsCache.clear();

        for (Vertex successor : successors)
            successor.successorsCache.clear();

        delVertex.predecessorsCache.clear();
        delVertex.successorsCache.clear();

        vertexToID.remove(delVertex);
        idToVertex.remove(id);

        rwLock.writeLock().unlock();
    }

    private boolean checkEdgeExists(Vertex fromVertex, Vertex toVertex) {
        return fromVertex.outboundEdges.contains(toVertex) && toVertex.inboundEdges.contains(fromVertex);
    }

    public void addEdge(String fromID, String toID) throws Exception {
        rwLock.writeLock().lock();

        checkEdgeIDs(fromID, toID);
        Vertex fromVertex = idToVertex.get(fromID);
        Vertex toVertex = idToVertex.get(toID);

        if (checkEdgeExists(fromVertex, toVertex)) {
            throw new EdgeDuplicateException(fromID, toID);
        }

        Set<Vertex> fromVertexPredecessor = getPredecessors(fromVertex);
        Set<Vertex> toVertexSuccessor = getSuccessors(toVertex);

        if (toVertexSuccessor.contains(fromVertex))
            throw new EdgeLoopException(fromID, toID);

        if (fromVertex.outboundEdges == null)
            fromVertex.outboundEdges = new HashSet<>();
        if (toVertex.inboundEdges == null)
            toVertex.inboundEdges = new HashSet<>();

        // link vertices
        fromVertex.outboundEdges.add(toVertex);
        toVertex.inboundEdges.add(fromVertex);

        /* Clear Cache */

        for (Vertex predecessor : fromVertexPredecessor)
            predecessor.predecessorsCache.clear();

        for (Vertex successor : toVertexSuccessor)
            successor.successorsCache.clear();

        fromVertex.predecessorsCache.clear();
        toVertex.successorsCache.clear();

        rwLock.writeLock().unlock();
    }

    public void deleteEdge(String fromID, String toID) throws Exception {
        rwLock.writeLock().lock();

        checkEdgeIDs(fromID, toID);

        Vertex fromVertex = idToVertex.get(fromID);
        Vertex toVertex = idToVertex.get(toID);

        if (!checkEdgeExists(fromVertex, toVertex))
            throw new EdgeUnknownException();

        Set<Vertex> fromVertexSuccessor = getSuccessors(fromVertex);
        Set<Vertex> toVertexPredecessor = getPredecessors(toVertex);

        // unlink vertices
        fromVertex.outboundEdges.remove(toVertex);
        toVertex.inboundEdges.remove(fromVertex);

        /* Clear Cache */

        for (Vertex successor : fromVertexSuccessor)
            successor.predecessorsCache.clear();

        for (Vertex predecessor : toVertexPredecessor)
            predecessor.successorsCache.clear();

        fromVertex.predecessorsCache.clear();
        toVertex.successorsCache.clear();

        rwLock.writeLock().unlock();
    }

    public int getOrder() {
        rwLock.readLock().lock();
        int order = vertexToID.size();
        rwLock.readLock().unlock();

        return order;
    }

    public int getSize() {
        rwLock.readLock().lock();

        int size = 0;
        for (Vertex vertex : idToVertex.values())
            size += vertex.outboundEdges.size();

        rwLock.readLock().unlock();

        return size;
    }

    public Map<String, Vertex> predecessors(String id) throws Exception {
        rwLock.readLock().lock();

        checkVertexID(id);
        Vertex vertex = idToVertex.get(id);

        Map<String, Vertex> predecessors = new HashMap<>();
        for (Vertex predecessor : getPredecessors(vertex))
            predecessors.put(predecessor.getID(), predecessor);

        rwLock.readLock().unlock();
        return predecessors;
    }

    public Map<String, Vertex> successors(String id) throws Exception {
        rwLock.readLock().lock();

        checkVertexID(id);
        Vertex vertex = idToVertex.get(id);

        Map<String, Vertex> successors = new HashMap<>();
        for (Vertex successor : getSuccessors(vertex))
            successors.put(successor.getID(), successor);

        rwLock.readLock().unlock();
        return successors;
    }

    private Set<Vertex> getPredecessors(Vertex vertex) {
        rwLockCache.readLock().lock();
        Set<Vertex> cache = vertex.predecessorsCache;
        rwLockCache.readLock().unlock();

        if (!cache.isEmpty())
            return cache;

        vertex.rwLock.readLock().lock();

        cache = new HashSet<>();

        for (Vertex parent : vertex.inboundEdges) {
            Set<Vertex> grandParents = getPredecessors(parent);
            synchronized (this) {
                cache.addAll(grandParents);
                cache.add(parent);
            }
        }

        vertex.rwLock.readLock().unlock();

        rwLockCache.writeLock().lock();
        vertex.predecessorsCache = cache;
        rwLockCache.writeLock().unlock();
        return cache;
    }

    private Set<Vertex> getSuccessors(Vertex vertex) {
        rwLockCache.readLock().lock();
        Set<Vertex> cache = vertex.successorsCache;
        rwLockCache.readLock().unlock();

        if (!cache.isEmpty()) {
            return cache;
        }

        vertex.rwLock.readLock().lock();

        cache = new HashSet<>();

        for (Vertex child : vertex.outboundEdges) {
            Set<Vertex> grandChildren = getSuccessors(child);
            synchronized (this) {
                cache.addAll(grandChildren);
                cache.add(child);
            }
        }

        vertex.rwLock.readLock().unlock();

        rwLockCache.writeLock().lock();
        vertex.successorsCache = cache;
        rwLockCache.writeLock().unlock();
        return cache;
    }

    public Map<String, Vertex> getLeaves() {
        rwLock.readLock().lock();

        Map<String, Vertex> leaves = new HashMap<>();
        for (Vertex vertex : idToVertex.values()) {
            if (vertex.outboundEdges.isEmpty())
                leaves.put(vertex.getID(), vertex);
        }

        rwLock.readLock().unlock();
        return leaves;
    }

    public Map<String, Vertex> getRoots() {
        rwLock.readLock().lock();

        Map<String, Vertex> roots = new HashMap<>();
        for (Vertex vertex : idToVertex.values()) {
            if (vertex.inboundEdges.isEmpty())
                roots.put(vertex.getID(), vertex);
        }

        rwLock.readLock().unlock();
        return roots;
    }

    public Map<String, Vertex> getVertices() {
        rwLock.readLock().lock();

        Map<String, Vertex> vertices = new HashMap<>();
        for (Vertex vertex : idToVertex.values())
            vertices.put(vertex.getID(), vertex);

        rwLock.readLock().unlock();
        return vertices;
    }

    public Map<String, Vertex> getParents(String id) throws Exception {
        rwLock.readLock().lock();

        checkVertexID(id);
        Vertex vertex = idToVertex.get(id);

        Map<String, Vertex> parents = new HashMap<>();
        for (Vertex parent : vertex.inboundEdges)
            parents.put(parent.getID(), parent);

        rwLock.readLock().unlock();
        return parents;
    }

    public Map<String, Vertex> getChildren(String id) throws Exception {
        rwLock.readLock().lock();

        checkVertexID(id);
        Vertex vertex = idToVertex.get(id);

        Map<String, Vertex> children = new HashMap<>();
        for (Vertex child : vertex.outboundEdges)
            children.put(child.getID(), child);

        rwLock.readLock().unlock();
        return children;
    }
}
