package com.cloudhubs.istiologparser;

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class GVGenerator {
    static double val = 0.0;

    /*public static void generate(RadResponseContext radResponseContext) throws IOException {
        Graph g = graph("rad").directed();
        for (RestFlow restFlow : radResponseContext.getRestFlowContext().getRestFlows()) {
            RestEntity server = restFlow.getServers().get(0);
            Node nodeFrom = node(restFlow.getClassName() + "." + restFlow.getMethodName());
            Node nodeTo = node(server.getClassName() + "." + server.getMethodName());
            g = g.with(nodeFrom.link(nodeTo));
        }
        Graphviz.fromGraph(g).render(Format.SVG).toFile(new File("C:\\Users\\das\\Desktop\\rad.svg"));
    }*/

    public static void generate(Map<Map<String, Map<String, String>>, Integer> FunctionsMap) throws IOException {
        Map<String, Set<String>> svcGraphMap = new HashMap<String, Set<String>>();
        Map<String, Boolean> flag = new HashMap();

        StringBuilder graph = new StringBuilder();

        graph.append("digraph endpoints_istio {").append("\n");
        graph.append("rankdir = LR;").append("\n");
//        graph.append("ratio=\"fill\";").append("\n");
        graph.append("size=\"8.3,11.7!\";").append("\n");
        graph.append("margin=0;").append("\n");
        graph.append("node [shape=record];").append("\n");

        int clusterIndex = 0;

        int last = 0;
        double diff = 2.5 / (double) FunctionsMap.size(); // penwidth diff
//        System.out.println(diff);

        FunctionsMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .forEach(x -> {
                    System.out.println(x);
                    Map<String, Map<String, String>> nestedMap = x.getKey();
                    Integer value = x.getValue();
                    nestedMap.forEach((fromSvc, secondNestedMap) -> {
                        secondNestedMap.forEach((toSvc, toEndPoint) -> {
                            if (toEndPoint == null)
                                toEndPoint = toSvc;
                            String fromSvcID = formatNodeName(fromSvc);
                            String toSvcID = formatNodeName(toSvc);
                            String toEndPointID = formatNodeName(toEndPoint);
                            String toSubEndPointID = toSvcID + ":" + toEndPoint;
                            if (value != last)
                                val = val + diff;

                            String format;

                            String hash = encodeColor();

                            if (fromSvc.equals(toSvc)) {
                                format = "%s:%s:e  -> %s:%s:e [taillabel = <<font color=\"%s\">%s</font>> arrowhead=\"empty\" color=\"%s\" penwidth=%s ];";
                            } else {
                                format = "%s:%s:e  -> %s:%s [label = <<font color=\"%s\">%s</font>> arrowhead=\"empty\" color=\"%s\" penwidth=%s];";
                            }

                            String link = String.format(format,
                                    fromSvcID, toEndPointID, toSvcID, toEndPointID, hash, addDoubleQuotations(String.valueOf(value)), hash, val);

                            graph.append(link).append("\n");

                            Set<String> set = svcGraphMap.get(toSvc);
                            if (set == null) {
                                set = new HashSet<String>();
                            }
                            set.add(toEndPoint);
                            svcGraphMap.put(toSvc, set);

                            if (flag.get(fromSvcID) == null) {
                                graph.append("\n");
                                graph.append(fromSvcID).append("[").append("\n");
                                graph.append("shape=\"record\"").append("\n");
                                graph.append("label=").append(addDoubleQuotations(String.format("<%s> %s", fromSvcID, fromSvc))).append("\n");
                                graph.append("]").append("\n");
                                flag.put(fromSvcID, true);
                            }
                        });
                    });
                });

//        System.out.println(FunctionsMap);
//        FunctionsMap.forEach((nestedMap, value) -> {
//            nestedMap.forEach((fromSvc, secondNestedMap) -> {
//                secondNestedMap.forEach((toSvc, toEndPoint) -> {
//
//                    String fromSvcID = formatNodeName(fromSvc);
//                    String toSvcID = formatNodeName(toSvc);
//                    String toEndPointID = formatNodeName(toEndPoint);
//                    String toSubEndPointID = toSvcID + ":" + toEndPoint;
//
//                    String format;
//
//                    String hash = encodeColor();
//
//                    if (fromSvc.equals(toSvc)) {
//                        format = "%s:%s:e  -> %s:%s:e [taillabel = <<font color=\"%s\">%s</font>> arrowhead=\"empty\" color=\"%s\" ];";
//                    } else {
//                        format = "%s:%s:e  -> %s:%s [label = <<font color=\"%s\">%s</font>> arrowhead=\"empty\" color=\"%s\" ];";
//                    }
//
//                    String link = String.format(format,
//                            fromSvcID, toEndPointID, toSvcID, toEndPointID, hash, addDoubleQuotations(String.valueOf(value)), hash);
//
//                    graph.append(link).append("\n");
//
//                    Set<String> set = svcGraphMap.get(toSvc);
//                    if (set == null) {
//                        set = new HashSet<String>();
//                    }
//                    set.add(toEndPoint);
//                    svcGraphMap.put(toSvc, set);
//
//                    if (flag.get(fromSvcID) == null) {
//                        graph.append("\n");
//                        graph.append(fromSvcID).append("[").append("\n");
//                        graph.append("shape=\"record\"").append("\n");
//                        graph.append("label=").append(addDoubleQuotations(String.format("<%s> %s", fromSvcID, fromSvc))).append("\n");
//                        graph.append("]").append("\n");
//                        flag.put(fromSvcID, true);
//                    }
//                });
//            });
//        });

        System.out.println(svcGraphMap);


        svcGraphMap.forEach((Svc, set) -> {
            String SvcID = formatNodeName(Svc);

            StringBuilder label = new StringBuilder();
            label.append(String.format("<%s> %s", SvcID, Svc));

            set.forEach((endPoint) -> {
                String endPointID = formatNodeName(endPoint);
                label.append(String.format("|<%s> %s", endPointID, endPoint));
            });

            graph.append("\n");
            graph.append(SvcID).append("[").append("\n");
            graph.append("shape=\"record\"").append("\n");
            graph.append("label=").append(addDoubleQuotations(label.toString())).append("\n");
            graph.append("]").append("\n");

        });

        graph.append("}");


        try (PrintWriter out = new PrintWriter("kubernetes-istio-sleuth-v0.2.1.dot")) {
            out.println(graph);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    //    private static Map<String, Set<String>> getClusters(RadResponseContext radResponseContext) {
//        Map<String, Set<String>> clusters = new HashMap<>();
//
//        for (RestFlow restFlow : radResponseContext.getRestFlowContext().getRestFlows()) {
//            String nodeFrom = getFullMethodName(restFlow);
//            addToMap(clusters, addDoubleQuotations(restFlow.getResourcePath()), nodeFrom);
//
//            for (RestEntity server : restFlow.getServers()) {
//                String nodeTo = getFullMethodName(server);
//                addToMap(clusters, addDoubleQuotations(server.getResourcePath()), nodeTo);
//            }
//        }
//
//        return clusters;
//    }
//
    private static String addDoubleQuotations(String str) {
        return "\"" + str + "\"";
    }

    private static String formatNodeName(String str) {
        return str.replaceAll("[^a-zA-Z0-9_]", "_");
    }

    private static void addToMap(Map<String, Set<String>> m, String key, String value) {
        if (!m.containsKey(key)) {
            m.put(key, new HashSet<>());
        }
        m.get(key).add(value);
    }

    /**
     * @return a hex Color string in the format #rrggbb.
     */
    private static String encodeColor() {
        // create random object - reuse this as often as possible
        Random random = new Random();

        // create a big random number - maximum is ffffff (hex) = 16777215 (dez)
        int nextInt = random.nextInt(0xffffff + 1);
        Color c = new Color(nextInt).darker();

        // format it as hexadecimal string (with hashtag and leading zeros)
//        return String.format("#%06x", nextInt);
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }

//    private static String getLinkLabel(RestEntity restEntity) {
//        return addDoubleQuotations(restEntity.getHttpMethod() + " " + restEntity.getUrl());
//    }
//
//    private static String getFullMethodName(RestEntity restEntity) {
//        return addDoubleQuotations(restEntity.getClassName() + "." + restEntity.getMethodName());
//    }
//
//    private static String getFullMethodName(RestFlow restFlow) {
//        return addDoubleQuotations(restFlow.getClassName() + "." + restFlow.getMethodName());
//    }
}