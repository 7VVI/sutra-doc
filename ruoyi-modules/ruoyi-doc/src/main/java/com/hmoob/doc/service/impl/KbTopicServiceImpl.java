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
import com.hmoob.doc.domain.KbTopic;
import com.hmoob.doc.domain.bo.KbTopicBo;
import com.hmoob.doc.domain.vo.KbTopicVo;
import com.hmoob.doc.mapper.KbTopicMapper;
import com.hmoob.doc.service.IKbTopicService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * KB主题管理 服务实现
 *
 * @author hmoob
 */
@RequiredArgsConstructor
@Service
public class KbTopicServiceImpl implements IKbTopicService {

    private final KbTopicMapper baseMapper;

    /**
     * 分页查询主题管理数据
     *
     * @param bo        主题信息
     * @param pageQuery 分页对象
     * @return 主题信息集合
     */
    @Override
    public TableDataInfo<KbTopicVo> selectPageTopicList(KbTopicBo bo, PageQuery pageQuery) {
        Page<KbTopicVo> page = baseMapper.selectVoPage(pageQuery.build(), buildQueryWrapper(bo));
        return TableDataInfo.build(page);
    }

    /**
     * 查询主题管理数据
     *
     * @param bo 主题信息
     * @return 主题信息集合
     */
    @Override
    public List<KbTopicVo> selectTopicList(KbTopicBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    /**
     * 查询主题树结构信息
     *
     * @param bo 主题信息
     * @return 主题树信息集合
     */
    @Override
    public List<Tree<Long>> selectTopicTreeList(KbTopicBo bo) {
        List<KbTopicVo> topics = selectTopicList(bo);
        return TreeBuildUtils.buildMultiRoot(
            topics,
            KbTopicVo::getTopicId,
            KbTopicVo::getParentId,
            (node, treeNode) -> treeNode
                .setId(node.getTopicId())
                .setParentId(node.getParentId())
                .setName(node.getTopicName())
                .setWeight(node.getSortOrder())
        );
    }

    /**
     * 构建查询条件
     *
     * @param bo 主题信息
     * @return 查询条件
     */
    private LambdaQueryWrapper<KbTopic> buildQueryWrapper(KbTopicBo bo) {
        LambdaQueryWrapper<KbTopic> lqw = Wrappers.lambdaQuery();
        lqw.eq(KbTopic::getDelFlag, SystemConstants.NORMAL);
        lqw.eq(ObjectUtil.isNotNull(bo.getTopicId()), KbTopic::getTopicId, bo.getTopicId());
        lqw.eq(ObjectUtil.isNotNull(bo.getParentId()), KbTopic::getParentId, bo.getParentId());
        lqw.like(StringUtils.isNotBlank(bo.getTopicName()), KbTopic::getTopicName, bo.getTopicName());
        lqw.eq(StringUtils.isNotBlank(bo.getTopicCode()), KbTopic::getTopicCode, bo.getTopicCode());
        lqw.eq(ObjectUtil.isNotNull(bo.getStatus()), KbTopic::getStatus, bo.getStatus());
        lqw.orderByAsc(KbTopic::getSortOrder);
        return lqw;
    }

    /**
     * 根据主题ID查询信息
     *
     * @param topicId 主题ID
     * @return 主题信息
     */
    @Override
    public KbTopicVo selectTopicById(Long topicId) {
        return baseMapper.selectVoById(topicId);
    }

    /**
     * 根据主题编码查询信息
     *
     * @param topicCode 主题编码
     * @return 主题信息
     */
    @Override
    public KbTopicVo selectTopicByCode(String topicCode) {
        return baseMapper.selectByTopicCode(topicCode);
    }

    /**
     * 校验主题编码是否唯一
     *
     * @param bo 主题信息
     * @return 结果
     */
    @Override
    public boolean checkTopicCodeUnique(KbTopicBo bo) {
        boolean exist = baseMapper.exists(new LambdaQueryWrapper<KbTopic>()
            .eq(KbTopic::getTopicCode, bo.getTopicCode())
            .ne(ObjectUtil.isNotNull(bo.getTopicId()), KbTopic::getTopicId, bo.getTopicId()));
        return !exist;
    }

    /**
     * 是否存在主题子节点
     *
     * @param topicId 主题ID
     * @return 结果
     */
    @Override
    public boolean hasChildByTopicId(Long topicId) {
        return baseMapper.exists(new LambdaQueryWrapper<KbTopic>()
            .eq(KbTopic::getParentId, topicId));
    }

    /**
     * 新增保存主题信息
     *
     * @param bo 主题信息
     * @return 结果
     */
    @Override
    public int insertTopic(KbTopicBo bo) {
        KbTopic topic = MapstructUtils.convert(bo, KbTopic.class);
        // 设置初始值
        if (topic.getStatus() == null) {
            topic.setStatus(1);
        }
        if (topic.getSortOrder() == null) {
            topic.setSortOrder(0);
        }
        // 设置层级
        if (bo.getParentId() != null && bo.getParentId() > 0) {
            KbTopic parentTopic = baseMapper.selectById(bo.getParentId());
            if (ObjectUtil.isNull(parentTopic)) {
                throw new ServiceException("父主题不存在");
            }
            topic.setTopicLevel(parentTopic.getTopicLevel() + 1);
        } else {
            topic.setParentId(0L);
            topic.setTopicLevel(1);
        }
        return baseMapper.insert(topic);
    }

    /**
     * 修改保存主题信息
     *
     * @param bo 主题信息
     * @return 结果
     */
    @Override
    public int updateTopic(KbTopicBo bo) {
        KbTopic topic = MapstructUtils.convert(bo, KbTopic.class);
        KbTopic oldTopic = baseMapper.selectById(topic.getTopicId());
        if (ObjectUtil.isNull(oldTopic)) {
            throw new ServiceException("主题不存在，无法修改");
        }
        // 如果父主题发生变化，需要更新层级
        if (!oldTopic.getParentId().equals(topic.getParentId())) {
            if (topic.getParentId() != null && topic.getParentId() > 0) {
                KbTopic parentTopic = baseMapper.selectById(topic.getParentId());
                if (ObjectUtil.isNull(parentTopic)) {
                    throw new ServiceException("父主题不存在");
                }
                topic.setTopicLevel(parentTopic.getTopicLevel() + 1);
            } else {
                topic.setParentId(0L);
                topic.setTopicLevel(1);
            }
        }
        return baseMapper.updateById(topic);
    }

    /**
     * 删除主题管理信息
     *
     * @param topicId 主题ID
     * @return 结果
     */
    @Override
    public int deleteTopicById(Long topicId) {
        // 校验是否有子主题
        if (hasChildByTopicId(topicId)) {
            throw new ServiceException("存在子主题，不允许删除");
        }
        return baseMapper.deleteById(topicId);
    }

}