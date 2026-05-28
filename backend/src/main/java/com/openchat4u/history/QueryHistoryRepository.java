package com.openchat4u.history;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QueryHistoryRepository extends BaseMapper<QueryHistory> {
}
