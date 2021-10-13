package com.cloudhubs.istiologparser;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {
    String filterNamespace = "test"; //keep it blank if requires
    String wildcardDns = "tcs.ecs.baylor.edu"; //keep it blank if requires
    String wildcardProxy = "istio-ingressgateway.istio-system"; //keep it blank if requires
    private Map<Map<String, Map<String, String>>, Integer> FunctionsMap = new HashMap<>();

    // Save file in format 'service_name.demo' or 'instance_name.demo'
    public String AssumeServiceName(String fullPath) {
        // Remove file extension
        int index = fullPath.lastIndexOf("/");
        int dot = fullPath.lastIndexOf(".");
        return fullPath.substring(index + 1, dot);
    }

    public String GetDestinationService(String upstream_cluster) {
        // format: "outbound|9080||productpage.default.svc.cluster.local"
        String split1 = upstream_cluster.split("\\|\\|")[1];
        //  Now split contains "productpage.default.svc.cluster.local"
        String str = split1.split(".svc.cluster.local")[0];
        if (filterNamespace.equals("")) return str;
        if (str.contains(filterNamespace)) return str;
        return "";
    }

    public void InsertIntoFunctionMap(String Svc, String DestSvc, String endpoint) {
        Map<String, Map<String, String>> IntermediateMap = new HashMap<>();
        Map<String, String> SecondInterMap = new HashMap<>();
        SecondInterMap.put(DestSvc, endpoint);
        IntermediateMap.put(Svc, SecondInterMap);

        FunctionsMap.merge(IntermediateMap, 1, Integer::sum);
    }

    /*
     * ReadSingleFile reads single json file and decodes it and saves
     * functions calls in FunctionMap map
     * */
    public void ReadSingleFile(String filename) {
        try {
            // create Gson instance
            Gson gson = new Gson();

            String ServiceName = AssumeServiceName(filename);

            // create a reader
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line = reader.readLine();
            while (line != null) {
                // read next line
                line = reader.readLine();

                Map<?, ?> map;
                try {
                    map = gson.fromJson(line, Map.class);
                } catch (JsonSyntaxException e) {
//                    e.printStackTrace();
                    continue;
                }

                if (map == null) {
                    continue;
                }

//                for (Map.Entry<?, ?> entry : map.entrySet()) {
//                    System.out.println(entry);
//
//                    if (entry.getValue()==null){
//                        continue;
//                    }
//
//                    String line2 = entry.getValue().toString();
//
//                    if (line2.charAt(0) != '{') {
//                        continue;
//                    }
//
//                    Map<?, ?> map2 = gson.fromJson(line2, Map.class);
//                    if (map2 == null) {
//                        continue;
//                    }

                if (map.containsKey("upstream_cluster")) {
                    String value = (String) map.get("upstream_cluster");
                    if (!value.contains("outbound")) {
                        continue;
                    }

                    String DestSvc = GetDestinationService(value);
                    String path = (String) map.get("path");
                    if (DestSvc.equals("")) continue;
                    if (path == null) {
                        System.out.println(DestSvc+"---------"+path);
                        path="/";
                    };

                    path=path.split("\\?")[0];

                    if (path.length() > 26)
                        path=path.substring(0,25)+"~";
                    if (path.length() > 1 && path.endsWith("/")) {
                        path = path.substring(0, path.length() - 1);
                    }

                    if (DestSvc.equals(wildcardDns)) {
                        DestSvc=wildcardProxy;
                        System.out.println("------------"+DestSvc);
                    }
                    InsertIntoFunctionMap(ServiceName, DestSvc, path);
                }
//                }
            }
            // close reader
            reader.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }

    public List<String> GetFiles(File[] files) {
        List<String> result = new ArrayList<>();
        for (File file : files) {
            if (file.isDirectory()) {
                GetFiles(file.listFiles()); // Calls same method again.
            } else {
                result.add(file.getAbsolutePath());
            }
        }
        return result;
    }


    public Controller() throws IOException {
        // Parse multiple file to map
        File dir = new File("tms-log");
        List<String> result = GetFiles(dir.listFiles());
        for (String filePath : result) {
            System.out.println(filePath);
            ReadSingleFile(filePath);
        }

        GVGenerator.generate(FunctionsMap);
    }

    public static boolean isJSONValid(String jsonInString) {
        try {
            Gson gson = new Gson();
            gson.fromJson(jsonInString, Object.class);
            return true;
        } catch (com.google.gson.JsonSyntaxException ex) {
            return false;
        }
    }

}
