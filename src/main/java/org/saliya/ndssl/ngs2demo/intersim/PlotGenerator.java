package org.saliya.ndssl.ngs2demo.intersim;

import com.google.common.base.Strings;
import com.google.common.io.Files;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * Saliya Ekanayake on 3/16/17.
 */
public class PlotGenerator {
    private static Pattern pat = Pattern.compile(" ");
    private static int[] ageRanges;
    public static void main(String[] args) {
        String file = args[0];
        int totalTimeSteps = Integer.parseInt(args[1]);
        int numBuckets = Integer.parseInt(args[2]);
        String bucketsStr = null;
        if (args.length > 3) {
            bucketsStr = args[3];
        }
        String graphFile = null;
        if (args.length > 4){
            graphFile = args[4];
        }


        File f = new File(file);
        String dir = f.getParent();
        OutputReader outputReader = new OutputReader();
        IntersimData intersimData = outputReader.readOutput(file);

        String fileNameWithoutExtension = Files.getNameWithoutExtension(file);
        Path infectedByAgeGroupFile = Paths.get(dir, fileNameWithoutExtension+"_cumulativeInfectedByAge.txt");

        findAgeRanges(numBuckets, bucketsStr, intersimData.getNodeIdToIntersimAgent());

        findInfectedByAgeGroup(infectedByAgeGroupFile, intersimData , numBuckets, totalTimeSteps);

        findNeighborInfo(graphFile, intersimData, numBuckets);

    }

    private static void findNeighborInfo(String graphFile, IntersimData intersimData, int numBuckets) {
        try(BufferedReader reader = java.nio.file.Files.newBufferedReader(Paths.get(graphFile))) {
            TreeMap<Integer, IntersimAgent> nodeIdToIntersimAgent = intersimData.getNodeIdToIntersimAgent();
            String line;
            while (!Strings.isNullOrEmpty(line = reader.readLine())){
                String[] splits = pat.split(line);
                int nodeId = Integer.parseInt(splits[0]);
                IntersimAgent nodeAgent = nodeIdToIntersimAgent.get(nodeId);
                for (int i = 1; i < splits.length; ++i){
                    int neighbor = Integer.parseInt(splits[i]);
                    IntersimAgent neighborAgent = nodeIdToIntersimAgent.get(neighbor);
                    nodeAgent.getNeighborIdToNeighbor().put(neighbor, neighborAgent);
                    neighborAgent.getNeighborIdToNeighbor().put(nodeId, nodeAgent); // make it undirectional
                }
            }

            nodeIdToIntersimAgent.values().forEach(agent -> {
                agent.setAgeGroupIdx(ageToBucketIdx(agent.getAge(), numBuckets, ageRanges));
                agent.setAvgNbrAgeGroupIdx(ageToBucketIdx((int)agent.getAverageNeighborAge(), numBuckets, ageRanges));
            });

            File f = new File(graphFile);
            String dir = f.getParent();
            String fileNameWithoutExtension = Files.getNameWithoutExtension(graphFile);

            // DEBUG
            try(BufferedWriter bw = java.nio.file.Files.newBufferedWriter(Paths.get(dir,
                    fileNameWithoutExtension+"_vectors.txt"))){
                PrintWriter writer = new PrintWriter(bw, true);
                writer.println("NodeId Age Gender AvgNbrAge");
                nodeIdToIntersimAgent.entrySet().forEach(nodeIdToV -> {
                    int nodeId = nodeIdToV.getKey();
                    IntersimAgent nodeAgent = nodeIdToV.getValue();
                    double averageNeighborAge = nodeAgent
                            .getAverageNeighborAge();
                    writer.println(nodeId + " " + nodeAgent.getAge() + " " + nodeAgent.getGender() + " " +
                            averageNeighborAge);
                });
            }


            Path distanceFile = Paths.get(dir, fileNameWithoutExtension+"_distance.bin");
            double normalizationConstant = 1.0*Short.MAX_VALUE/20000;
            try(BufferedOutputStream bos = new BufferedOutputStream(java.nio.file.Files.newOutputStream(distanceFile))){
                DataOutputStream dos = new DataOutputStream(bos);
                nodeIdToIntersimAgent.values().forEach(agentI -> nodeIdToIntersimAgent.values().forEach(agentJ -> {
                    try {
                        short normalizedDistance = agentI.getNormalizedDistance(agentJ, normalizationConstant);
                        dos.writeShort(normalizedDistance);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static void findInfectedByAgeGroup(Path infectedByAgeGroupFile, IntersimData intersimData, int numBuckets, int totalTimeSteps) {
        try (BufferedWriter bw = java.nio.file.Files.newBufferedWriter(infectedByAgeGroupFile)) {
            PrintWriter writer = new PrintWriter(bw, true);
            TreeMap<Integer, IntersimAgent> nodeIdToIntersimAgent = intersimData.getNodeIdToIntersimAgent();

            TreeMap<Integer, Integer> ageGroupIdxToTotalCount = new TreeMap<>();
            for (IntersimAgent agent : nodeIdToIntersimAgent.values()){
                int age = agent.getAge();
                int ageGroupIdx = ageToBucketIdx(age, numBuckets, ageRanges);
                ageGroupIdxToTotalCount.putIfAbsent(ageGroupIdx, 0);
                ageGroupIdxToTotalCount.put(ageGroupIdx, ageGroupIdxToTotalCount.get(ageGroupIdx)+1);
            }

            int totalNumberOfAgents = 0;
            for (Integer v : ageGroupIdxToTotalCount.values()){
                totalNumberOfAgents += v;
            }

            TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, int[]>>> iterToTimeStampToNodeIdToStates
                    = intersimData.getIterToTimeStampToNodeIdToStates();
            TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Integer>>> iterToTimeStampToAgeGroupIdxToInfectedCount =
                    new TreeMap<>();

            iterToTimeStampToNodeIdToStates.entrySet().forEach(iterToV -> {
                int iter = iterToV.getKey();

                iterToV.getValue().entrySet().forEach(timeStampToV -> {
                    int timeStamp = timeStampToV.getKey();
                    timeStampToV.getValue().entrySet().forEach(nodeIdToV -> {
                        int nodeId = nodeIdToV.getKey();
                        int[] states = nodeIdToV.getValue();
                        if (!iterToTimeStampToAgeGroupIdxToInfectedCount.containsKey(iter)){
                            iterToTimeStampToAgeGroupIdxToInfectedCount.put(iter, new TreeMap<>());
                        }
                        TreeMap<Integer, TreeMap<Integer, Integer>> timeStampToAgeGroupIdxToInfectedCount
                                = iterToTimeStampToAgeGroupIdxToInfectedCount.get(iter);
                        if (!timeStampToAgeGroupIdxToInfectedCount.containsKey(timeStamp)){
                            timeStampToAgeGroupIdxToInfectedCount.put(timeStamp, new TreeMap<>());
                        }
                        TreeMap<Integer, Integer> ageGroupIdxToInfectedCount
                                = timeStampToAgeGroupIdxToInfectedCount.get(timeStamp);
                        int age = nodeIdToIntersimAgent.get(nodeId).getAge();
                        int ageGroupIdx = ageToBucketIdx(age, numBuckets, ageRanges);

                        if (!ageGroupIdxToInfectedCount.containsKey(ageGroupIdx)){
                            ageGroupIdxToInfectedCount.put(ageGroupIdx, 0);
                        }
                        if (states[0] == 1){
                            ageGroupIdxToInfectedCount.put(ageGroupIdx, ageGroupIdxToInfectedCount.get(ageGroupIdx)+1);
                        }
                    });
                });
            });



            int finalTotalNumberOfAgents = totalNumberOfAgents;
            TreeMap<Integer, TreeMap<Integer, Integer>> timeStampToAgeGroupIdxToCumulativeInfectedCount = new
                    TreeMap<>();
            TreeMap<Integer, Integer> timeStampToItrCount = new TreeMap<>();
            int totalIterations = iterToTimeStampToAgeGroupIdxToInfectedCount.size();
            iterToTimeStampToAgeGroupIdxToInfectedCount.entrySet().forEach(iterToV -> {
                int iter = iterToV.getKey();
                writer.println("\nIteration: " + iter);
                writer.print("TS ");
                IntStream.range(0,numBuckets).forEach(ageGroupIdx -> writer.print(" [" + ageRanges[ageGroupIdx] + "," + (ageRanges[ageGroupIdx + 1] - 1) + "]"));
                writer.println();

                TreeMap<Integer, Integer> ageGroupIdxToCumulativeInfectedCount = new TreeMap<>();
                iterToV.getValue().entrySet().forEach(timeStampToV -> {
                    int timeStamp = timeStampToV.getKey();
                    if (!timeStampToAgeGroupIdxToCumulativeInfectedCount.containsKey(timeStamp)){
                        timeStampToAgeGroupIdxToCumulativeInfectedCount.put(timeStamp, new TreeMap<>());
                    }
                    if (!timeStampToItrCount.containsKey(timeStamp)){
                        timeStampToItrCount.put(timeStamp, 1);
                    } else {
                        timeStampToItrCount.put(timeStamp, timeStampToItrCount.get(timeStamp)+1);
                    }
                    writer.print(timeStamp);
                    TreeMap<Integer, Integer> ageGroupIdxToInfectedCount = timeStampToV.getValue();
                    IntStream.range(0,numBuckets).forEach(ageGroupIdx -> {
                        if (!timeStampToAgeGroupIdxToCumulativeInfectedCount.get(timeStamp).containsKey(ageGroupIdx)){
                            timeStampToAgeGroupIdxToCumulativeInfectedCount.get(timeStamp).put(ageGroupIdx, 0);
                        }
                        int infectedCount = ageGroupIdxToInfectedCount.getOrDefault(ageGroupIdx, 0);
                        int cumulativeInfectedCount = infectedCount + (ageGroupIdxToCumulativeInfectedCount.getOrDefault(ageGroupIdx, 0));
                        ageGroupIdxToCumulativeInfectedCount.put(ageGroupIdx, cumulativeInfectedCount);

                        timeStampToAgeGroupIdxToCumulativeInfectedCount.get(timeStamp).put(ageGroupIdx,
                                timeStampToAgeGroupIdxToCumulativeInfectedCount.get(timeStamp).get(ageGroupIdx)+cumulativeInfectedCount);

                        writer.printf(" %.2f", (cumulativeInfectedCount*1.0/finalTotalNumberOfAgents));

                    });
                    writer.println();
                });

                // Assuming only the last timesteps are missing, i.e. everything in the middle is complete
                if(iterToV.getValue().size() < totalTimeSteps){
                    int[] lastReportedTimeStamp = new int[]{Integer.MIN_VALUE};
                    iterToV.getValue().keySet().forEach(k -> lastReportedTimeStamp[0] = Math.max
                            (lastReportedTimeStamp[0], k));
                    int diff = totalTimeSteps - iterToV.getValue().size();
                    for (int i = lastReportedTimeStamp[0]+1; i <= lastReportedTimeStamp[0]+diff; ++i){
                        writer.print(i);
                        int finalI = i;
                        if (!timeStampToAgeGroupIdxToCumulativeInfectedCount.containsKey(finalI)){
                            timeStampToAgeGroupIdxToCumulativeInfectedCount.put(finalI, new TreeMap<>());
                        }
                        IntStream.range(0,numBuckets).forEach(ageGroupIdx -> {
                            if (!timeStampToAgeGroupIdxToCumulativeInfectedCount.get(finalI).containsKey(ageGroupIdx)){
                                timeStampToAgeGroupIdxToCumulativeInfectedCount.get(finalI).put(ageGroupIdx, 0);
                            }
                            int cumulativeInfectedCount =  ageGroupIdxToCumulativeInfectedCount.get(ageGroupIdx);
                            timeStampToAgeGroupIdxToCumulativeInfectedCount.get(finalI).put(ageGroupIdx,
                                    timeStampToAgeGroupIdxToCumulativeInfectedCount.get(finalI).get(ageGroupIdx)
                                            +cumulativeInfectedCount);
                            writer.printf(" %.2f", (cumulativeInfectedCount*1.0/finalTotalNumberOfAgents));
                        });
                        writer.println();
                    }
                }

            });

            writer.println("Avg. Over All Iterations");
            timeStampToAgeGroupIdxToCumulativeInfectedCount.entrySet().forEach(timeStampToV -> {
                int timeStamp = timeStampToV.getKey();
                writer.print(timeStamp);
                timeStampToV.getValue().entrySet().forEach(kv -> {
                    int ageGroupIdx = kv.getKey();
                    int cumulativeInfectedCount = kv.getValue();
                    writer.printf(" %.2f", (cumulativeInfectedCount*1.0/(finalTotalNumberOfAgents*totalIterations)));
                });
                writer.println();
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void findAgeRanges(int numBuckets, String bucketsStr, TreeMap<Integer, IntersimAgent>
            nodeIdToIntersimAgent) {
        ageRanges = new int[numBuckets+1];

        int minAge = Integer.MAX_VALUE;
        int maxAge = Integer.MIN_VALUE;
        for (IntersimAgent agent : nodeIdToIntersimAgent.values()){
            minAge = Math.min(agent.getAge(), minAge);
            maxAge = Math.max(agent.getAge(), maxAge);
        }

        if (Strings.isNullOrEmpty(bucketsStr)) {
            int ageDiff = (maxAge - minAge) + 1;
            int p = ageDiff / numBuckets;
            ageRanges[0] = minAge;
            for (int i = 1; i <= numBuckets; ++i) {
                ageRanges[i] = ageRanges[i - 1] + p;
            }
        } else {
            String[] splits = pat.split(bucketsStr);
            if (splits.length != numBuckets){
                throw new RuntimeException("number of buckets should equal to the buckets in bucket string");
            }

            IntStream.range(0, numBuckets).forEach(i -> ageRanges[i] = Integer.parseInt(splits[i]));
        }
        ageRanges[numBuckets] = maxAge + 1;
    }

    private static int ageToBucketIdx(int age, int numBuckets, int[] ageRanges) {
        for (int i = 0; i < numBuckets; ++i){
            if (age >= ageRanges[i] && age < ageRanges[i+1]) return i;
        }
        return -1;
    }

}
