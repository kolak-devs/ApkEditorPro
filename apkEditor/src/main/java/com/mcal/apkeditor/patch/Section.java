package com.mcal.apkeditor.patch;

import java.util.List;

/**
 * Класс в который добавляются результаты матчинга файлов
 */
public class Section {
    public int start;
    public int end;
    List<String> groupStrs;

    public Section(int _start, int _end, List<String> _groupStrs) {
        this.start = _start;
        this.end = _end;
        this.groupStrs = _groupStrs;
    }

    /**
     * Возвращет последний индекс матчинга
     */
    public int getEnd() {
        return end;
    }
    /**
     * Возвращет первый индекс матчинга
     */
    public int getStart() {
        return start;
    }

    /**
     * Возвращет список групп матчинга
     */
    public List<String> getGroupStrs() {
        return groupStrs;
    }
}