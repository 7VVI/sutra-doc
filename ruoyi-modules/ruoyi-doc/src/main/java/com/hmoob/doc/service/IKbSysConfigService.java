package com.hmoob.doc.service;

import com.hmoob.doc.domain.bo.KbSysConfigBo;
import com.hmoob.doc.domain.vo.KbSysConfigVo;

import java.util.List;

/**
 * KB系统配置 服务层
 *
 * @author hmoob
 */
public interface IKbSysConfigService {

    /**
     * 查询系统配置列表
     *
     * @param bo 系统配置信息
     * @return 系统配置列表
     */
    List<KbSysConfigVo> selectConfigList(KbSysConfigBo bo);

    /**
     * 根据配置项编码查询配置
     *
     * @param itemCode 配置项编码
     * @return 系统配置信息
     */
    KbSysConfigVo selectConfigByCode(String itemCode);

    /**
     * 获取配置项值
     *
     * @param itemCode 配置项编码
     * @return 配置项值
     */
    String getConfigValue(String itemCode);

    /**
     * 获取配置项值(数字类型)
     *
     * @param itemCode 配置项编码
     * @return 配置项值
     */
    Integer getConfigValueAsInt(String itemCode);

    /**
     * 获取配置项值(布尔类型)
     *
     * @param itemCode 配置项编码
     * @return 配置项值
     */
    Boolean getConfigValueAsBoolean(String itemCode);

    /**
     * 根据配置ID查询信息
     *
     * @param configId 配置ID
     * @return 系统配置信息
     */
    KbSysConfigVo selectConfigById(Long configId);

    /**
     * 校验配置项编码是否唯一
     *
     * @param bo 系统配置信息
     * @return 结果
     */
    boolean checkConfigCodeUnique(KbSysConfigBo bo);

    /**
     * 新增保存系统配置信息
     *
     * @param bo 系统配置信息
     * @return 结果
     */
    int insertConfig(KbSysConfigBo bo);

    /**
     * 修改保存系统配置信息
     *
     * @param bo 系统配置信息
     * @return 结果
     */
    int updateConfig(KbSysConfigBo bo);

    /**
     * 删除系统配置信息
     *
     * @param configId 配置ID
     * @return 结果
     */
    int deleteConfigById(Long configId);

}