package com.jackson.lai.transactional.dao;

import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @author cmd
 * @data 2020/4/10 19:19
 */
@Repository
public interface MyTestMapper {

    void saveDate(@Param("name")String name);
}
