package io.github.ihongs.action.serv;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsExemption;
import io.github.ihongs.action.ActionDriver;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.action.anno.Action;
import io.github.ihongs.action.anno.CustomReplies;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.HashSet;
import java.util.TreeSet;
import java.util.Set;
import java.util.regex.Pattern;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 自动处理过滤器
 *
 * <h3>初始化参数(init-param):</h3>
 * <pre>
 * url-exclude  排除的 URL, 可用","分割多个, 可用"*"为通配符
 * url-include  包含的 URL, 可用","分割多个, 可用"*"为通配符
 * action-path  默认动作地址
 * layout-path  默认页面地址
 * </pre>
 * <p>
 * 注意:
 * action-path, layout-path 与 filter-mapping 的 url-pattern
 * 需要避免从属关系, 否则会造成死循环.
 * action-path, layout-path 在 filter-mapping 的 url-pattern
 * 内部时需将其排除, 即 url-exclude.
 * </p>
 *
 * @author Hongs
 */
public class AutoFilter extends ActionDriver {

    private String action;
    private String layout;
    private URLPatterns ignore = null;
    private Set<String> layset = null;
    private Set<String> actset = null;
    private Set<String> cstset = null;

    // 禁止访问: xxx.xxx.jsp
    private static final Pattern DENY_JSPS = Pattern.compile(".*(/_|\\.)[^/]*\\.jsp$"); // [_#$]

    @Override
    public void init(FilterConfig cnf) throws ServletException {
        super.init(cnf);

        action = cnf.getInitParameter("action-path");
        layout = cnf.getInitParameter("layout-path");
        if (action == null) {
            action ="/common/auto";
        }
        if (layout == null) {
            layout =  action;
        }

        // 获取不包含的URL
        this.ignore = new URLPatterns(
            cnf.getInitParameter("url-exclude"),
            cnf.getInitParameter("url-include")
        );
    }

    @Override
    public void destroy() {
        super.destroy();
        actset = null;
        layset = null;
    }

    @Override
    public void doFilter(Core core, ActionHelper hlpr, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse rsp = hlpr.getResponse();
        HttpServletRequest  req = hlpr.getRequest( );
        String url = ActionDriver.getRecentPath(req);

        // 检查是否需要跳过
        if (ignore != null && ignore.matches(url)) {
            chain.doFilter( req , rsp );
            return;
        }

        // 禁止访问动作脚本, 避免绕过权限过滤
        if (DENY_JSPS.matcher( url ).matches(   )) {
            rsp.sendError ( HttpServletResponse.SC_NOT_FOUND , "What's your problem?" );
            return;
        }

        if (url.endsWith(Cnst.API_EXT)) {
            /**
             * 为避免一个动作有多种路径,
             * 这很可能导致权限校验歧义,
             * 故这里不用做类似下方处理;
             * 上方的 DENY_JSPS 同此理.
             */
        } else
        if (url.endsWith(Cnst.ACT_EXT)) {
            String act; // 动作路径
            String src; // 资源路径
//          String met; // 动作方法
            String uri;
            int    pos;

            try {
                pos = url.lastIndexOf('.');
                act = url.substring(1,pos);

                pos = act.lastIndexOf('/');
                src = act.substring(0,pos);
            } catch (IndexOutOfBoundsException ex) {
                // 如果无法拆分则直接跳过
                chain.doFilter( req, rsp );
                return;
            }

            // 检查是否有特定动作脚本
            uri = "/" + src + "/__main__.jsp";
            if (new File(Core.BASE_PATH+ uri).exists()) {
                include ( req, rsp, url, uri);
                return;
            }

            uri =  layout  +  "/__main__.jsp";
            if (new File(Core.BASE_PATH+ uri).exists()) {
                include ( req, rsp, url, uri);
                return;
            }

            if (ActionRunner.getActions().containsKey(act) == false) {
                for(String axt: getacts()) {
                    if (act.endsWith(axt)) {
                        if (cstset.contains(axt)) {
                            forward(req, rsp, url, action + axt + Cnst.ACT_EXT);
                        } else {
                            include(req, rsp, url, action + axt + Cnst.ACT_EXT);
                        }
                        return;
                    }
                }
            }
        } else {
            // 默认引导页总是叫 default.html
            if (url.endsWith("/")) {
                url  = url + "default.html";
            }

            if (new File(Core.BASE_PATH + url).exists() == false) {
                // xxx.xxx > xxx.xxx.jsp
                String jsp = url + ".jsp";

                for(String uri: getlays()) {
                    if (jsp.endsWith(uri)) {
                        forward(req, rsp, url, layout + uri);
                        return;
                    }
                    if (url.endsWith(uri)) {
                        forward(req, rsp, url, layout + uri);
                        return;
                    }
                }
            }
        }

        chain.doFilter(req, rsp);
    }

    private void include(ServletRequest req, ServletResponse rsp, String url, String uri)
            throws ServletException, IOException {
        // 虚拟路径
        req.setAttribute(Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get());
        req.setAttribute(Cnst.ACTION_ATTR, url.substring(1));
        // 转发请求
        req.getRequestDispatcher( uri ).include( req , rsp );
    }

    private void forward(ServletRequest req, ServletResponse rsp, String url, String uri)
            throws ServletException, IOException {
        // 虚拟路径
        req.setAttribute(Cnst.ORIGIN_ATTR, Core.ACTION_NAME.get());
        req.setAttribute(Cnst.ACTION_ATTR, url.substring(1));
        // 转发请求
        req.getRequestDispatcher( uri ).forward( req , rsp );
    }

    private Set<String> getacts() {
        if (null != actset) {
            return  actset;
        }

        // 总是通过 search 获取动作 class
        // 即使无需 search 也要存在 search 方法才行
        Class cls;
        try {
            cls = ActionRunner.getActions()
                              .get(action.substring(1) + "/search")
                              .getMclass( );
        } catch ( NullPointerException ex ) {
            throw new HongsExemption(0x1130,
                 "Auto action '" + action.substring(1) + "/search' is not exists", ex);
        }

        cstset = new HashSet();
        actset = new TreeSet(new Comparator<String>( ) {
            @Override
            public int compare( String o1, String o2 ) {
                // 对比两个动作路径层级数
                // 优先匹配层级更深的动作
                int i, c1 = 0, c2 = 0;
                i = 0;
                while ((i = o1.indexOf('/', i)) != -1) {
                    i  ++;
                    c1 ++;
                }
                i = 0;
                while ((i = o2.indexOf('/', i)) != -1) {
                    i  ++;
                    c2 ++;
                }
                i = Integer.compare(c2, c1);
                return i != 0 ? i : 1;
            }
        });

        for (Method mtd : cls.getMethods( )) {
             Action ann = mtd.getAnnotation(Action.class);
            if (null != ann) {
                String  uri;
                if (!"".equals(ann.value())) {
                    uri = "/"+ ann.value(  );
                } else {
                    uri = "/"+ mtd.getName();
                }
                if (mtd.isAnnotationPresent(CustomReplies.class)) {
                    cstset.add(uri);
                }
                    actset.add(uri);
            }
        }

        return actset;
    }

    private Set<String> getlays() {
        if (null != layset) {
            return  layset;
        }

        File dir = new File(Core.BASE_PATH + layout);
        if (!dir.exists( )) {
            throw new HongsExemption(0x1131,
                 "Auto layout '" + layout.substring(1) + "' is not exists");
        }
        if (!dir.isDirectory()) {
            throw new HongsExemption(0x1131,
                 "Auto layout '" + layout.substring(1) + "' is not a directory");
        }

        /**
         * 这里不需要管层级的深度
         * 下面是按越深越先加入的
         */
        layset = new LinkedHashSet();

        // 递归获取目录下所有文件
        getlays(layset, dir, "/");

        return  layset;
    }

    private void getlays(Set layset, File dx, String dn) {
        File[] fs = dx.listFiles(  );
        if (null == fs) {
            return;
        }

        Set tmpset;
        tmpset = new LinkedHashSet();

        for ( File fx : fs ) {
            String fn = fx.getName();
            if (fn.startsWith (".")
            ||  fn.startsWith ("_")) {
                continue;
            }

            if (fx.isFile     (   )) {
                tmpset.add (dn + fn);
            } else
            if (fx.isDirectory(   )) {
                getlays(layset , fx , dn + fn + "/");
            }
        }

        layset.addAll(tmpset);
    }

}
