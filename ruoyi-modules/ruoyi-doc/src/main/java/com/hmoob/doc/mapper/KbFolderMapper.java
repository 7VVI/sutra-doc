package com.hmoob.doc.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hmoob.common.mybatis.annotation.DataColumn;
import com.hmoob.common.mybatis.annotation.DataPermission;
import com.hmoob.common.mybatis.core.mapper.BaseMapperPlus;
import com.hmoob.common.mybatis.helper.DataBaseHelper;
import com.hmoob.doc.domain.KbFolder;
import com.hmoob.doc.domain.vo.KbFolderVo;

import java.util.List;

/**
 * KB目录 Mapper接口
 *
 * @author hmoob
 */
public interface KbFolderMapper extends BaseMapperPlus<KbFolder, KbFolderVo> {

    /**
     * 查询目录树结构
     *
     * @return 目录树列表
     */
    @DataPermission({
        @DataColumn(key = "deptName", value = "create_by")
    })
    default List<KbFolderVo> selectFolderTree() {
        return this.selectVoList(new LambdaQueryWrapper<KbFolder>()
            .eq(KbFolder::getStatus, 1)
            .orderByAsc(KbFolder::getSortOrder));
    }

    /**
     * 根据父目录ID查询子目录列表
     *
     * @param parentId 父目录ID
     * @return 子目录列表
     */
    default List<KbFolder> selectListByParentId(Long parentId) {
        return this.selectList(new LambdaQueryWrapper<KbFolder>()
            .eq(KbFolder::getParentId, parentId)
            .eq(KbFolder::getStatus, 1)
            .orderByAsc(KbFolder::getSortOrder));
    }

    /**
     * 根据目录编码查询目录
     *
     * @param folderCode 目录编码
     * @return 目录Vo对象
     */
    default KbFolderVo selectFolderByCode(String folderCode) {
        return this.selectVoOne(new LambdaQueryWrapper<KbFolder>()
            .eq(KbFolder::getFolderCode, folderCode)
            .eq(KbFolder::getStatus, 1));
    }

}