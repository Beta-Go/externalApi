package com.baiduAI.app.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baiduAI.app.bean.TemplateBean;
import com.baiduAI.app.dao.FormIdInfoDAO;
import com.baiduAI.app.dao.WechatInfoDAO;
import com.baiduAI.app.dto.FormIdDTO;
import com.baiduAI.app.dto.WechatDTO;
import com.baiduAI.app.sao.WxSao;
import com.baiduAI.app.util.WeChatSystemContext;
import com.baiduAI.app.util.WeixinTemplateNotice;
import io.swagger.client.model.RegisterUsers;
import io.swagger.client.model.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by luoyifei on 2018/2/10.
 */
@Slf4j
@Service
//@Transactional
public class WechatService {

    public static final Logger logger = LoggerFactory.getLogger(WechatService.class);

    @Autowired
    private WxSao wxSao;

    @Autowired
    private FormIdInfoDAO formIdInfoDAO;

    @Autowired
    private WeChatSystemContext weChatSystemContext;

    @Autowired
    private WechatInfoDAO wechatInfoDAO;

    @Autowired
    private EasemobService easemobService;

    @Value("${wx.appid.c}")
    private String appid;

    @Value("${wx.appsecret.c}")
    private String appSecret;

    @Value("${img.local.path}")
    private String imgLocalPath;

    @Value("${img.host}")
    private String imgHost;

    /**
     * 用户咨询模版消息
     */
    @Value("${wx.micropro.consult.tempid}")
    private String consult_notice_tempid;


    /**
     * 根据code获取openid
     *
     * @param code
     * @return
     */
    public String getOpenidByCode(@RequestParam("code") String code) {
        try {
            String userStr = wxSao.getJscode2session(appid, appSecret, code, "authorization_code");
            JSONObject userInfo = JSONObject.parseObject(userStr);
            // String openid = userInfo.getString("openid");
            // String session_key = userInfo.getString("session_key");
            return userStr;
        } catch (Exception e) {
            return e.toString();
        }
    }

    public Map<String, Object> saveUserInfo(@RequestParam("userInfo") String userInfo, @RequestParam("code") String code) {
        Map<String, Object> returnMap = new HashMap<String, Object>();
        logger.info(userInfo);
        String loginInfo = this.getOpenidByCode(code);
        if ("".equals(loginInfo) || null == loginInfo) {
            returnMap.put("msg", "非法code");
            return returnMap;
        }
        String unionId = "";
        if (JSON.parseObject(loginInfo).get("unionid") != null ) {
            unionId = JSON.parseObject(loginInfo).get("unionid").toString();
        }
        // String unionId = WxDecrypt.wxDecrypt(encryptedData, session_key, iv);
        logger.info(unionId);
        JSONObject jsonObject = JSON.parseObject(userInfo);
        String openid = jsonObject.get("openid").toString();
        String nickName = jsonObject.get("nickName").toString();
        String avatarUrl = jsonObject.get("avatarUrl").toString();
        String gender = StringUtils.equals(jsonObject.get("gender").toString(), "1") ? "male" : "female";
        String city = jsonObject.get("city").toString();
        String province = jsonObject.get("province").toString();
        String country = jsonObject.get("country").toString();

        WechatDTO wechatDTO = wechatInfoDAO.getWechatInfoByOpenid(openid);
        if (wechatDTO == null) {
            // 保存用户信息
            wechatInfoDAO.saveWechatInfo(openid, nickName, avatarUrl, gender, city, province, country, unionId);
            returnMap.put("msg", "保存用户信息成功");
            // 注册环信用户
            RegisterUsers users = new RegisterUsers();
            User user = new User().username(openid).password(openid);
            users.add(user);
            Object easemobresult = easemobService.registerEasemobUser(users);
            logger.info(easemobresult.toString());
            // Assert.assertNotNull(easemobresult);
            return returnMap;
        }
        returnMap.put("msg", "已经存在用户数据");
        return returnMap;
    }

    /**
     * 微信上传图片
     *
     * @param file
     * @return
     */
    public String uploadImage(@RequestParam(required = true, value = "file") MultipartFile file) {
        if (null == file) {
            return null;
        }
        String random = RandomStringUtils.randomAlphanumeric(16);
        String fileName = random + ".jpg";
        try {
            String uploadDirName = imgLocalPath.substring(imgLocalPath.lastIndexOf("/"), imgLocalPath.length());
            logger.info(uploadDirName);
            FileCopyUtils.copy(file.getBytes(), new File(imgLocalPath + "/", fileName));
            return imgHost + uploadDirName + "/" + fileName;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 发送模板消息
     * @param openid
     * @param contentArr
     * @param templateMsgType
     * @param url
     * @return
     */
    public Map<String, Object> sendTemplateMsg(String openid, String[] contentArr, String templateMsgType, String url) {
        Map<String, Object> returnMap = new HashMap<String, Object>();

        WeixinTemplateNotice.CommonNoticeBean bean = new WeixinTemplateNotice().new CommonNoticeBean();
        WeixinTemplateNotice.CommonNoticeFourBean beanFour = new WeixinTemplateNotice().new CommonNoticeFourBean();
        WeixinTemplateNotice.CommonNoticeFiveBean beanFive = new WeixinTemplateNotice().new CommonNoticeFiveBean();
        bean.setTouser(openid);
        bean.setPage(url);
        beanFour.setTouser(openid);
        beanFour.setPage(url);
        beanFive.setTouser(openid);
        beanFive.setPage(url);

        // 查找7天内可用的formId
        FormIdDTO formIdDTO = formIdInfoDAO.findByOpenidAndIsusedOrderByCreatedDateDesc(openid, "N");
        if (formIdDTO == null) {
            returnMap.put("msg", "无可用formId");
            return returnMap;
        }
        String formId = formIdDTO.getForm_id();
        bean.setForm_id(formId);
        beanFour.setForm_id(formId);
        beanFive.setForm_id(formId);
        if (contentArr.length == 3) {
            bean.setKeyword1(contentArr[0]);
            bean.setKeyword2(contentArr[1]);
            bean.setKeyword3(contentArr[2]);
        } else if (contentArr.length == 4) {
            beanFour.setKeyword1(contentArr[0]);
            beanFour.setKeyword2(contentArr[1]);
            beanFour.setKeyword3(contentArr[2]);
            beanFour.setKeyword4(contentArr[3]);
        } else if (contentArr.length == 5) {
            beanFive.setKeyword1(contentArr[0]);
            beanFive.setKeyword2(contentArr[1]);
            beanFive.setKeyword3(contentArr[2]);
            beanFive.setKeyword4(contentArr[3]);
            beanFive.setKeyword5(contentArr[4]);
        }
        TemplateBean td = new TemplateBean();
        if ("consult".equals(templateMsgType)) {
            beanFour.setTemplate_id(consult_notice_tempid);
            td = WeixinTemplateNotice.sendCommonFourNotice(beanFour);
        }
        String access_token = weChatSystemContext.getAccessToken();
        String json = send(access_token, td);
        logger.info("根据token:{}发送模板消息:{}，结果是{}", access_token, td, json);
        JSONObject jo = JSONObject.parseObject(json);
        if (jo.containsKey("errcode") && "0".equals(jo.getString("errcode"))) {
            //2、然后翻转formID的状态
            formIdInfoDAO.updateFormInfo(openid, formId);
            logger.info("向{}发送了模板消息返回：{}", openid, bean);
            returnMap.put("msg", "消息推送成功");
        } else {
            returnMap.put("msg", json);
        }

        return returnMap;
    }

    public String send(String access_token, TemplateBean templateBean) {
        String json = wxSao.send(access_token, templateBean);
        return json;
    }

}
