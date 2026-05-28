package com.openchat4u.masking;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DataMaskingRuleRepository extends BaseMapper<DataMaskingRule> {
}
