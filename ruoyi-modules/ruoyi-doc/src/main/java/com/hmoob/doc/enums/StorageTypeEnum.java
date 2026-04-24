package com.hmoob.doc.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件存储类型枚举
 */
@Getter
@AllArgsConstructor
public enum StorageTypeEnum {

    LOCAL("local", "本地存储"),
    OSS("oss", "对象存储");

    private final String code;
    private final String desc;

    /**
     * 根据code获取枚举
     *
     * @param code 存储类型编码
     * @return 枚举值
     */
    public static StorageTypeEnum getByCode(String code) {
        for (StorageTypeEnum e : values()) {
            if (e.getCode().equals(code)) {
                return e;
            }
        }
        return null;
    }

    /**
     * 判断是否为本地存储
     *
     * @param code 存储类型编码
     * @return 是否本地存储
     */
    public static boolean isLocal(String code) {
        return LOCAL.getCode().equals(code);
    }
}
