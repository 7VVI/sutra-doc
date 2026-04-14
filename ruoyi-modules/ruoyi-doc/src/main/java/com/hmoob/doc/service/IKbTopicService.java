package com.hmoob.doc.service;

import cn.hutool.core.lang.tree.Tree;
import com.hmoob.common.mybatis.core.page.PageQuery;
import com.hmoob.common.mybatis.core.page.TableDataInfo;
import com.hmoob.doc.domain.bo.KbTopicBo;
import com.hmoob.doc.domain.vo.KbTopicVo;

import java.util.List;

/**
 * KB主题管理 服务层
 *
 * @author hmoob
 */
public interface IKbTopicService {

    /**
     * 分页查询主题管理数据
     *
     * @param bo        主题信息
     * @param pageQuery 分页对象
     * @return 主题信息集合
     */
    TableDataInfo<KbTopicVo> selectPageTopicList(KbTopicBo bo, PageQuery pageQuery);

    /**
     * 查询主题管理数据
     *
     * @param bo 主题信息
     * @return 主题信息集合
     */
    List<KbTopicVo> selectTopicList(KbTopicBo bo);

    /**
     * 查询主题树结构信息
     *
     * @param bo 主题信息
     * @return 主题树信息集合
     */
    List<Tree<Long>> selectTopicTreeList(KbTopicBo bo);

    /**
     * 根据主题ID查询信息
     *
     * @param topicId 主题ID
     * @return 主题信息
     */
    KbTopicVo selectTopicById(Long topicId);

    /**
     * 根据主题编码查询信息
     *
     * @param topicCode 主题编码
     * @return 主题信息
     */
    KbTopicVo selectTopicByCode(String topicCode);

    /**
     * 校验主题编码是否唯一
     *
     * @param bo 主题信息
     * @return 结果
     */
    boolean checkTopicCodeUnique(KbTopicBo bo);

    /**
     * 是否存在主题子节点
     *
     * @param topicId 主题ID
     * @return 结果
     */
    boolean hasChildByTopicId(Long topicId);

    /**
     * 新增保存主题信息
     *
     * @param bo 主题信息
     * @return 结果
     */
    int insertTopic(KbTopicBo bo);

    /**
     * 修改保存主题信息
     *
     * @param bo 主题信息
     * @return 结果
     */
    int updateTopic(KbTopicBo bo);

    /**
     * 删除主题管理信息
     *
     * @param topicId 主题ID
     * @return 结果
     */
    int deleteTopicById(Long topicId);

}