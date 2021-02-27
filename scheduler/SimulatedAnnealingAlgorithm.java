package ru.technopolis.scheduler;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulatedAnnealingAlgorithm {
    Random random = new Random();
    List<int[]> solutions;
    int[] metrics;
    Event[] events;
    DateTimeClass[] dtc;
    int eventSize;
    List<Group> groups;

    public SimulatedAnnealingAlgorithm(List<int[]> solutions, Event[] events, DateTimeClass[] dtc) {
        this.solutions = solutions;
        this.events = events;
        this.dtc = dtc;
        Set<Group> gr = new HashSet<>();
        for (Event e : events) {
            gr.add(e.group);
        }
        this.groups = new ArrayList<>();
        this.groups.addAll(gr);
        this.metrics = new int[solutions.size()];
    }

    public int findBestSolution(){
        int i = 0;
        metrics[0]=getRate(0);
        for (int q = dtc.length/10; q > 0; q--) {
            int x = randomIndex(0);
            getRate(x);
            double p = probability(i,x,q);
            if(random.nextFloat() < p){
                i = x;
            }
        }
        return i;
    }

    public int randomIndex(int i){
       int r = random.nextInt(solutions.size()-i);
       if (r==i) return i+1;
       return r;
    }

    public int getRate(int i) {
        if (metrics[i]!=0) return metrics[i];
        int[] sol = solutions.get(i);
        Map<Group, LocalDate> mapGroupDuration = new HashMap<>();

        for (int j = 0; j < eventSize; j++) {
            if (sol[j] != -1) {
                mapGroupDuration.put(events[sol[j]].group, dtc[j].date);
            }
        }
        AtomicInteger groupDurationRate = new AtomicInteger();
        mapGroupDuration.forEach((gr, se) -> groupDurationRate.addAndGet(se.getYear() * 365 + se.getDayOfMonth()));
        metrics[i] = groupDurationRate.get();
        return groupDurationRate.get();
    }

    public double probability(int from, int to, int q){
        float diff = metrics[to]-metrics[from];
        if (diff<0) return 1;
        return Math.exp(-diff/q);
    }
}
