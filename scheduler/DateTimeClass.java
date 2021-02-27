package ru.technopolis.scheduler;

import java.time.LocalDate;

public class DateTimeClass {
    public LocalDate date;
    public int time;
    public Classroom classroom;

    public DateTimeClass(LocalDate date, int time, Classroom classroom) {
        this.date = date;
        this.time = time;
        this.classroom = classroom;
    }
}
