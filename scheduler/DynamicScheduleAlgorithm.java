package ru.technopolis.scheduler;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DynamicScheduleAlgorithm {
    Event[] events;
    DateTimeClass[] dtc;
    List<List<Integer>>[][] solutionsMatrix;
    List<List<Integer>> solutions;
    int eventSize;
    int dtcSize;
    int maxTimePerDay;

    public DynamicScheduleAlgorithm(Event[] events, DateTimeClass[] dtc, int maxTimePerDay) {
        this.events = events;
        this.dtc = dtc;
        this.eventSize = events.length;
        this.dtcSize = dtc.length;
        this.maxTimePerDay = maxTimePerDay;

        this.solutionsMatrix = new ArrayList[eventSize][dtcSize];
    }

    public List<List<Integer>> findSolutions() {
        //заполнение матрицы решений
        for (int i = 0; i < dtcSize; i++) {
            if (simpleCheck(0, i)) {
                solutionsMatrix[0][i] = new ArrayList<>();
                solutionsMatrix[0][i].add(Collections.singletonList(i));
            }
        }
        int nullCount = 0;
        for (int i = 1; i < eventSize; i++) {
            for (int j = 0; j < dtcSize; j++) {
                // если сама аудитория не подходит не подходит для проведения аттестации, нет смысла проверять её загруженность
                if (simpleCheck(i, j)) {
                    for (int k = 0; k < dtcSize; k++) {
                        if (solutionsMatrix[i][j - 1] != null) {
                            for (List<Integer> path : solutionsMatrix[i][j - 1]) {
                                if (pathsCheck(i, j, path)) {
                                    if (solutionsMatrix[i][j] == null) {
                                        solutionsMatrix[i][j] = new ArrayList<>();
                                    }
                                    List<Integer> newPath = new ArrayList<>(path);
                                    newPath.add(j);
                                    solutionsMatrix[i][j].add(newPath);
                                }
                            }
                        }
                    }
                }
                /* Если во время попытки разместить какую-то аттестацию выясняется, что её не возможно поставить,
                сразу возвращаем null, чтобы не обходить зря следующие аттестации
                 */
                if (solutionsMatrix[i][j] == null) {
                    nullCount++;
                }
                if (nullCount == dtcSize) {
                    return null;
                }
            }
        }
        // собираем все возможные решения расстановки всех событий в один массив
        solutions = new ArrayList<>();
        for (int i = 0; i < dtcSize; i++) {
            if (solutionsMatrix[eventSize - 1][i] != null) {
                solutions.addAll(solutionsMatrix[eventSize - 1][i]);
            }
        }
        return solutions;
    }

    // проверка условий, которые завязаны на уже существующем расписании
    private boolean pathsCheck(int i, int j, List<Integer> path) {
        Event ev = events[i];
        DateTimeClass dateTimeClass = dtc[j];
        Teacher teacher = ev.teacher;
        Group group = ev.group;
        LocalDate pauseStart = dateTimeClass.date.minusDays(ev.type.pauseBefore);
        LocalDate pauseEnd = dateTimeClass.date.plusDays(ev.type.pauseAfter);
        int duration = ev.type.duration;

        int teacherTimeToday = ev.type.duration;
        int groupsEventsToday = 1;
        boolean success = true;
        for (int k = 0; k < path.size(); k++) {
            DateTimeClass d = dtc[path.get(k)];
            Event e = events[k];
            if (d.date == dateTimeClass.date) {
                if (teacher.name.equals(e.teacher.name)) {
                    teacherTimeToday += e.type.duration;
                }
                if (group.group.equals(e.group.group)) {
                    groupsEventsToday++;
                }
            }
            if (
                // если одна из предыдущих аттестаций попадает в окно отдыха до и после текущей аттестации
                    (d.date == dateTimeClass.date) && ((d.time + e.type.duration > dateTimeClass.time) || (dateTimeClass.time + duration > d.time))
                            //если в этот день есть аттестации, которые по времени накладываются с текущей
                            || (d.date.compareTo(pauseStart) >= 0) && (d.date.compareTo(pauseEnd) <= 0)
                            // если преподаватель переработал максимум рабочих часов в день
                            || (teacherTimeToday > maxTimePerDay)
                            // если у группы больше аттестаций этого типа, чем положено
                            || (groupsEventsToday > ev.type.countPerDay)
            ) {
                success = false;
                break;
            }
        }
        return success;
    }

    // проверка условий, которые не завязаны на уже существующем расписании
    private boolean simpleCheck(int i, int j) {
        Event ev = events[i];
        DateTimeClass location = dtc[j];
        Teacher teacher = ev.teacher;
        Group group = ev.group;
        int duration = ev.type.duration;

        return ev.wishedClassroomType <= location.classroom.type //есть ли в аудитории нужное оборудование
                && group.size <= location.classroom.size //влезает ли группа в аудиторию
                // не слишком ли сейчас поздно для начала проведения длительного события
                && location.classroom.num == dtc[i + duration].classroom.num
                && location.date.equals(dtc[i + duration].date)
                // подходит ли дата и время преподавателю
                && Arrays.stream(Arrays.stream(teacher.date).toArray()).anyMatch(dt -> dt.equals(location.date))
                && Arrays.stream(Arrays.stream(teacher.time).toArray()).anyMatch(t -> t == location.time)
                && Arrays.stream(Arrays.stream(teacher.time).toArray()).anyMatch(t -> t == location.time + duration);
    }
}