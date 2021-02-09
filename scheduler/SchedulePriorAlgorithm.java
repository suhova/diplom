package ru.technopolis.scheduler;

import java.util.Arrays;

public class SchedulePriorAlgorithm {
    Event[] events;
    DateTimeClass[] dtc;
    int[] solution;
    int eventSize;
    int dtcSize;
    int maxTimePerDay;
    boolean[] ignoreWishes; //приходится ли игнорировать пожелания преподавателя для этого события

    public SchedulePriorAlgorithm(Event[] events, DateTimeClass[] dtc, int maxTimePerDay) {
        this.events = events;
        // отсортируем события так, чтобы аттестации, котороые проводятся более приоритетным преподавателями, были раньше
        Arrays.sort(events);
        this.dtc = dtc;
        this.eventSize = events.length;
        this.dtcSize = dtc.length;
        this.maxTimePerDay = maxTimePerDay;

        this.solution = new int[dtcSize];
        for (int i = 0; i < dtcSize; i++) {
            solution[i] = -1;
        }
        this.ignoreWishes = new boolean[eventSize];
    }

    boolean findSolution() {
        int event = 0;
        while (event < eventSize) {
            int time = this.getTime(event);
            int nextTime;
            this.cleanEvent(event);
            nextTime = this.findNextStartTime(event, time);

            //переходим в режим игнорирования пожеланий преподавателя, если не смогли найти идеального решения
            if(!ignoreWishes[event] && nextTime == -1){
                ignoreWishes[event] = true;
                nextTime = this.findNextStartTime(event, 0);
            }

            if (nextTime == -1) { //если не удалось найти другого подходящего DateTimeClass для этого события
                if (event == 0) {
                    return false; //если речь о первом событии, то уже были перебраны все остальные варинты, и решения нет
                } else {
                    ignoreWishes[event] = false;
                    event--; // иначе возвращаемся к предыдущему событию и пробуем изменить для него DateTimeClass
                }
            } else {
                this.submitDateTimeClass(event, nextTime); //бронируем за событием время и аудиторию
                event++; // переходим к следующему событию
            }
        }
        return true;
    }

    private int findNextStartTime(int event, int time) {
        if (time < 0) return -1;

        int duration = events[event].type.duration;
        for (int i = time; i < dtcSize; i++) {
            DateTimeClass location = dtc[i];
            Event ev = events[event];
            Group group = ev.group;
            Teacher teacher = ev.teacher;
            boolean success = false;

            boolean teacherWishes = Arrays.stream(Arrays.stream(teacher.date).toArray()).anyMatch(dt -> dt.equals(location.date))
                    && Arrays.stream(Arrays.stream(teacher.time).toArray()).anyMatch(t -> t == location.time)
                    && Arrays.stream(Arrays.stream(teacher.time).toArray()).anyMatch(t -> t == location.time + duration);
            if (ignoreWishes[event]) {
                teacherWishes = !teacherWishes;
            }
            if (solution[i] == -1 // аудитория в это время свободна
                    && ev.wishedClassroomType <= location.classroom.type //есть ли в аудитории нужное оборудование
                    && group.size <= location.classroom.size //влезает ли группа в аудиторию
                    // не слишком ли сейчас поздно для начала проведения длительного события
                    && location.classroom.num == dtc[i + duration].classroom.num
                    && location.date.equals(dtc[i + duration].date)
                    // подходит ли дата и время преподавателю
                    && teacherWishes
            ) {
                int teacherTime = 0;
                int countPerDay = 0;
                for (int k = 0; k < dtcSize; k++) {
                    // если время-место свободны, они не повлияют на ограничения
                    if (solution[k] == -1) continue;

                    Event currentEvent = events[solution[k]];
                    DateTimeClass currentLocation = dtc[k];

                    // если проверяемая аудитория в это время занята
                    if (location.date.compareTo(currentLocation.date) == 0) {
                        break;
                    }
                    // если у преподавателя в этот день уже были занятия
                    if (currentEvent.teacher.name.equals(teacher.name) && currentLocation.date == location.date
                    ) {
                        teacherTime++;
                        // если преподаватель уже достаточно отработал в этот день.
                        if (teacherTime > maxTimePerDay - duration) {
                            break;
                        }
                    }
                    if (currentEvent.group.group.equals(group.group)) {
                        //если в этот день уже были занятия у этой группы
                        if (currentEvent.type.type.equals(ev.type.type) && location.date == currentLocation.date) {
                            countPerDay++;
                            if (ev.type.countPerDay < countPerDay) {
                                break;
                            }
                        }
                        //если до или после события есть другое событие, до которого меньше "отдыха", чем нужно
                        if (location.date.minusDays(ev.type.pauseBefore).compareTo(currentLocation.date) <= 0
                                && location.date.plusDays(ev.type.pauseAfter).compareTo(currentLocation.date) >= 0) {
                            break;
                        }

                    }
                    if (k == dtcSize - 1) success = true;
                }
            }
            if (success) return i;
        }
        return -1;
    }

    private void cleanEvent(int event) {
        for (int i = 0; i < dtcSize; i++) {
            if (solution[i] == event) solution[i] = -1;
        }
    }

    // бронинирование за событием помещения и времени
    private void submitDateTimeClass(int event, int nextTime) {
        int duration = events[event].type.duration;
        for (int i = nextTime; i < nextTime + duration; i++) {
            solution[i] = event;
        }
    }

    // возвращает следующий свободный индекс DateTimeClass для указанного события
    private int getTime(int event) {
        int min = -1;
        boolean contains = false;
        for (int i = 0; i < dtcSize - 1; i++) {
            // если аудитория в это время ещё никем не забронирована
            if (solution[i] == -1) {
                if (contains) {
                    return i;
                } else {
                    min = i;
                }
            }
            // если это событие уже бронировало какое-то время и место
            if (solution[i] == event) {
                contains = true;
            }
        }
        // если последняя ячейка была занята текущим событием, то следующей для него нет
        if (contains) return -1;
        return min;
    }
}