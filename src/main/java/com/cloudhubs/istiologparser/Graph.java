package com.cloudhubs.istiologparser;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

/**
 * author: Abdullah Al Maruf
 * date: 5/18/22
 * time: 5:45 PM
 * website : https://maruftuhin.com
 */


public class Graph {
    static HashMap<String, List<String>> graph = new HashMap<>();
    static SortedSet<String> allnodes = new TreeSet<>();
    static HashMap<String, Integer> indegree = new HashMap<>();
    static HashMap<String, Integer> outdegree = new HashMap<>();
    static HashMap<String, Integer> visited = new HashMap<>();
    // 0 = unvisited
    // 1 = in stack
    // 2 = visited

    public static void findMetrics(Map<Map<String, Map<String, String>>, Integer> FunctionsMap) {
        // https://www.baeldung.com/cs/detecting-cycles-in-directed-graph
        buildGraph(FunctionsMap);

        try {
            findCoupling();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // print the graph
//        for (String node : graph.keySet()) {
//            System.out.printf(node + ":> ");
//            List<String> edges = graph.get(node);
//            for (String edge : edges) {
//                System.out.printf(edge + ", ");
//            }
//            System.out.println("\n");
//        }
        findCycle();
    }

    static void findCoupling() throws Exception {
        File file = new File("kubernetes-istio-sleuth-v0.2.1.csv");

        //Create the file

        if (file.createNewFile()) {
            System.out.println("New Text File is created!");
        } else {
            System.out.println("File already exists.");
        }
//        //Write Content
//        FileWriter fileWriter = new FileWriter(file);
//
        CSVWriter writer = new CSVWriter(new FileWriter(file));
        String[] arr1 = {"Service Name",
                "Service Dependencies",
                "",
                "",
                "Criticality and Reliability Metrics",
                ""};
        String[] arr2 = {"",
                "In-Degrees",
                "Out-Degrees",
                "Absolute Importance of the Service (AIS)",
                "Absolute Dependence of the Service (ADS)",
                "Absolute Criticality of the Service (ACS)"};
        writer.writeNext(arr1);
        writer.writeNext(arr2);
        for (String svc : allnodes) {
            String[] array = {svc,
                    String.valueOf(indegree.getOrDefault(svc, 0)),
                    String.valueOf(outdegree.getOrDefault(svc, 0)),
                    String.valueOf(indegree.getOrDefault(svc, 0)),
                    String.valueOf(outdegree.getOrDefault(svc, 0)),
                    String.valueOf(indegree.getOrDefault(svc, 0) * outdegree.getOrDefault(svc, 0)),
            };
            writer.writeNext(array);
        }

        writer.close();
    }

    static void findCycle() {
        for (String s : allnodes) {
            if (!visited.containsKey(s)) {
                Stack<String> stack = new Stack<>();
                stack.add(s);
                visited.put(s, 1);
                dfs(stack, s);
            }
        }
    }

    static void dfs(Stack<String> stack, String u) {
        if (graph.get(u) == null) {
            visited.put(u, 2);
            stack.pop();
            return;
        }
        for (String s : graph.get(u)) {
            if (visited.getOrDefault(s, 0) == 1) {
                printcycle(stack, s);
            } else if (visited.getOrDefault(s, 0) == 0) {
                stack.add(s);
                visited.put(s, 1);
                dfs(stack, s);
            }
        }
        visited.put(u, 2);
        stack.pop();
    }

    static void printcycle(Stack<String> stack, String u) {
        Stack<String> backup = new Stack<>();
        backup.add(stack.peek());
        stack.pop();
        while (!stack.empty() && !backup.peek().equals(u)) {
            backup.add(stack.peek());
            stack.pop();
        }

        System.out.println("\nFound a cycle in microservices: ");
        while (!backup.empty()) {
            System.out.println(backup.peek());
            stack.add(backup.peek());
            backup.pop();
        }
    }

    static void buildGraph(Map<Map<String, Map<String, String>>, Integer> FunctionsMap) {


        FunctionsMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(x -> {
                    System.out.println(x);
                    Map<String, Map<String, String>> nestedMap = x.getKey();
                    Integer value = x.getValue();
                    nestedMap.forEach((fromSvc, secondNestedMap) -> {
                        secondNestedMap.forEach((toSvc, toEndPoint) -> {

                            allnodes.add(fromSvc);
                            allnodes.add(toSvc);

                            indegree.put(toSvc, indegree.getOrDefault(toSvc, 0) + 1);
                            outdegree.put(fromSvc, outdegree.getOrDefault(fromSvc, 0) + 1);

                            if (graph.containsKey(fromSvc)) {
                                graph.get(fromSvc).add(toSvc);
                            } else {
                                List<String> nodes = new LinkedList<>();
                                nodes.add(toSvc);
                                graph.put(fromSvc, nodes);
                            }
                        });
                    });
                });
    }

    private static String formatNodeName(String str) {
        return str.replaceAll("[^a-zA-Z0-9_]", "_");
    }


}
