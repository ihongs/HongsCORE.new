package io.github.ihongs.serv.matrix;

import io.github.ihongs.Cnst;
import io.github.ihongs.Core;
import io.github.ihongs.CoreLogger;
import io.github.ihongs.HongsException;
import io.github.ihongs.action.ActionHelper;
import io.github.ihongs.util.Synt;
import io.github.ihongs.util.daemon.Async;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 级联操作队列
 * @author Kevin
 */
public class Casc {

    private enum ACTION {UPDATE, DELETE};

    private static final Async QUEUE = new Queue();

    private static final class Queue extends Async <Group> {

        public Queue () {
            super ("matrix.cascade", Integer.MAX_VALUE, 1);
        }

        @Override
        public void run (Group group) {
            group.run();
        }
    }

    private static final class Group implements  Runnable  {

        public final     ACTION  ac;
        public final     Object  id;
        public final Set<String> aq;

        public Group(Set<String> aq, Object id, ACTION ac) {
            this.aq = aq;
            this.id = id;
            this.ac = ac;
        }

        @Override
        public void run () {
            Core core = Core.getInstance();
            long time = System.currentTimeMillis() / 1000;
            try {
                // 设置会话用户 ID, 规避更新错误
                ActionHelper hlpr = ActionHelper.newInstance();
                hlpr.setSessibute(Cnst.UID_SES, Cnst.ADM_UID );
                core.set(ActionHelper.class.getName( ), hlpr );

                switch (ac) {
                    case UPDATE: update (aq, id, time); break ;
                    case DELETE: delete (aq, id, time); break ;
                }
            }
            catch (Exception|Error e) {
                CoreLogger.error ( e);
            }
            finally {
                core.reset();
            }
        }

    }

    public static void update(Set<String> aq, Object id) {
        if (id != null && aq != null && ! aq.isEmpty( )) {
            QUEUE.add (new Group(aq, id, ACTION.UPDATE));
        }
    }

    public static void delete(Set<String> aq, Object id) {
        if (id != null && aq != null && ! aq.isEmpty( )) {
            QUEUE.add (new Group(aq, id, ACTION.DELETE));
        }
    }

    public static void update(Set<String> aq, Object id, long ct) throws HongsException {
        for(String at : aq) {
            if (at == null || at.isEmpty()) {
                continue;
            }

            // 格式: conf.form?fk#DELETE#UPDATE
            int     p = at.indexOf  ("#");
            if (0 < p ) {
            String tk = at.substring(0+p);
            if ( ! tk.contains("#UPDATE") ) {
                continue;
            }      at = at.substring(0,p);
            }
                    p = at.indexOf  ("?");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  ("!");
            String  f = at.substring(1+p);
            String  c = at.substring(0,p);

            update(Data.getInstance(c, f), fk, id, ct);
        }
    }

    public static void delete(Set<String> aq, Object id, long ct) throws HongsException {
        for(String at : aq) {
            if (at == null || at.isEmpty()) {
                continue;
            }

            // 格式: conf.form?fk#DELETE#UPDATE
            int     p = at.indexOf  ("#");
            if (0 < p ) {
            String tk = at.substring(0+p);
            if ( ! tk.contains("#DELETE") ) {
                continue;
            }      at = at.substring(0,p);
            }
                    p = at.indexOf  ("?");
            String fk = at.substring(1+p);
                   at = at.substring(0,p);
                    p = at.indexOf  ("!");
            String  f = at.substring(1+p);
            String  c = at.substring(0,p);

            delete(Data.getInstance(c, f), fk, id, ct);
        }
    }

    public static void update(Data inst, String fk, Object fv, long ct) throws HongsException {
        // 可能多个关联指向同一资源
        Map  ar = new HashMap();
        Set  or = new HashSet();
        Set  rb = new HashSet();
        ar.put(Cnst.OR_KEY, or);
        ar.put(Cnst.RB_KEY, rb);
        rb.add(Cnst.ID_KEY    );
        for (String fn : fk.split(";")) {
            or.add(Synt.mapOf(fn , fv));
        }

        Data.Loop loop = inst.search(ar, 0, 0);
        String  fn = inst.getFormId();
        for (Map info : loop) {
            String id = (String) info.get(Cnst.ID_KEY);
            inst.set(id, Synt.mapOf(
                "__meno__" , "system.cascade",
                "__memo__" , "Update cascade " + fn + ":" + id
            ) , ct);
            CoreLogger.debug("Update cascade {} {}:{}", fn, fk, id);
        }
    }

    public static void delete(Data inst, String fk, Object fv, long ct) throws HongsException {
        // 可能多个关联指向同一资源
        Map  ar = new HashMap();
        Set  or = new HashSet();
        Set  rb = new HashSet();
        ar.put(Cnst.OR_KEY, or);
        ar.put(Cnst.RB_KEY, rb);
        rb.add(Cnst.ID_KEY    );
        for (String fn : fk.split(";")) {
            or.add(Synt.mapOf(fn , fv));
        }

        Data.Loop loop = inst.search(ar, 0, 0);
        String  fn = inst.getFormId();
        for (Map info : loop) {
            String id = (String) info.get(Cnst.ID_KEY);
            inst.cut(id, Synt.mapOf(
                "__meno__" , "system.cascade",
                "__memo__" , "Delete cascade " + fn + ":" + id
            ) , ct);
            CoreLogger.debug("Delete cascade {} {}:{}", fn, fk, id);
        }
    }

}
