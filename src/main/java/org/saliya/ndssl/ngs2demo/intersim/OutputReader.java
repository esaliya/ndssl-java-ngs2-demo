package org.saliya.ndssl.ngs2demo.intersim;

import com.google.common.base.Strings;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Hashtable;
import java.util.Random;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Saliya Ekanayake on 3/16/17.
 */
public class OutputReader {
    Pattern pat = Pattern.compile("[ \t]");
    public IntersimData readOutput(String file){
        TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, int[]>>> iterToTimeStampToNodeIdToStates = new TreeMap<>();
        TreeMap<Integer, IntersimAgent> nodeIdToIntersimAgent = new TreeMap<>();
        Random random = new Random();
        try(BufferedReader reader = Files.newBufferedReader(Paths.get(file))) {
            String line;
            while (!Strings.isNullOrEmpty(line = reader.readLine())){
                if (line.startsWith("#")) continue;
                String [] splits = pat.split(line);
                int nodeId = Integer.parseInt(splits[0]);
                int iter = Integer.parseInt(splits[1]);
                int timeStamp = Integer.parseInt(splits[2]);
                int[] states = new int[2];
                states[0] = Integer.parseInt(splits[4]);
                states[1] = Integer.parseInt(splits[5]);

                if(!iterToTimeStampToNodeIdToStates.containsKey(iter)){
                    iterToTimeStampToNodeIdToStates.put(iter, new TreeMap<>());
                }
                TreeMap<Integer, TreeMap<Integer, int[]>> timeStampToNodeIdToStates = iterToTimeStampToNodeIdToStates.get(iter);
                if (!timeStampToNodeIdToStates.containsKey(timeStamp)){
                    timeStampToNodeIdToStates.put(timeStamp, new TreeMap<>());
                }
                TreeMap<Integer, int[]> nodeIdToStates = timeStampToNodeIdToStates.get(timeStamp);
                if (!nodeIdToStates.containsKey(nodeId)){
                    nodeIdToStates.put(nodeId, states);
                }

                if (!nodeIdToIntersimAgent.containsKey(nodeId)){
                    nodeIdToIntersimAgent.put(nodeId, new IntersimAgent(random.nextInt(100), random.nextInt(1)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new IntersimData(iterToTimeStampToNodeIdToStates, nodeIdToIntersimAgent);
    }
}
