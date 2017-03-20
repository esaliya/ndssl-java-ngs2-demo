package org.saliya.ndssl.ngs2demo.intersim;

import com.google.common.io.Files;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.TreeMap;
import java.util.stream.IntStream;

/**
 * Saliya Ekanayake on 3/16/17.
 */
public class PlotGenerator {
    public static void main(String[] args) {
        String file = args[0];
        int numBuckets = Integer.parseInt(args[1]);
        File f = new File(file);
        String dir = f.getParent();
        OutputReader outputReader = new OutputReader();
        IntersimData intersimData = outputReader.readOutput(file);

        String fileNameWithoutExtension = Files.getNameWithoutExtension(file);
        Path infectedByAgeGroupFile = Paths.get(dir, fileNameWithoutExtension+"_cumulativeInfectedByAge.txt");

        findInfectedByAgeGroup(infectedByAgeGroupFile, intersimData , numBuckets);

    }



    private static void findInfectedByAgeGroup(Path infectedByAgeGroupFile, IntersimData intersimData, int buckets) {
        try (BufferedWriter bw = java.nio.file.Files.newBufferedWriter(infectedByAgeGroupFile)) {
            PrintWriter writer = new PrintWriter(bw, true);
            TreeMap<Integer, IntersimAgent> nodeIdToIntersimAgent = intersimData.getNodeIdToIntersimAgent();
            int minAge = Integer.MAX_VALUE;
            int maxAge = Integer.MIN_VALUE;
            for (IntersimAgent agent : nodeIdToIntersimAgent.values()){
                minAge = Math.min(agent.getAge(), minAge);
                maxAge = Math.max(agent.getAge(), maxAge);
            }
            System.out.println(minAge + " " + maxAge);
            int ageDiff = (maxAge - minAge)+1;
            int p = ageDiff / buckets;
            int[] ageRanges = new int[buckets+1];
            ageRanges[0] = minAge;
            for (int i = 1; i <= buckets; ++i){
                ageRanges[i] = ageRanges[i-1]+p;
            }
            ageRanges[buckets] = maxAge+1;

            TreeMap<Integer, Integer> ageGroupIdxToTotalCount = new TreeMap<>();
            for (IntersimAgent agent : nodeIdToIntersimAgent.values()){
                int age = agent.getAge();
                int ageGroupIdx = ageToBucketIdx(age, minAge, p);
                ageGroupIdxToTotalCount.putIfAbsent(ageGroupIdx, 0);
                ageGroupIdxToTotalCount.put(ageGroupIdx, ageGroupIdxToTotalCount.get(ageGroupIdx)+1);
            }

            // DEBUG code
            {
                System.out.println();
                IntStream.range(0, buckets).forEach(i -> {
                    System.out.print("[" + ageRanges[i] + "," + (ageRanges[i + 1] - 1) + "],");
                });
            }

            // DEBUG code
            /*{
                int i = ageToBucketIdx(80, minAge, p);
                System.out.println(i + " [" + ageRanges[i] + ", " + ageRanges[i + 1] + "]");
            }*/

            TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, int[]>>> iterToTimeStampToNodeIdToStates
                    = intersimData.getIterToTimeStampToNodeIdToStates();
            TreeMap<Integer, TreeMap<Integer, TreeMap<Integer, Integer>>> iterToTimeStampToAgeGroupIdxToInfectedCount =
                    new
                    TreeMap<>();
            int minAgeFinal = minAge;
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
                        int ageGroupIdx = ageToBucketIdx(age, minAgeFinal, p);

                        if (!ageGroupIdxToInfectedCount.containsKey(ageGroupIdx)){
                            ageGroupIdxToInfectedCount.put(ageGroupIdx, 0);
                        }
                        if (states[0] == 1){
                            ageGroupIdxToInfectedCount.put(ageGroupIdx, ageGroupIdxToInfectedCount.get(ageGroupIdx)+1);
                        }
                    });
                });
            });

            // DEBUG Code
            {
                iterToTimeStampToAgeGroupIdxToInfectedCount.entrySet().forEach(iterToV -> {
                    int iter = iterToV.getKey();
                    System.out.println("\nIteration: " + iter);
                    System.out.print("TS ");
                    IntStream.range(0,buckets).forEach(ageGroupIdx -> {
                        System.out.print(" [" + ageRanges[ageGroupIdx] + "," + (ageRanges[ageGroupIdx + 1] - 1) + "]");
                    });
                    System.out.println();
                    iterToV.getValue().entrySet().forEach(timeStampToV -> {
                        int timeStamp = timeStampToV.getKey();
//                        System.out.print("TS: " + timeStamp);
                        System.out.print(timeStamp);
                        TreeMap<Integer, Integer> ageGroupIdxToInfectedCount = timeStampToV.getValue();
                        IntStream.range(0,buckets).forEach(ageGroupIdx -> {
                            int infectedCount = ageGroupIdxToInfectedCount.containsKey(ageGroupIdx) ?
                                    ageGroupIdxToInfectedCount.get(ageGroupIdx) : 0;
                            System.out.printf(" %.2f", (infectedCount*1.0/ageGroupIdxToTotalCount.get(ageGroupIdx)));

                        });
//                        timeStampToV.getValue().entrySet().forEach(ageGroupIdxToV -> {
//                            int ageGroupIdx = ageGroupIdxToV.getKey();
//                            int infectedCount = ageGroupIdxToV.getValue();
//                            System.out.print(" [" + ageRanges[ageGroupIdx] + "," + (ageRanges[ageGroupIdx + 1] - 1) + "]-" +
//                                    infectedCount);
//                        });
                        System.out.println();
                    });
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int ageToBucketIdx(int age, int minAge, int bucketSize){
        int diff = age - minAge;
        int p = diff / bucketSize;
        return p;
    }
}
