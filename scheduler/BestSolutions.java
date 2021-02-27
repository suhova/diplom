package ru.technopolis.scheduler;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class BestSolutions {
    List<int[]> solutions;
    Event[] events;
    DateTimeClass[] dtc;
    int eventSize;
    int dtcSize;
    List<Group> groups;
    List<Teacher> teachers;

    public BestSolutions(List<int[]> solutions, Event[] events, DateTimeClass[] dtc, int eventSize, int dtcSize) {
        this.solutions = solutions;
        this.events = events;
        this.dtc = dtc;
        this.eventSize = eventSize;
        this.dtcSize = dtcSize;
        Set<Teacher> teach = new HashSet<>();
        Set<Group> gr = new HashSet<>();
        for (Event e : events) {
            gr.add(e.group);
            teach.add(e.teacher);
        }
        this.groups = new ArrayList<>();
        this.groups.addAll(gr);
        this.teachers = new ArrayList<>();
        this.teachers.addAll(teach);
    }

    public List<int[]> getBestSolutions() {
        List<int[]> result = new ArrayList<>();
        float[][] normalRates = findSolutionsRates();

        float[] sumRates = new float[solutions.size()];
        // находим сумму метрик по всем решениям
        for (int i = 0; i < solutions.size(); i++) {
            for (int j = 0; j < 4; j++) {
                sumRates[i] += normalRates[j][i];
            }
        }
        // в результате возвращаем решения, имеющие лучшие показатели по одной из метрик или по сумме метрик
        Set<Integer> set = new HashSet<>();
        set.addAll(getIndexesOfLargest(sumRates));
        for (int i = 0; i < 4; i++) {
            set.addAll(getIndexesOfLargest(normalRates[i]));
        }
        set.forEach(ind -> result.add(solutions.get(ind)));
        return result;
    }

    public List<Integer> getIndexesOfLargest(float[] array) {
        List<Integer> indexes = new ArrayList<>();
        indexes.add(0);
        for (int i = 1; i < array.length; i++) {
            if (array[i] > array[indexes.get(0)]) {
                indexes.clear();
                indexes.add(i);
            } else if(array[i] == array[indexes.get(0)]){
                indexes.add(i);
            }
        }
        return indexes;
    }

    public float[][] findSolutionsRates() {
        float[] min = new float[4];
        float[] max = new float[4];

        float[][] normalRates = new float[4][solutions.size()];

        // записываем в массив метрик расписаний и для каждой метрики ищем максимум и минимум
        for (int i = 0; i < solutions.size(); i++) {
            int[] sol = solutions.get(i);
            int[] rates = getRates(sol);
            for (int j = 0; j < 4; j++) {
                if (min[j] < 0 || min[j] > rates[j]) min[j] = rates[j];
                if (max[j] < 0 || max[j] < rates[j]) max[j] = rates[j];
                normalRates[j][i] = rates[j];
            }
        }
        //минимаксная нормализация
        float[] rates = new float[solutions.size()];
        for (int i = 0; i < solutions.size(); i++) {
            for (int j = 0; j < 3; j++) {
                normalRates[j][i] = 1 - (normalRates[j][i] - min[j]) / (max[j] - min[j]);
            }
            normalRates[3][i] = (normalRates[3][i] - min[3]) / (max[3] - min[3]);
        }
        return normalRates;
    }

    private class PauseDate {
        public LocalDate date;
        public long pause;

        public PauseDate(LocalDate date) {
            this.date = date;
        }

        public PauseDate(LocalDate date, long pause) {
            this.date = date;
            this.pause = pause;
        }
    }

    /*
    возвращает массив метрик
    индекс 0 - Длительность сессии для каждого преподавателя с учётом его приоритета
    индекс 1 - Суммарное кол-во рабочих дней для преподавателей с учётом их приоритетов
    индекс 2 - Суммарная длительность сессии по группа
    индекс 3 - Суммарная длительность минимального отдыха между экзаменами по группам
     */
    public int[] getRates(int[] sol) {
        Map<Teacher, StartEnd> mapTeacherDuration = new HashMap<>();
        Map<Teacher, Set<LocalDate>> mapTeacherWorkingDays = new HashMap<>();
        Map<Group, LocalDate> mapGroupDuration = new HashMap<>();
        Map<Group, PauseDate> mapGroupsPause = new HashMap<>();
        for (int i = 0; i < eventSize; i++) {
            if (sol[i] != -1) {
                Event e = events[sol[i]];
                if (!mapTeacherDuration.containsKey(e.teacher)) {
                    mapTeacherDuration.put(e.teacher, new StartEnd(dtc[i].date));
                } else {
                    LocalDate start = mapTeacherDuration.get(e.teacher).start;
                    mapTeacherDuration.put(e.teacher, new StartEnd(start, dtc[i].date));
                }

                mapGroupDuration.put(e.group, dtc[i].date);

                if (!mapGroupsPause.containsKey(e.group)) {
                    mapGroupsPause.put(e.group, new PauseDate(dtc[i].date));
                } else {
                    LocalDate date = mapGroupsPause.get(e.group).date;
                    long pause = ChronoUnit.DAYS.between(date, dtc[i].date);
                    if (pause > 0) {
                        long minPause = mapGroupsPause.get(e.group).pause;
                        if (pause < minPause) {
                            mapGroupsPause.put(e.group, new PauseDate(dtc[i].date, pause));
                        } else {
                            mapGroupsPause.put(e.group, new PauseDate(dtc[i].date, minPause));
                        }
                    }
                }
                Set<LocalDate> set;
                if (mapTeacherWorkingDays.containsKey(e.teacher)) {
                    set = mapTeacherWorkingDays.get(e.teacher);
                } else {
                    set = new HashSet<>();
                }
                set.add(dtc[i].date);
                mapTeacherWorkingDays.put(e.teacher, set);
            }
        }
        AtomicInteger teacherDurationRate = new AtomicInteger();
        mapTeacherDuration.forEach((t, se) -> teacherDurationRate.addAndGet((int) (t.prior * (ChronoUnit.DAYS.between(se.start, se.end)))));

        AtomicInteger groupDurationRate = new AtomicInteger();
        mapGroupDuration.forEach((gr, se) -> groupDurationRate.addAndGet(se.getYear() * 365 + se.getDayOfMonth()));

        AtomicInteger groupsPauseRate = new AtomicInteger();
        mapGroupsPause.forEach((gr, se) -> groupsPauseRate.addAndGet((int) se.pause));

        AtomicInteger teacherWorkingDaysRate = new AtomicInteger();
        mapTeacherWorkingDays.forEach((t, d) -> teacherWorkingDaysRate.addAndGet(t.prior * d.size()));
        return new int[]{
                teacherDurationRate.get(),
                teacherWorkingDaysRate.get(),
                groupDurationRate.get(),
                groupsPauseRate.get()
        };
    }

    private class StartEnd {
        public LocalDate start;
        public LocalDate end;

        public StartEnd(LocalDate start) {
            this.start = start;
        }

        public StartEnd(LocalDate start, LocalDate end) {
            this.start = start;
            this.end = end;
        }
    }
}
