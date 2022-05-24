package com.cloudhubs.istiologparser;

import java.util.*;

/**
 * author: Abdullah Al Maruf
 * date: 5/18/22
 * time: 5:45 PM
 * website : https://maruftuhin.com
 */


public class Graph {
    static HashMap<String, List<String>> graph = new HashMap<>();
    static Set<String> allnodes = new HashSet<String>();
    static HashMap<String, Integer> visited = new HashMap<>();
    // 0 = unvisited
    // 1 = in stack
    // 2 = visited

    public static void findCycle(Map<Map<String, Map<String, String>>, Integer> FunctionsMap) {
        // https://www.baeldung.com/cs/detecting-cycles-in-directed-graph
        buildGraph(FunctionsMap);

//        // print the graph
//        for (String node: graph.keySet()) {
//            System.out.printf(node+":> ");
//            List<String> edges = graph.get(node);
//            for (String edge : edges) {
//                System.out.printf(edge+", ");
//            }
//            System.out.println("\n");
//        }

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
        while (!stack.empty() && backup.peek() != u) {
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
