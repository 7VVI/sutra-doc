package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmoob.common.mybatis.annotation.DataColumn;
import com.hmoob.common.mybatis.annotation.DataPermission;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.doc.domain.KbDoc;
import com.hmoob.doc.domain.vo.KbDocVo;

import java.util.List;

/**
 * KB文档 Mapper接口
 *
 * @author hmoob
 */
public interface KbDocMapper extends BaseMapperPlus<KbDoc, KbDocVo> {

    /**
     * 查询文档列表(带数据权限)
     *
     * @param queryWrapper 查询条件
     * @return 文档列表
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dep_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default List<KbDocVo> selectDocList(Wrapper<KbDoc> queryWrapper) {
        return this.selectVoList(queryWrapper);
    }

    /**
     * 分页查询文档列表(带数据权限)
     *
     * @param page         分页信息
     * @param queryWrapper 查询条件
     * @return 文档分页列表
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "dep_id"),
        @DataColumn(key = "userName", value = "create_by")
    })
    default Page<KbDocVo> selectPageDocList(Page<KbDoc> page, Wrapper<KbDoc> queryWrapper) {
        return this.selectVoPage(page, queryWrapper);
    }

    /**
     * 根据文档编号查询文档
     *
     * @param serialNumber 文档编号
     * @return 文档Vo对象
     */
    default KbDocVo selectBySerialNumber(String serialNumber) {
        return this.selectVoOne(new LambdaQueryWrapper<KbDoc>()
            .eq(KbDoc::getSerialNumber, serialNumber));
    }

    /**
     * 根据目录ID查询文档列表
     *
     * @param folderId 目录ID
     * @return 文档列表
     */
    default List<KbDocVo> selectByFolderId(Long folderId) {
        return this.selectVoList(new LambdaQueryWrapper<KbDoc>()
            .eq(KbDoc::getFolderId, folderId)
            .orderByDesc(KbDoc::getCreateTime));
    }

}