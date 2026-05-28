package com.openchat4u.tenant;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TenantRepository extends BaseMapper<Tenant> {
}
