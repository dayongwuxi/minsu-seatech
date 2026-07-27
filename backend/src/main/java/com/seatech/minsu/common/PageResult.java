package com.seatech.minsu.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/** 统一分页返回：{ total, pages, current, size, records } */
@Data
public class PageResult<T> {

    private long total;
    private long pages;
    private long current;
    private long size;
    private List<T> records;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.total = page.getTotal();
        r.pages = page.getPages();
        r.current = page.getCurrent();
        r.size = page.getSize();
        r.records = page.getRecords();
        return r;
    }
}
