package io.github.ihongs.util.verify;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.action.ActionRunner;
import io.github.ihongs.util.Synt;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 唯一规则
 *
 * <pre>
 * 规则参数:
 *  data-ut 查询动作名, 末尾可添加 #其他参与唯一的字段
 *  diverse 并非用在这, 而是被用于 Repeated 中去除重复
 * </pre>
 *
 * @author Hongs
 */
public class Diverse extends Rule {

    @Override
    public Object verify(Value watch) throws Wrong {
        // 跳过空值和空串
        Object value = watch.get();
        if (value  ==  null ) {
            return STAND;
        }
        if (value.equals("")) {
            return STAND;
        }

        String ut = (String) getParam("data-ut" );
        String uk = (String) getParam("data-uk" );
        String nk = (String) getParam("__name__");
        String ck = (String) getParam("__conf__");
        String fk = (String) getParam("__form__");

        if (ut == null || ut.isEmpty()) {
            ut = ck + "/" + fk + "/search" ;
        }
        if (uk == null && uk.isEmpty()) {
            uk = nk ;
        }

        // 请求数据
        Map cd = new HashMap();
        Map rd = new HashMap();
        rd.put(Cnst.PN_KEY, 0);
        rd.put(Cnst.RN_KEY, 1);
        rd.put(Cnst.RB_KEY, Synt.setOf (  Cnst.ID_KEY  ) );

        // 更新需排除当前记录
        if (watch.isUpdate( )) {
            Object vo = watch.getValues().get(Cnst.ID_KEY);
            Map ne = new HashMap( );
            ne.put(Cnst.NE_REL, vo);
            rd.put(Cnst.ID_KEY, ne);
        }

        // 参与唯一约束的字段
        Set<String> ks = Synt.toTerms ( uk );
        if (null != ks) for (String kn: ks ) {
            rd.put( kn , watch.getValues().get(kn) );
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
                return value;
        }
        if (sd.containsKey("list")) {
           List list = (List) sd.get("list");
            if (list == null || list.isEmpty()) {
                return value;
            }
        } else
        if (sd.containsKey("info")) {
            Map info = (Map ) sd.get("info");
            if (info == null || info.isEmpty()) {
                return value;
            }
        } else
        if (sd.containsKey("page")) {
            Map page = (Map ) sd.get("page");
            if (page == null || page.isEmpty()) {
                return value;
            } else
            if (page.containsKey("count")
            &&  Synt.declare(page.get("count"), 0) == 0) {
                return value;
            } else
            if (page.containsKey("pages")
            &&  Synt.declare(page.get("pages"), 0) == 0) {
                return value;
            }
        }

        throw new Wrong("fore.form.is.not.unique");
    }

}
