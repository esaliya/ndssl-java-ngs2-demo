package org.saliya.ndssl.ngs2demo.intersim;

import java.util.TreeMap;

/**
 * Saliya Ekanayake on 3/16/17.
 */
public class IntersimAgent {
    private int age;
    private int gender;
    private TreeMap<Integer, IntersimAgent> neighborIdToNeighbor = new TreeMap<>();
    private Double avgNbrAge = null;
    private int ageGroupIdx;
    private int avgNbrAgeGroupIdx;

    public IntersimAgent(int age, int gender) {
        this.age = age;
        this.gender = gender;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    public int getGender() {
        return gender;
    }

    public void setGender(int gender) {
        this.gender = gender;
    }

    public TreeMap<Integer, IntersimAgent> getNeighborIdToNeighbor() {
        return neighborIdToNeighbor;
    }

    public void setNeighborIdToNeighbor(TreeMap<Integer, IntersimAgent> neighborIdToNeighbor) {
        this.neighborIdToNeighbor = neighborIdToNeighbor;
    }

    public double getAverageNeighborAge(){
        final double[] sum = {0.0};
        if (avgNbrAge == null) {
            neighborIdToNeighbor.values().forEach(neighbor -> sum[0] += neighbor.age);
            avgNbrAge = sum[0] / neighborIdToNeighbor.size();
        }
        return avgNbrAge;
    }

    public short getNormalizedDistance(IntersimAgent agent, double normalizationConstant){
        double dist = Math.pow(age - agent.age, 2);
        dist += Math.pow(gender - agent.gender, 2);
        dist += Math.pow(getAverageNeighborAge() - agent.getAverageNeighborAge(), 2);
        return (short)(dist*normalizationConstant);
    }

    public short getNormalizedDistanceFromAgeGroup(IntersimAgent agent, double normalizationConstant){
        double dist = Math.pow(ageGroupIdx - agent.ageGroupIdx, 2);
        dist += Math.pow(gender - agent.gender, 2);
        dist += Math.pow(avgNbrAgeGroupIdx - agent.avgNbrAgeGroupIdx, 2);
        return (short)(dist*normalizationConstant);
    }

    public int getAgeGroupIdx() {
        return ageGroupIdx;
    }

    public void setAgeGroupIdx(int ageGroupIdx) {
        this.ageGroupIdx = ageGroupIdx;
    }

    public int getAvgNbrAgeGroupIdx() {
        return avgNbrAgeGroupIdx;
    }

    public void setAvgNbrAgeGroupIdx(int avgNbrAgeGroupIdx) {
        this.avgNbrAgeGroupIdx = avgNbrAgeGroupIdx;
    }
}
