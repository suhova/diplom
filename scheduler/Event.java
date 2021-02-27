package ru.technopolis.scheduler;

public class Event {
    public Group group;
    public Teacher teacher;
    public AttestationType type;
    public String course;
    public int wishedClassroomType;

    public Event(Group group, Teacher teacher, AttestationType type, String course, int wishedClassroomType) {
        this.group = group;
        this.teacher = teacher;
        this.type = type;
        this.course = course;
        this.wishedClassroomType = wishedClassroomType;
    }

    // чтобы при сортировке сначала был больший приоритет
    public int compareTo(Event event) {
        return -this.teacher.compareTo(event.teacher);
    }
}
