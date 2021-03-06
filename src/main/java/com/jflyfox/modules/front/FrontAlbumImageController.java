package com.jflyfox.modules.front;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Db;
import com.jflyfox.component.base.BaseProjectController;
import com.jflyfox.component.util.JFlyFoxUtils;
import com.jflyfox.jfinal.component.annotation.ControllerBind;
import com.jflyfox.modules.admin.image.model.TbImage;
import com.jflyfox.modules.admin.image.model.TbImageAlbum;
import com.jflyfox.modules.front.interceptor.FrontInterceptor;
import com.jflyfox.modules.front.service.FrontImageService;
import com.jflyfox.util.NumberUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerBind(controllerKey = "/album/image")
public class FrontAlbumImageController extends BaseProjectController {

	public static final String path = "/album/";

	/**
	 * 图片专辑
	 * 
	 * 2016年2月10日 上午3:43:39 flyfox 369191470@qq.com
	 */
	@Before(FrontInterceptor.class)
	public void index() {
		String albumIdStr = getPara();
		albumIdStr = albumIdStr.substring(5);
		int albumId = NumberUtils.parseInt(albumIdStr);
		// 活动目录
		setAttr("album_selected", albumId);

		TbImageAlbum album = new FrontImageService().getAlbum(albumId);
		setAttr("album", album);

		setAttr("paginator", getPaginator());

		// seo：title优化
		String albumName = (album == null ? "" : album.getName() + " - ");
		setAttr(JFlyFoxUtils.TITLE_ATTR, albumName + getAttr(JFlyFoxUtils.TITLE_ATTR));

		renderAuto(path + "common_album.html");
	}

	/**
	 * 图片
	 * 
	 * 2016年2月10日 上午3:43:47 flyfox 369191470@qq.com
	 */
	@Before(FrontInterceptor.class)
	public void img() {
		int imageId = getParaToInt();
		// 活动目录
		setAttr("imageId", imageId);

		TbImage image = new FrontImageService().getImage(imageId);
		setAttr("image", image);

		// 设置标签
		String tags = Db.findFirst("select group_concat(tagname) tags " //
				+ " from tb_image_tags where image_id = ? order by id", image.getId()).getStr("tags");
		setAttr("tags", tags);
				
		setAttr("paginator", getPaginator());

		// seo：title优化
		String imageName = (image == null ? "" : image.getName() + " - ");
		setAttr(JFlyFoxUtils.TITLE_ATTR, imageName + getAttr(JFlyFoxUtils.TITLE_ATTR));

		renderAuto(path + "common_image.html");
	}

	@Before(FrontInterceptor.class)
	public void imgAlbum() {
		Map<Object,Object> ret = new HashMap<Object,Object>();
		String sql ="select * from tb_image_album where 1=1 and remark IS NOT NULL order by ID asc";

		List<TbImageAlbum> tbImageAlbumList = TbImageAlbum.dao.find(sql);
		for(int i =0 ; i<tbImageAlbumList.size(); i++){
			String sql1 = "SELECT * FROM tb_image  WHERE album_id = '"+tbImageAlbumList.get(i).getId()+"'";
			List<TbImage> tbImages = TbImage.dao.find(sql1);

//			Map tbImageMap = new HashMap();
//			tbImageMap.put("tbImages",tbImages);
//			tbImageAlbum.put("tbImageMap",tbImageMap);
			if(tbImages.size()>0){
				TbImageAlbum tbImageAlbum = tbImageAlbumList.get(i);
				tbImageAlbum.put(tbImages.get(0));
				tbImageAlbumList.set(i,tbImageAlbum);
			}
		}
		ret.put("data",tbImageAlbumList);
		renderJson(ret);
	}
}
