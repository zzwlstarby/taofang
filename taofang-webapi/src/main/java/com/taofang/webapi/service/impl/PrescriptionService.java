package com.taofang.webapi.service.impl;

import com.taofang.webapi.bean.CommentBean;
import com.taofang.webapi.bean.PrescriptionInfoBean;
import com.taofang.webapi.dao.CommentstarMapper;
import com.taofang.webapi.dao.PrescriptionmessageMapper;
import com.taofang.webapi.dao.RelationlinkMapper;
import com.taofang.webapi.dao.persistence.ElasticsearchDao;
import com.taofang.webapi.domain.*;
import com.taofang.webapi.model.Relationlink;
import com.taofang.webapi.service.IPrescriptionService;
import com.taofang.webapi.util.PaginationUtil;
import com.taofang.webapi.util.PrescriptionModelUtil;
import com.taofang.webapi.util.RelationLinkModelUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * @Desc
 * @Author Remilia
 * @Create 2016-04-16
 */
@Service
public class PrescriptionService implements IPrescriptionService{
    private static final Logger LOGGER = LoggerFactory.getLogger(PrescriptionService.class);
    @Autowired
    private ElasticsearchDao elasticsearchDao;
    @Autowired
    private RelationlinkMapper relationlinkMapper;
    @Autowired
    private PrescriptionmessageMapper prescriptionmessageMapper;
    @Autowired
    private CommentstarMapper commentstarMapper;

    @Override
    public PrescriptionPagination getPrescriptionPagination(String prescription, String sort, int start, int limit) {
        PrescriptionPagination prescriptionPagination;
        try{
            prescriptionPagination = elasticsearchDao.searchPrescription(prescription, sort, start, limit);
            prescriptionPagination.setTotalPage(PaginationUtil.getTotalPage(prescriptionPagination.getTotalCount(), limit));
            LOGGER.error("根据偏方或疾病名称[prescription:" + prescription + ";sort:" + sort + ";start:" + start +
                    ";limit:" + limit + "]查询偏方信息 ==> " + prescriptionPagination);
        }catch(Exception e){
            LOGGER.error("根据偏方或疾病名称[prescription:" + prescription + ";sort:" + sort + ";start:" + start +
                    ";limit:" + limit + "]查询偏方信息 ==> error ==> " + e.getMessage(), e);
            prescriptionPagination = new PrescriptionPagination(0, new ArrayList<>());
        }
        return prescriptionPagination;
    }

    @Override
    public PrescriptionRelateInfo getPrescriptionRelateInfoByName(String prescriptionName) {
        PrescriptionRelateInfo prescriptionRelateInfo = new PrescriptionRelateInfo();

        return prescriptionRelateInfo;
    }

    @Override
    public PrescriptionWithLinks getPrescriptionWithLinksById(int id) {
        Prescription prescription = new Prescription();
        List<RelationLinkInfo> videoLinks = new ArrayList<>();
        List<RelationLinkInfo> contentLinks = new ArrayList<>();
        List<PrescriptionComment> commentList = new ArrayList<>();
        try{
            List<PrescriptionInfoBean> prescriptionInfoBeanList = prescriptionmessageMapper.selectPrescriptionInfoById(id);
            if(prescriptionInfoBeanList.size() > 0){
                prescription = PrescriptionModelUtil.tranPrescriptionInfoBean(prescriptionInfoBeanList.get(0));
                String diseaseNames = elasticsearchDao.searchDiseaseById(id);
                prescription.setDiseaseNames(diseaseNames);
                // 相关信息
                List<Relationlink> relationlinkList = relationlinkMapper.selectPrescriptionLink(id);
                for(Relationlink relationlink : relationlinkList){
                    RelationLinkInfo relationLinkInfo = RelationLinkModelUtil.tranRelationLink(relationlink);
                    if(relationlink.getLinkurl().contains(".mp3")){
                        videoLinks.add(relationLinkInfo);
                    }else{
                        contentLinks.add(relationLinkInfo);
                    }
                }
                // 评论信息
                List<CommentBean> commentBeanList = commentstarMapper.selectCommentByPrescriptionID(id);
                prescription.setCommentCount(commentBeanList.size());
                commentList = PrescriptionModelUtil.tranCommentBeanList(commentBeanList);
            }else{
                prescription.setId(0);
            }
            LOGGER.info("根据偏方ID[id:" + id + "],查询偏方的详细信息 ==> " + prescription);
        }catch(Exception e){
            LOGGER.error("根据偏方ID[id:" + id + "],查询偏方的详细信息 ==> error ==> " + e.getMessage(), e);
            prescription.setId(0);
        }
        return new PrescriptionWithLinks(prescription, videoLinks, contentLinks, commentList);
    }

}
