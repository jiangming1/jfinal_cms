package com.jflyfox.modules.front.service;

import java.util.List;

import com.jfinal.plugin.activerecord.Page;
import com.jflyfox.jfinal.base.BaseService;
import com.jflyfox.jfinal.base.Paginator;
import com.jflyfox.jfinal.component.db.SQLUtils;
import com.jflyfox.modules.admin.image.model.TbImage;
import com.jflyfox.modules.admin.image.model.TbImageAlbum;
import com.jflyfox.util.StrUtils;
import com.jflyfox.util.cache.CacheManager;

public class FrontImageService extends BaseService {

	private final static String cacheName = "FrontImageService";

	/**
	 * 更新缓存,清空
	 * 
	 * 2015年4月29日 下午4:37:40 flyfox 369191470@qq.com
	 */
	public void clearCache() {
		 CacheManager.get(cacheName).clear();
	}

	/**
	 * 返回相册列表
	 * 
	 * 2016年2月10日 上午3:05:07 flyfox 369191470@qq.com
	 * 
	 * @return
	 */
	public List<TbImageAlbum> getAlbumList() {
		String key = "albumList";
		String sql = "select * from tb_image_album t where  status = 1 order by sort,id desc";
		return TbImageAlbum.dao.findCache(cacheName, key, sql);
	}

	/**
	 * 返回相册分页
	 */
	public Page<TbImageAlbum> getAlbumPage(Paginator paginator , int albumId){
		String key = ("photoAlbum_" + paginator.getPageNo() + "_" + paginator.getPageSize());
		SQLUtils sql = new SQLUtils(" from tb_image_album t where 1=1 ");
//			if (model.getAttrValues().length != 0) {
//				sql.setAlias("t");
//				sql.whereEquals("album_id", model.getAlbumId());
//				sql.whereLike("name", model.getStr("name"));
//				sql.whereEquals("status", model.getInt("status"));
//			}


			sql.append(" and t.status = 1 and parent_id <> 0 ");
			// 排序
//			String orderBy = getBaseForm().getOrderBy();
//			if (StrUtils.isEmpty(orderBy)) {
//				sql.append(" order by sort,id desc");
//			} else {
//				sql.append(" order by ").append(orderBy);
//			}
		String sqlSelect = "select t.*,(select ifnull(im.image_net_url,im.image_url) " //
					+ " from tb_image im where im.album_id = t.id order by sort,id desc limit 1 ) as imageUrl ";
//		Paginator paginator = new Paginator();
//		int a = 1;
//		int b = 18;
//		paginator.setPageNo(a);
//		paginator.setPageSize(b);
		Page<TbImageAlbum> page = TbImageAlbum.dao.paginateCache(cacheName, key, paginator, sqlSelect , sql.toString());
		return page;
	}

	/**
	 * 返回相册类型
	 * @return
	 */
	public List<TbImageAlbum> getAlbumType() {
		String key = "albumType";
		String sql = "select * from tb_image_album t where  status = 1 and parent_id = 0 order by sort,id desc";
		return TbImageAlbum.dao.findCache(cacheName, key, sql);
	}

	/**
	 * 查询相册
	 * 
	 * 2016年2月10日 上午2:51:57 flyfox 369191470@qq.com
	 * 
	 * @param albumId
	 * @return
	 */
	public TbImageAlbum getAlbum(Integer albumId) {
		String key = "album_" + albumId;
		String sql = "select * from tb_image_album t where id = ? ";
		return TbImageAlbum.dao.findFirstCache(cacheName, key, sql, albumId);
	}

	/**
	 * 查询图片
	 * 
	 * 2016年2月10日 上午3:45:05
	 * flyfox 369191470@qq.com
	 * @param paginator
	 * @return
	 */
	public Page<TbImage> getImage(Paginator paginator) {
		String key = ("article_" + paginator.getPageNo() + "_" + paginator.getPageSize());
		Page<TbImage> images = TbImage.dao.paginateCache(cacheName, key, paginator, "select * " //
				, " from tb_image " //
						+ " where status = 1 " // 查询状态为显示
						+ " order by sort,create_time desc");
		return images;
	}

	/**
	 * 查询图片
	 * 
	 * 2016年2月10日 上午3:45:18
	 * flyfox 369191470@qq.com
	 * @param paginator
	 * @param albumId
	 * @return
	 */
	public Page<TbImage> getImage(Paginator paginator, int albumId) {
		String key = ("article_" + albumId + "_" + paginator.getPageNo() + "_" + paginator.getPageSize());
		Page<TbImage> images = TbImage.dao.paginateCache(cacheName, key, paginator, "select * " //
				, " from tb_image " //
						+ " where status = 1 " // 查询状态为显示
						+ " and album_id =  ? " //
						+ " order by sort,create_time desc", albumId);
		return images;
	}

	/**
	 * 查询图片
	 * 
	 * 2016年2月10日 上午3:45:26
	 * flyfox 369191470@qq.com
	 * @param imageId
	 * @return
	 */
	public TbImage getImage(Integer imageId) {
		String key = "image_" + imageId;
		String sql = "select * from tb_image t where id = ? ";
		return TbImage.dao.findFirstCache(cacheName, key, sql, imageId);
	}

	/**
	 * 查询推荐图片
	 * 
	 * 2015年4月29日 下午4:48:24 flyfox 369191470@qq.com
	 * 
	 * @param paginator
	 * @return
	 */
	public Page<TbImage> getRecommendImages(Paginator paginator) {
		String key = ("recommendImage_" + paginator.getPageNo() + "_" + paginator.getPageSize());
		Page<TbImage> articles = TbImage.dao.paginateCache(cacheName, key, paginator, "select * " //
				, " from tb_image  where status = 1 " //
						+ " and is_recommend = 1 " // 推荐文章
						+ " order by sort,create_time desc");
		return articles;
	}
}
