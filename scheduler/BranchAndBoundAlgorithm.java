package ru.technopolis.scheduler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class BranchAndBoundAlgorithm {
    List<Integer[]> solutions;
    Event[] events;
    DateTimeClass[] dtc;
    List<Teacher> teachers;
    Integer[] solution;
    long[] metrics;
    long[] bestMetrics;
    int eventSize;
    int dtcSize;
    int maxTimePerDay;

    public BranchAndBoundAlgorithm(Event[] events, DateTimeClass[] dtc, ArrayList<Teacher> teachers, int maxTimePerDay) {
        this.events = events;
        // отсортируем события так, чтобы аттестации, которые проводятся более приоритетным преподавателями, были раньше
        Arrays.sort(this.events);
        this.dtc = dtc;
        this.eventSize = events.length;
        this.dtcSize = dtc.length;
        this.maxTimePerDay = maxTimePerDay;
        this.teachers = teachers;
        this.teachers.sort(Comparator.comparingInt(t -> t.prior));
        this.metrics = new long[teachers.size()];
        this.bestMetrics = new long[teachers.size()];
        bestMetrics[0] = -1;

        this.solution = new Integer[dtcSize];
        for (int i = 0; i < dtcSize; i++) {
            solution[i] = -1;
        }
        this.solutions = new ArrayList<>();
    }

    void findSolutions() {
        int event = 0;
        while (event < eventSize) {
            int time = this.getTime(event);
            this.cleanEvent(event);
            int nextTime = this.findNextStartTime(event, time);
            if (nextTime == -1) { //если не удалось найти другого подходящего DateTimeClass для этого события
                if (event == 0) {
                    return; //если речь о первом событии, то уже были перебраны все остальные варианты, и решения нет
                } else {
                    event--; // иначе возвращаемся к предыдущему событию и пробуем изменить для него DateTimeClass
                }
            } else {
                this.submitDateTimeClass(event, nextTime); //бронируем за событием время и аудиторию
                // если решение уже хуже, чем лучшее, можно не продолжать его строить
                if (event == 0 || !isItWorse(event)) {
                    event++; // переходим к следующему событию
                }
            }
            // если решение найдено, добавляем его в список решений и продолжаем искать другие
            if (event == eventSize) {
                solutions.add(solution.clone());
                // теперь метрики этого решения считаются лучшими
                if (bestMetrics[0] == -1) {
                    bestMetrics = metrics.clone();
                }
                event--;
            }
        }
    }

    private boolean isItWorse(int event) {
        Event e = events[event];
        int i = event - 1;
        List<Integer> list = Arrays.asList(solution);
        int ind = list.indexOf(event);
        LocalDate min = dtc[ind].date;
        LocalDate max = min;
        //для рассматриваемого преподавателя пересчитываем длительность его сессии
        while (i >= 0 && events[i].teacher.name.equals(e.teacher.name)) {
            LocalDate dt = dtc[list.indexOf(i)].date;
            if (dt.compareTo(max) > 0) {
                max = dt;
            } else if (dt.compareTo(min) < 0) {
                min = dt;
            }
            i--;
        }
        long diff = ChronoUnit.DAYS.between(min, max);
        metrics[event] = diff;
        int t = teachers.indexOf(e.teacher);
        // если это не первое решение и длительнольность сессии преподавателя уже больше, чем в прошлом решении, считаем, что это хуже
        return bestMetrics[0] != -1 && bestMetrics[t] < metrics[t];
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
