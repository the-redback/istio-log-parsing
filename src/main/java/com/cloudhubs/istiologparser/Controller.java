package com.cloudhubs.istiologparser;


import com.google.gson.Gson;

import javax.crypto.spec.DESKeySpec;
import javax.swing.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Controller {

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
        return split1.split(".svc.cluster.local")[0];
    }

    public void InsertIntoFunctionMap(String Svc, String DestSvc,String endpoint) {
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
                Map<?, ?> map = gson.fromJson(line, Map.class);

                if (map == null) {
                    continue;
                }

                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    String line2 = (String) entry.getValue();

                    if (line2.charAt(0) != '{') {
                        continue;
                    }

                    Map<?, ?> map2 = gson.fromJson(line2, Map.class);
                    if (map2 == null) {
                        continue;
                    }

                    if (map2.containsKey("upstream_cluster")) {
                        String value = (String) map2.get("upstream_cluster");
                        if (!value.contains("outbound")) {
                            continue;
                        }
                        String DestSvc = GetDestinationService(value);
                        String path = (String) map2.get("path");
                        InsertIntoFunctionMap(ServiceName, DestSvc, path);
                    }
                }
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
        File dir = new File("sample-logs-1");
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
