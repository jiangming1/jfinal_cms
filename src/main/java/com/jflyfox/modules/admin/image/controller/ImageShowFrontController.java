package com.jflyfox.modules.admin.image.controller;

import com.jfinal.plugin.activerecord.Db;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jflyfox.jfinal.base.BaseController;
import com.jflyfox.jfinal.base.Paginator;
import com.jflyfox.jfinal.component.annotation.ControllerBind;
import com.jflyfox.jfinal.component.db.SQLUtils;
import com.jflyfox.modules.admin.image.model.TbImage;
import com.jflyfox.modules.admin.image.model.TbImageAlbum;
import com.jflyfox.modules.admin.image.service.ImageAlbumService;
import com.jflyfox.modules.front.Home;
import com.jflyfox.util.StrUtils;
import com.jflyfox.util.cache.Cache;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 图片前台
 * 
 * 2016年2月5日 上午11:23:24 flyfox 369191470@qq.com
 */
@ControllerBind(controllerKey = "/front/imageshow")
public class ImageShowFrontController extends BaseController {

	private static final String path = "/pages/admin/imageshow/imageshow_";

	public void list() {
		TbImage model = getModelByAttr(TbImage.class);

		SQLUtils sql = new SQLUtils(" from tb_image_album t where 1=1 ");
		if (model.getAttrValues().length != 0) {
			sql.setAlias("t");
			sql.whereEquals("parent_id", model.getAlbumId());
			sql.whereLike("name", model.getStr("name"));
			//sql.whereEquals("status", 1);
		}
		//if(model.getAlbumId()!=null) sql.append(" and t.parent_id = '"+model.getAlbumId()+"' ");
		sql.append(" and t.status = 1 and parent_id <> 0 ");
		// 排序
		String orderBy = getBaseForm().getOrderBy();
		if (StrUtils.isEmpty(orderBy)) {
			sql.append(" order by sort,id asc");
		} else {
			sql.append(" order by ").append(orderBy);
		}

		String sqlSelect = "select t.*,(select ifnull(im.image_net_url,im.image_url) " //
				+ " from tb_image im where im.album_id = t.id order by sort,id desc limit 1 ) as imageUrl ";
		//List<TbImageAlbum> list = TbImageAlbum.dao.find(sqlSelect + sql.toString());
		Paginator paginator = new Paginator();
		Integer pageNo = getParaToInt("pageNo",0);
		if (pageNo != null && pageNo > 0) {
			paginator.setPageNo(pageNo);
		}
		Integer pageSize = getParaToInt("recordsperpage",18);
		if (pageSize != null && pageSize > 0) {
			paginator.setPageSize(pageSize);
		}
		Page<TbImageAlbum> page = TbImageAlbum.dao.paginate(paginator, sqlSelect, //
				sql.toString());

		setAttr("page", page);
		setAttr("attr", model);
//		render(path + "list.html");
		render("/template/photo/home/home.html");
	}

	public void edit() {
		int albumId = getParaToInt();
		TbImage model = getModelByAttr(TbImage.class);
		model.setAlbumId(albumId);

		SQLUtils sql = new SQLUtils(" from tb_image t where 1=1 ");
		if (model.getAttrValues().length != 0) {
			sql.setAlias("t");
			sql.whereLike("album_name", model.getStr("name"));
			sql.whereEquals("status", model.getInt("status"));
		}
		sql.whereEquals("album_id", model.getAlbumId());

		// 排序
		String orderBy = getBaseForm().getOrderBy();
		if (StrUtils.isEmpty(orderBy)) {
			sql.append(" order by sort,id desc");
		} else {
			sql.append(" order by ").append(orderBy);
		}

		List<TbImage> list = TbImage.dao.find("select t.* " + sql.toString());

		//相册信息
		setAttr("album", new ImageAlbumService().getAlbum(model.getAlbumId()));

		setAttr("list", list);
		setAttr("attr", model);
		render(path + "front_edit.html");
	}


}
