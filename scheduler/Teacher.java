package ru.technopolis.scheduler;

import java.util.Date;

public class Teacher {
    public String name;
    public Date[] date;
    public int[] time;
    public int prior;

    public int compareTo(Teacher teacher) {
        return Integer.compare(this.prior, teacher.prior);
    }
}
