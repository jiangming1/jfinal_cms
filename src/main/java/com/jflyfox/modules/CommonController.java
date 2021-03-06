package com.jflyfox.modules;

import com.jfinal.aop.Before;
import com.jfinal.plugin.activerecord.Page;
import com.jfinal.plugin.ehcache.CacheKit;
import com.jflyfox.component.base.BaseProjectController;
import com.jflyfox.component.util.ImageCode;
import com.jflyfox.component.util.JFlyFoxUtils;
import com.jflyfox.jfinal.base.Paginator;
import com.jflyfox.jfinal.component.annotation.ControllerBind;
import com.jflyfox.jfinal.component.db.SQLUtils;
import com.jflyfox.modules.admin.folder.FolderService;
import com.jflyfox.modules.admin.folder.TbFolder;
import com.jflyfox.modules.admin.image.model.TbImage;
import com.jflyfox.modules.admin.image.model.TbImageAlbum;
import com.jflyfox.modules.admin.site.SessionSite;
import com.jflyfox.modules.front.Home;
import com.jflyfox.modules.front.interceptor.FrontInterceptor;
import com.jflyfox.system.dict.DictCache;
import com.jflyfox.system.log.SysLog;
import com.jflyfox.system.user.SysUser;
import com.jflyfox.system.user.UserCache;
import com.jflyfox.util.Config;
import com.jflyfox.util.NumberUtils;
import com.jflyfox.util.StrUtils;
import com.jflyfox.util.task.job.SpiderJob;
import com.jflyfox.util.task.job.UploadOssJob;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommonController
 */
@ControllerBind(controllerKey = "/")
public class CommonController extends BaseProjectController {

    public static final String loginPage = "/login.html";
    public static final String firstPage = "/home";

//	public void test() {
//		//new SpiderJob().run();
//	}

    /**
     * 首页，菜单
     * <p>
     * 2015年5月25日 下午11:00:28 flyfox 369191470@qq.com
     */
    @Before(FrontInterceptor.class)
    public void index() {
        // new FrontService().menu(this);
        int folderRoot = TbFolder.ROOT;
        SessionSite site = getSessionSite();
        Integer siteFolderId = site.getModel().getSiteFolderId();
        if (siteFolderId != null && siteFolderId > 0) {
            folderRoot = siteFolderId;
        }

        String folderStr = getPara();
        Integer folderId = folderRoot;

        if (folderStr != null) {
            if (NumberUtils.parseInt(folderStr) > 0) {
                folderId = NumberUtils.parseInt(folderStr);
            } else {
                folderId = NumberUtils.parseInt(FolderService.getMenu(folderStr, site.getSiteId()));
            }
        }

        if (folderId == null || folderId <= 0) {
            folderId = folderRoot;
        }
        // 活动目录
        setAttr("folders_selected", folderId);

        TbFolder folder = new FolderService().getFolder(folderId);
        setAttr("folder", folder);

        setAttr("paginator", getPaginator());

        // seo：title优化
        String folderName = (folder == null ? "" : folder.getStr("name") + " - ");
        setAttr(JFlyFoxUtils.TITLE_ATTR, folderName + getAttr(JFlyFoxUtils.TITLE_ATTR));

        // 栏目跳转规则
        String jumpUrl = folder.getJumpUrl();
        String path = folder.getPath();
        String urlKey = folder.getKey();
        if (StrUtils.isNotEmpty(jumpUrl)) {
            redirectAuto(jumpUrl);
        } else if (StrUtils.isNotEmpty(path)) {
            renderAuto(path);
        } else {
//			renderAuto(Home.PATH + urlKey + ".html");

//			renderAuto("login.html");

            SysUser user = (SysUser) getSessionUser();
            if (user == null) {
                renderAuto(loginPage);
                return;
            }
            setAttr("nowUser", user);
            //跳转到自定义相册展示
            TbImage model = getModelByAttr(TbImage.class);

            SQLUtils sql = new SQLUtils(" from tb_image_album t where 1=1 ");
//			if (model.getAttrValues().length != 0) {
//				sql.setAlias("t");
//				sql.whereEquals("album_id", model.getAlbumId());
//				sql.whereLike("name", model.getStr("name"));
//				sql.whereEquals("status", model.getInt("status"));
//			}


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
            Integer pageNo = getParaToInt("pageNo", 0);
            if (pageNo != null && pageNo > 0) {
                paginator.setPageNo(pageNo);
            }
            Integer pageSize = getParaToInt("recordsperpage", 18);

            if (pageSize != null && pageSize > 0) {
                paginator.setPageSize(pageSize);
            }
            Page<TbImageAlbum> page = TbImageAlbum.dao.paginate(paginator, sqlSelect, //
                    sql.toString());
            setAttr("page", page);
//			setAttr("attr", model);
            renderAuto(Home.PATH + urlKey + ".html");
        }

    }

    /**
     * 登录
     *
     * @author flyfox 2013-11-11
     */
    @Before(FrontInterceptor.class)
    public void login() {
        // 获取验证码
        String imageCode = getSessionAttr(ImageCode.class.getName());
        String checkCode = this.getPara("imageCode");

        if (StrUtils.isEmpty(imageCode) || !imageCode.equalsIgnoreCase(checkCode)) {
            setAttr("msg", "验证码错误！");
            renderAuto(loginPage);
            return;
        }

        // 初始化数据字典Map
        String username = getPara("username");
        String password = getPara("password");

        // 新加入，判断是否有上一个页面
        String prePage = getPara("pre_page");
        String toPage = StrUtils.isEmpty(prePage) || prePage.indexOf("login") >= 0 //
                || prePage.indexOf("trans") >= 0 ? firstPage : prePage;
        setAttr("pre_page", prePage); // 如果密码错误还需要用到

        if (StrUtils.isEmpty(username)) {
            setAttr("msg", "用户名不能为空");
            renderAuto(loginPage);
            return;
        } else if (StrUtils.isEmpty(password)) {
            setAttr("msg", "密码不能为空");
            renderAuto(loginPage);
            return;
        }
        String encryptPassword = JFlyFoxUtils.passwordEncrypt(password); // 加密
        SysUser user = SysUser.dao.findFirstByWhere(" where username = ? and password = ? " //
                        + " and usertype != " + JFlyFoxUtils.USER_TYPE_THIRD // 第三方的只能通过oauth登录
                , username, encryptPassword);

        if (user == null || user.getInt("userid") <= 0) {
            setAttr("msg", "认证失败，请您重新输入。");
            renderAuto(loginPage);
            return;
        } else {
            setSessionUser(user);
        }

        // 添加日志
        user.put("update_id", user.getUserid());
        user.put("update_time", getNow());
        saveLog(user, SysLog.SYSTEM_LOGIN);

        redirect(toPage);
    }

    /**
     * 登出
     *
     * @author flyfox 2013-11-13
     */
    @Before(FrontInterceptor.class)
    public void logout() {
        SysUser user = (SysUser) getSessionUser();
        if (user != null) {
            // 添加日志
            user.put("update_id", user.getUserid());
            user.put("update_time", getNow());
            saveLog(user, SysLog.SYSTEM_LOGOUT);
            // 删除session
            removeSessionUser();
        }

        setAttr("msg", "您已退出");
        renderAuto(loginPage);
    }

    public void update_cache() {
        DictCache.init();
        UserCache.init();
        renderHtml("1");
    }

    public void trans() {
        String redirectPath = getPara();
        if (StrUtils.isEmpty(redirectPath)) {
            redirectPath = Config.getStr("PAGES.TRANS");
        } else if (redirectPath.equals("auth")) {
            redirectPath = "/pages/error/trans_no_auth.html";
        }
        render(redirectPath);
    }
}
