package ru.technopolis.scheduler;

import java.util.Arrays;

public class ScheduleAlgorithm {
    Event[] events;
    DateTimeClass[] dtc;
    boolean[][] solution;
    int eventSize;
    int dtcSize;
    int maxTimePerDay;

    public ScheduleAlgorithm(Event[] events, DateTimeClass[] dtc, int maxTimePerDay) {
        this.events = events;
        this.dtc = dtc;
        this.eventSize = events.length;
        this.dtcSize = dtc.length;
        this.maxTimePerDay = maxTimePerDay;

        this.solution = new boolean[eventSize][dtcSize];
    }

    boolean findSolution() {
        int event = 0;
        while (event < eventSize) {
            int time = this.getTime(event);
            int nextTime = this.findNextStartTime(event, time);
            if (nextTime > 0) {
                this.cleanEvent(event);
            }
            if (nextTime == -1) { //если не удалось найти другого подходящего DateTimeClass для этого события
                if (event == 0) {
                    return false; //если речь о первом событии, то уже были перебраны все остальные варинты, и решения нет
                } else {
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
        for (int i = 0; i < dtcSize; i++) {
            DateTimeClass location = dtc[i];
            Event ev = events[event];
            Group group = ev.group;
            Teacher teacher = ev.teacher;

            if (ev.wishedClassroomType <= location.classroom.type //есть ли в аудитории нужное оборудование
                    && group.size <= location.classroom.size //влезает ли группа в аудиторию
                    // не слишком ли сейчас поздно для начала проведения длительного события
                    && location.classroom.num == dtc[i + duration].classroom.num
                    && location.date.equals(dtc[i + duration].date)
                    // подходит ли дата и время преподавателю
                    && Arrays.stream(Arrays.stream(teacher.date).toArray()).anyMatch(dt -> dt.equals(location.date))
                    && Arrays.stream(Arrays.stream(teacher.time).toArray()).anyMatch(t -> t == location.time)
                    && Arrays.stream(Arrays.stream(teacher.time).toArray()).anyMatch(t -> t == location.time + duration)
            ) {
                int teacherTime = 0;
                int countPerDay = 0;

                for (int j = 0; j < event; j++) {
                    Event currentEvent = events[j];
                    boolean success = true;
                    for (int k = 0; k < dtcSize; k++) {
                        DateTimeClass currentLocation = dtc[k];
                        if (solution[j][k]) {
                            // если аудитория уже занята в это время
                            if (location.date.compareTo(currentLocation.date) == 0){
                                success = false;
                                break;
                            }
                            // если у преподавателя в этот день уже были занятия
                            if (currentEvent.teacher.name.equals(teacher.name) && currentLocation.date == location.date
                            ) {
                                teacherTime++;
                                // если преподаватель уже достаточно отработал в этот день.
                                if (teacherTime > maxTimePerDay - duration) {
                                    success = false;
                                    break;
                                }
                            }
                            if (currentEvent.group.group.equals(group.group)) {
                                //если в этот день уже были занятия у этой группы
                                if (currentEvent.type.type.equals(ev.type.type) && location.date == currentLocation.date) {
                                    countPerDay++;
                                    if (ev.type.countPerDay < countPerDay) {
                                        success = false;
                                        break;
                                    }
                                }
                                //если до или после события есть другое событие, до которого меньше "отдыха", чем нужно
                                if (location.date.minusDays(ev.type.pauseBefore).compareTo(currentLocation.date) <= 0
                                && location.date.plusDays(ev.type.pauseAfter).compareTo(currentLocation.date) >= 0){
                                    success = false;
                                    break;
                                }

                            }
                        }
                    }
                    if(!success) break;

                    if (j == event-1) {
                        return i;
                    }
                }

            }
        }
        return -1;
    }

    private void cleanEvent(int event) {
        for (int i = 0; i < dtcSize; i++) {
            solution[event][i] = false;
        }
    }

    // броинирование за событием помещения и времени
    private void submitDateTimeClass(int event, int nextTime) {
        int duration = events[event].type.duration;
        for (int i = nextTime; i < nextTime + duration; i++) {
            solution[event][i] = true;
        }
    }

    // возвращает первый свободный индекс столбца (идентификатор DateTimeClass)
    private int getTime(int event) {
        for (int i = 0; i < dtcSize - 1; i++) {
            if (solution[event][i] && !solution[event][i + 1]) return i + 1;
            // если последняя ячейка не пуста
            if (i == dtcSize - 2 && solution[event][i + 1]) return -1;
        }
        // если строка события пуста
        return 0;
    }
}
