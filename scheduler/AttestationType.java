package ru.technopolis.scheduler;

public class AttestationType {
    public String type;
    public int pauseBefore;
    public int pauseAfter;
    public int maxInDay;
    public int duration;
    public int countPerDay;

    public AttestationType(String type, int pauseBefore, int pauseAfter, int duration, int maxInDay, int countPerDay) {
        this.type = type;
        this.pauseBefore = pauseBefore;
        this.pauseAfter = pauseAfter;
        this.duration = duration;
        this.maxInDay = maxInDay;
        this.countPerDay = countPerDay;
    }
}
