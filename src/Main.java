package dag;

import java.util.Map;

public class Main {
    private static DAG dag = null;

    private static void printMap(Map<String, Vertex> map) {
        for (Map.Entry<String, Vertex> entry : map.entrySet())
            System.out.println(entry.getKey() + ": " + entry.getValue().getID());
        System.out.println();
    }

    private static void printVertex(Vertex vertex) throws Exception {
        System.out.println(" -----------------------" + vertex.getID() + " -----------------------");

        System.out.println("value: " + vertex.getValue());

        System.out.println("parents");
        printMap(dag.getParents(vertex.getID()));

        System.out.println("children");
        printMap(dag.getChildren(vertex.getID()));

        System.out.println("predecessors");
        printMap(dag.predecessors(vertex.getID()));

        System.out.println("successors");
        printMap(dag.successors(vertex.getID()));

        vertex.setValue("task A");
    }

    public static void main(String[] args) throws Exception {
        Main.dag = new DAG();

        Vertex taskA = new Vertex("taskA", "callback of task A");
        Vertex taskB = new Vertex("taskB", "callback of task B");
        Vertex taskC = new Vertex("taskC", "callback of task C");
        Vertex taskD = new Vertex("taskD", "callback of task D");
        Vertex taskE = new Vertex("taskE", "callback of task E");
        Vertex taskF = new Vertex("taskF", "callback of task F");
        Vertex taskG = new Vertex("taskG", "callback of task G");
        Vertex taskH = new Vertex("taskH", "callback of task H");

        dag.addVertex(taskA);
        dag.addVertex(taskB);
        dag.addVertex(taskC);
        dag.addVertex(taskD);
        dag.addVertex(taskE);
        dag.addVertex(taskF);
        dag.addVertex(taskG);
        dag.addVertex(taskH);

        System.out.println(dag.getVertex("taskA").getID());
        System.out.println(dag.getVertex("taskB").getID());
        System.out.println(dag.getVertex("taskC").getID());
        System.out.println(dag.getVertex("taskD").getID());
        System.out.println(dag.getVertex("taskE").getID());
        System.out.println(dag.getVertex("taskF").getID());
        System.out.println(dag.getVertex("taskG").getID());
        System.out.println(dag.getVertex("taskH").getID());

        dag.addEdge(taskA.getID(), taskC.getID());
        dag.addEdge(taskB.getID(), taskC.getID());
        dag.addEdge(taskC.getID(), taskD.getID());
        dag.addEdge(taskC.getID(), taskE.getID());
        dag.addEdge(taskD.getID(), taskF.getID());
        dag.addEdge(taskE.getID(), taskF.getID());
        dag.addEdge(taskB.getID(), taskG.getID());
        dag.addEdge(taskF.getID(), taskH.getID());
        dag.addEdge(taskG.getID(), taskH.getID());

        System.out.println("order: " + dag.getOrder());
        System.out.println("size: " + dag.getSize());

        System.out.println("roots");
        printMap(dag.getRoots());

        System.out.println("leaves");
        printMap(dag.getLeaves());

        for (Vertex vertex : dag.getVertices().values())
            printVertex(vertex);

        dag.deleteVertex(taskC.getID());

        dag.deleteEdge(taskE.getID(), taskF.getID());
    }
}
