package com.baiduAI.app.dao;

import com.baiduAI.app.dto.SalesInfoDTO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * Created by luoyifei on 2018/2/20.
 */
@Mapper
public interface SalesInfoDAO {

    @Insert("INSERT INTO sales_info(name, job, store, tel, wechat, location, cover_url, photos, created_date, created_by, updated_date, updated_by) VALUES(#{name}, #{job}, #{store}, #{tel}, #{wechat}, #{location}, #{cover_url}, #{photos}, NOW(), 'system', NOW(), 'system')")
    void saveSalesInfo(@Param("name") String name, @Param("job") String job, @Param("store") String store, @Param("tel") String tel, @Param("wechat") String wechat, @Param("location") String location, @Param("cover_url") String cover_url, @Param("photos") String photos);

    @Select("SELECT * FROM sales_info WHERE id=#{salesId}")
    SalesInfoDTO getSalesInfoBySalesId(@Param("salesId") Long salesId);
}
