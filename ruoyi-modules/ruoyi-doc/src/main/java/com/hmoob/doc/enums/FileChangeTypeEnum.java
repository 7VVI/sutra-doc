package com.hmoob.doc.enums;

import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件变化类型枚举
 *
 * @author hmoob
 */
@Getter
@AllArgsConstructor
public enum FileChangeTypeEnum {

    CREATED("CREATED", "新增"),
    MODIFIED("MODIFIED", "修改"),
    DELETED("DELETED", "删除"),
    RENAMED("RENAMED", "重命名");

    private final String code;
    private final String desc;

    private static final Map<String, FileChangeTypeEnum> CODE_MAP = new HashMap<>();

    static {
        for (FileChangeTypeEnum e : values()) {
            CODE_MAP.put(e.getCode(), e);
        }
    }

    public static FileChangeTypeEnum getByCode(String code) {
        return CODE_MAP.get(code);
    }
}
