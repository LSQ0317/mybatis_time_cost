package com.mybatis.timecost.demo;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Select("select id, name from users where id = #{id}")
    User findById(Long id);
}
