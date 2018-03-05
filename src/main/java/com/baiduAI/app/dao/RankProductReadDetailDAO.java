package com.baiduAI.app.dao;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * Created by luoyifei on 2018/2/28.
 */
@Mapper
public interface RankProductReadDetailDAO {

    @Insert("INSERT INTO rank_product_read_detail(openid, cuser_id, sales_id, add_time) VALUES(#{openid}, #{cuser_id}, #{sales_id}, NOW())")
    void saveProductReadDetail(@Param("openid") String openid, @Param("cuser_id") Long cuser_id, @Param("sales_id") Long sales_id);

}
