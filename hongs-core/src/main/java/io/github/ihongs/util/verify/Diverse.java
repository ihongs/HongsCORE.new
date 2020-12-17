package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 唯一规则
 *
 * <pre>
 * 规则参数:
 *  data-ut 查询动作名
 *  data-uk 唯一字段名, 缺省为当前字段
 *  diverse 并非用在这, 而是被用于 Repeated 中去除重复
 * 注意事项:
 *  对于可以存储多个值的字段请慎重使用
 * </pre>
 *
 * @author Hongs
 */
public class Diverse extends Rule {

    private  final  Object FORCE;

    /**
     * 全局唯一校验
     * @param force
     */
    public Diverse(boolean force) {
        if (force) {
            FORCE = BLANK;
        } else {
            FORCE = STAND;
        }
    }

    public Diverse() {
        this (false);
    }

    @Override
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object  value  = watch.get( );
        if (FORCE == STAND) {
            if (value  ==  null ) {
                return STAND;
            }
            if (value.equals("")) {
                return STAND;
            }
        }

        Map nd = watch.getCleans(     );
        Object id = nd.get(Cnst.ID_KEY);
        String ut = (String) getParam("data-ut" );
        String uk = (String) getParam("data-uk" );
        String nk = (String) getParam("__name__");
        String ck = (String) getParam("__conf__");
        String fk = (String) getParam("__form__");

        if (ut == null || ut.isEmpty()) {
            ut = ck +"/"+ fk +"/search";
        }
        if (uk == null || uk.isEmpty()) {
            uk = nk ;
        }

        // 请求数据
        Map cd = new HashMap();
        Map rd = new HashMap();
        rd.put(Cnst.PN_KEY, 0);
        rd.put(Cnst.RN_KEY, 1);
        rd.put(Cnst.RB_KEY, Synt.setOf(Cnst.ID_KEY));

        // 更新需排除当前记录
        if (watch.isUpdate( )) {
            Map ne = new HashMap( );
            ne.put(Cnst.NE_REL, id);
            rd.put(Cnst.ID_KEY, ne);
        }

        // 参与唯一约束的字段
        Set us = Synt.toTerms(uk);
        Set ns = new HashSet (us);
        Iterator<String> it = ns.iterator( );
        while  (it.hasNext( )) {
            String n = it.next( );
            Object v ;
            if (nd.containsKey(n)) {
                it.remove ( );
                v = nd.get(n);
            } else
            if (nk.equals (n)) {
                it.remove ( );
                v = value;
            } else {
                continue ;
            }

            if (v == null) {
                rd.put(n, Synt.mapOf(Cnst.IS_REL, "null"));
            } else
            if (v.equals("") ) {
                rd.put(n, Synt.mapOf(Cnst.EQ_REL,   v   ));
            } else {
                rd.put(n, v);
            }
        }

        // 没提供任何值则跳过
        if (ns.size() == us.size()) {
            return FORCE;
        }

        // 补充缺的旧的字段值
        if (watch.isUpdate() && !ns.isEmpty()) {
            Map ud = new HashMap( );
            ud.put(Cnst.ID_KEY, id);
            ud.put(Cnst.RB_KEY, ns);
            ud.put(Cnst.RN_KEY, 0 );

            ActionHelper ah = ActionHelper.newInstance();
            ah.setContextData( cd );
            ah.setRequestData( ud );
            try {
                ActionRunner.newInstance(ah, ut).doInvoke();
            } catch (HongsException ex) {
                throw ex.toExemption( );
            }

            SD: {
                Map sd  = ah.getResponseData(  );
                if (sd == null) {
                    break SD;
                }

                if (sd.containsKey("list")) {
                    List sl = (List) sd.get("list");
                    if ( sl . isEmpty( ) ) break SD;
                    sd = (Map) sl.get(  00  );
                } else
                if (sd.containsKey("info")) {
                    sd = (Map) sd.get("info");
                } else {
                    break SD;
                }

                for(Object n : ns) {
                    Object v = sd.get(n);
                    if (v == null) {
                        rd.put(n, Synt.mapOf(Cnst.IS_REL, "null"));
                    } else
                    if (v.equals("")) {
                        rd.put(n, Synt.mapOf(Cnst.EQ_REL,   v   ));
                    } else
                    {
                        rd.put(n, v);
                    }
                }
            }
        }

        // 执行动作
        ActionHelper ah = ActionHelper.newInstance();
        ah.setContextData( cd );
        ah.setRequestData( rd );
        try {
            ActionRunner.newInstance(ah, ut).doInvoke();
        } catch (HongsException ex) {
            throw ex.toExemption( );
        }

        // 对比结果
        Map sd  = ah.getResponseData();
        if (sd == null) {
                return FORCE;
        }
        if (sd.containsKey("list")) {
           List list = (List) sd.get("list");
            if (list == null || list.isEmpty()) {
                return FORCE;
            }
        } else
        if (sd.containsKey("info")) {
            Map info = (Map ) sd.get("info");
            if (info == null || info.isEmpty()) {
                return FORCE;
            }
        } else
        if (sd.containsKey("page")) {
            Map page = (Map ) sd.get("page");
            if (page == null || page.isEmpty()) {
                return FORCE;
            } else
            if (page.containsKey("count")
            &&  Synt.declare(page.get("count"), 0) == 0) {
                return FORCE;
            } else
            if (page.containsKey("pages")
            &&  Synt.declare(page.get("pages"), 0) == 0) {
                return FORCE;
            }
        }

        throw new Wrong("fore.form.is.not.unique");
    }

}
