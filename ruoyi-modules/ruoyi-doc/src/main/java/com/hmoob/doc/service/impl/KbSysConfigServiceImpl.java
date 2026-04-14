package com.hmoob.doc.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.doc.domain.KbSysConfig;
import com.hmoob.doc.domain.bo.KbSysConfigBo;
import com.hmoob.doc.domain.vo.KbSysConfigVo;
import com.hmoob.doc.mapper.KbSysConfigMapper;
import com.hmoob.doc.service.IKbSysConfigService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KB系统配置 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbSysConfigServiceImpl implements IKbSysConfigService {

    private final KbSysConfigMapper baseMapper;

    /**
     * 查询系统配置列表
     *
     * @param bo 系统配置信息
     * @return 系统配置列表
     */
    @Override
    public List<KbSysConfigVo> selectConfigList(KbSysConfigBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 构建查询条件
     *
     * @param bo 系统配置信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbSysConfig> buildQueryWrapper(KbSysConfigBo bo) {
        LambdaQueryWrapper<KbSysConfig> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbSysConfig::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getConfigId()), KbSysConfig::getConfigId, bo.getConfigId());
        lqw.like(StringUtils.isNotBlank(bo.getItemName()), KbSysConfig::getItemName, bo.getItemName());
        lqw.eq(StringUtils.isNotBlank(bo.getItemCode()), KbSysConfig::getItemCode, bo.getItemCode());
        lqw.eq(StringUtils.isNotBlank(bo.getItemGroup()), KbSysConfig::getItemGroup, bo.getItemGroup());
        lqw.eq(ObjectUtil.isNotNull(bo.getStatus()), KbSysConfig::getStatus, bo.getStatus());
        lqw.orderByAsc(KbSysConfig::getSortOrder);
        return lqw;
    }

    /**
     * 根据配置项编码查询配置
     *
     * @param itemCode 配置项编码
     * @return 系统配置信息
     */
    @Override
    public KbSysConfigVo selectConfigByCode(String itemCode) {
        return baseMapper.selectByItemCode(itemCode);
    }

    /**
     * 获取配置项值
     *
     * @param itemCode 配置项编码
     * @return 配置项值
     */
    @Override
    public String getConfigValue(String itemCode) {
        KbSysConfigVo config = selectConfigByCode(itemCode);
        if (ObjectUtil.isNull(config)) {
            return null;
        }
        return config.getItemValue();
    }

    /**
     * 获取配置项值(数字类型)
     *
     * @param itemCode 配置项编码
     * @return 配置项值
     */
    @Override
    public Integer getConfigValueAsInt(String itemCode) {
        String value = getConfigValue(itemCode);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取配置项值(布尔类型)
     *
     * @param itemCode 配置项编码
     * @return 配置项值
     */
    @Override
    public Boolean getConfigValueAsBoolean(String itemCode) {
        String value = getConfigValue(itemCode);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    /**
     * 根据配置ID查询信息
     *
     * @param configId 配置ID
     * @return 系统配置信息
     */
    @Override
    public KbSysConfigVo selectConfigById(Long configId) {
        return baseMapper.selectVoById(configId);
    }

    /**
     * 校验配置项编码是否唯一
     *
     * @param bo 系统配置信息
     * @return 结果
     */
    @Override
    public boolean checkConfigCodeUnique(KbSysConfigBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<KbSysConfig>()
            .eq(KbSysConfig::getItemCode, bo.getItemCode())
            .ne(ObjectUtil.isNotNull(bo.getConfigId()), KbSysConfig::getConfigId, bo.getConfigId()));
        return !exist;
    }

    /**
     * 新增保存系统配置信息
     *
     * @param bo 系统配置信息
     * @return 结果
     */
    @Override
    public int insertConfig(KbSysConfigBo bo) {
        // 校验配置项编码是否唯一
        if (!checkConfigCodeUnique(bo)) {
            throw new ServiceException("配置项编码已存在");
        }
        KbSysConfig config = MapstructUtils.convert(bo, KbSysConfig.class);
        // 设置初始值
        if (config.getStatus() == null) {
            config.setStatus(1);
        }
        if (config.getSortOrder() == null) {
            config.setSortOrder(0);
        }
        return baseMapper.insert(config);
    }

    /**
     * 修改保存系统配置信息
     *
     * @param bo 系统配置信息
     * @return 结果
     */
    @Override
    public int updateConfig(KbSysConfigBo bo) {
        KbSysConfig config = MapstructUtils.convert(bo, KbSysConfig.class);
        KbSysConfig oldConfig = baseMapper.selectById(config.getConfigId());
        if (ObjectUtil.isNull(oldConfig)) {
            throw new ServiceException("配置不存在，无法修改");
        }
        // 校验配置项编码是否唯一
        if (!oldConfig.getItemCode().equals(config.getItemCode()) && !checkConfigCodeUnique(bo)) {
            throw new ServiceException("配置项编码已存在");
        }
        return baseMapper.updateById(config);
    }

    /**
     * 删除系统配置信息
     *
     * @param configId 配置ID
     * @return 结果
     */
    @Override
    public int deleteConfigById(Long configId) {
        return baseMapper.deleteById(configId);
    }

}