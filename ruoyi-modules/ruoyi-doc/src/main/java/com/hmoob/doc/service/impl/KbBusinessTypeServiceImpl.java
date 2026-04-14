package com.hmoob.doc.service.impl;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import com.hmoob.common.core.constant.SystemConstants;
import com.hmoob.common.core.exception.ServiceException;
import com.hmoob.common.core.utils.MapstructUtils;
import com.hmoob.common.core.utils.StringUtils;
import com.hmoob.common.core.utils.TreeBuildUtils;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.KbBusinessType;
import com.hmoob.doc.domain.bo.KbBusinessTypeBo;
import com.hmoob.doc.domain.vo.KbBusinessTypeVo;
import com.hmoob.doc.mapper.KbBusinessTypeMapper;
import com.hmoob.doc.service.IKbBusinessTypeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KB业务类型管理 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbBusinessTypeServiceImpl implements IKbBusinessTypeService {

    private final KbBusinessTypeMapper baseMapper;

    /**
     * 分页查询业务类型管理数据
     *
     * @param bo        业务类型信息
     * @param pageQuery 分页对象
     * @return 业务类型信息集合
     */
    @Override
    public TableDataInfo<KbBusinessTypeVo> selectPageBusinessTypeList(KbBusinessTypeBo bo, PageQuery pageQuery) {
        Page<KbBusinessTypeVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询业务类型管理数据
     *
     * @param bo 业务类型信息
     * @return 业务类型信息集合
     */
    @Override
    public List<KbBusinessTypeVo> selectBusinessTypeList(KbBusinessTypeBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 查询业务类型树结构信息
     *
     * @param bo 业务类型信息
     * @return 业务类型树信息集合
     */
    @Override
    public List<Tree<Long>> selectBusinessTypeTreeList(KbBusinessTypeBo bo) {
        List<KbBusinessTypeVo> businessTypes = selectBusinessTypeList(bo);
        return TreeBuildUtils.buildMultiRoot(
            businessTypes,
            KbBusinessTypeVo::getBusinessTypeId,
            KbBusinessTypeVo::getParentId,
            (node, treeNode) -> treeNode
                .setId(node.getBusinessTypeId())
                .setParentId(node.getParentId())
                .setName(node.getBusinessTypeName())
                .setWeight(node.getSortOrder())
        );
    }

    /**
     * 构建查询条件
     *
     * @param bo 业务类型信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbBusinessType> buildQueryWrapper(KbBusinessTypeBo bo) {
        LambdaQueryWrapper<KbBusinessType> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbBusinessType::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getBusinessTypeId()), KbBusinessType::getBusinessTypeId, bo.getBusinessTypeId());
        lqw.eq(ObjectUtil.isNotNull(bo.getParentId()), KbBusinessType::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getBusinessTypeName()), KbBusinessType::getBusinessTypeName, bo.getBusinessTypeName());
        lqw.eq(StringUtils.isNotBlank(bo.getBusinessTypeCode()), KbBusinessType::getBusinessTypeCode, bo.getBusinessTypeCode());
        lqw.eq(ObjectUtil.isNotNull(bo.getStatus()), KbBusinessType::getStatus, bo.getStatus());
        lqw.orderByAsc(KbBusinessType::getSortOrder);
        return lqw;
    }

    /**
     * 根据业务类型ID查询信息
     *
     * @param businessTypeId 业务类型ID
     * @return 业务类型信息
     */
    @Override
    public KbBusinessTypeVo selectBusinessTypeById(Long businessTypeId) {
        return baseMapper.selectVoById(businessTypeId);
    }

    /**
     * 根据业务类型编码查询信息
     *
     * @param businessTypeCode 业务类型编码
     * @return 业务类型信息
     */
    @Override
    public KbBusinessTypeVo selectBusinessTypeByCode(String businessTypeCode) {
        return baseMapper.selectByCode(businessTypeCode);
    }

    /**
     * 校验业务类型编码是否唯一
     *
     * @param bo 业务类型信息
     * @return 结果
     */
    @Override
    public boolean checkBusinessTypeCodeUnique(KbBusinessTypeBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<KbBusinessType>()
            .eq(KbBusinessType::getBusinessTypeCode, bo.getBusinessTypeCode())
            .ne(ObjectUtil.isNotNull(bo.getBusinessTypeId()), KbBusinessType::getBusinessTypeId, bo.getBusinessTypeId()));
        return !exist;
    }

    /**
     * 是否存在业务类型子节点
     *
     * @param businessTypeId 业务类型ID
     * @return 结果
     */
    @Override
    public boolean hasChildByBusinessTypeId(Long businessTypeId) {
        return baseMapper.exists(new LambdaQueryWrapper<KbBusinessType>()
            .eq(KbBusinessType::getParentId, businessTypeId));
    }

    /**
     * 新增保存业务类型信息
     *
     * @param bo 业务类型信息
     * @return 结果
     */
    @Override
    public int insertBusinessType(KbBusinessTypeBo bo) {
        KbBusinessType businessType = MapstructUtils.convert(bo, KbBusinessType.class);
        // 设置初始值
        if (businessType.getStatus() == null) {
            businessType.setStatus(1);
        }
        if (businessType.getSortOrder() == null) {
            businessType.setSortOrder(0);
        }
        // 设置层级
        if (bo.getParentId() != null && bo.getParentId() > 0) {
            KbBusinessType parentType = baseMapper.selectById(bo.getParentId());
            if (ObjectUtil.isNull(parentType)) {
                throw new ServiceException("父业务类型不存在");
            }
            businessType.setTypeLevel(parentType.getTypeLevel() + 1);
        } else {
            businessType.setParentId(0L);
            businessType.setTypeLevel(1);
        }
        return baseMapper.insert(businessType);
    }

    /**
     * 修改保存业务类型信息
     *
     * @param bo 业务类型信息
     * @return 结果
     */
    @Override
    public int updateBusinessType(KbBusinessTypeBo bo) {
        KbBusinessType businessType = MapstructUtils.convert(bo, KbBusinessType.class);
        KbBusinessType oldType = baseMapper.selectById(businessType.getBusinessTypeId());
        if (ObjectUtil.isNull(oldType)) {
            throw new ServiceException("业务类型不存在，无法修改");
        }
        // 如果父业务类型发生变化，需要更新层级
        if (!oldType.getParentId().equals(businessType.getParentId())) {
            if (businessType.getParentId() != null && businessType.getParentId() > 0) {
                KbBusinessType parentType = baseMapper.selectById(businessType.getParentId());
                if (ObjectUtil.isNull(parentType)) {
                    throw new ServiceException("父业务类型不存在");
                }
                businessType.setTypeLevel(parentType.getTypeLevel() + 1);
            } else {
                businessType.setParentId(0L);
                businessType.setTypeLevel(1);
            }
        }
        return baseMapper.updateById(businessType);
    }

    /**
     * 删除业务类型管理信息
     *
     * @param businessTypeId 业务类型ID
     * @return 结果
     */
    @Override
    public int deleteBusinessTypeById(Long businessTypeId) {
        // 校验是否有子业务类型
        if (hasChildByBusinessTypeId(businessTypeId)) {
            throw new ServiceException("存在子业务类型，不允许删除");
        }
        return baseMapper.deleteById(businessTypeId);
    }

}