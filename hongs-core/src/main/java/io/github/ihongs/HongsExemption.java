package io.github.ihongs;

/**
 * 通用免责类
 *
 * 与 HongsException 不同, 无需 throws;
 *
 * <h3>取值范围:</h3>
 * <pre>
 * 核心: 400~999
 * 框架: 1000~9999
 * 用户: 10000~99999
 * </pre>
 *
 * @author Hongs
 */
public class HongsExemption
  extends  RuntimeException
  implements HongsCause {

    public static final int COMMON = 0;
    public static final int NOTICE = 1;

    protected final HongsCurse that;

    public HongsExemption(int code, String desc, Throwable fact) {
        super(fact);

        that = new HongsCurse(code, desc, this);
    }

    public HongsExemption(String desc, Throwable fact) {
        this(0x0 , null, fact);
    }

    public HongsExemption(int code, Throwable fact) {
        this(code, null, fact);
    }

    public HongsExemption(int code, String desc) {
        this(code, desc, null);
    }

    public HongsExemption(Throwable fact) {
        this(0x0 , null, fact);
    }

    public HongsExemption(String desc) {
        this(0x0 , desc, null);
    }

    public HongsExemption(int code) {
        this(code, null, null);
    }

    @Override
    public int getErrno() {
        return that.getErrno();
    }

    @Override
    public String getError() {
        return that.getError();
    }

    @Override
    public int getState() {
        return that.getState();
    }

    @Override
    public String getStage() {
        return that.getStage();
    }

    @Override
    public String toString() {
        return that.toString();
    }

    @Override
    public String getMessage() {
        return that.getMessage();
    }

    @Override
    public String getLocalizedMessage() {
        return that.getLocalizedMessage();
    }

    @Override
    public String getLocalizedContext() {
        return that.getLocalizedContext();
    }

    @Override
    public String getLocalizedContent() {
        return that.getLocalizedContent();
    }

    @Override
    public String[] getLocalizedOptions() {
        return that.getLocalizedOptions();
    }

    @Override
    public HongsExemption setLocalizedContext(String    lang) {
        that.setLocalizedContext(lang);
        return this;
    }

    @Override
    public HongsExemption setLocalizedContent(String    term) {
        that.setLocalizedContent(term);
        return this;
    }

    @Override
    public HongsExemption setLocalizedOptions(String... opts) {
        that.setLocalizedOptions(opts);
        return this;
    }

    @Override
    public HongsExemption toExemption() {
        return this;
    }

    @Override
    public HongsException toException() {
        return new HongsException(this.getErrno(), this.getError(), this)
             .setLocalizedContext(this.getLocalizedContext())
             .setLocalizedContent(this.getLocalizedContent())
             .setLocalizedOptions(this.getLocalizedOptions());
    }

    /**
     * 常规错误(无需错误代码)
     * @deprecated 请改用 HongsExemption(String, Throwable)
     */
    public static final class Common extends HongsExemption {
        public Common(String error, Throwable cause) {
            super(COMMON, error,cause);
        }
        public Common(Throwable cause) {
            super(COMMON, cause);
        }
        public Common(String error) {
            super(COMMON, error);
        }
    }

    /**
     * 通告错误(无需错误代码)
     * @deprecated 请改用 HongsExemption(String, Throwable)
     */
    public static final class Notice extends HongsExemption {
        public Notice(String error, Throwable cause) {
            super(NOTICE, error,cause);
        }
        public Notice(Throwable cause) {
            super(NOTICE, cause);
        }
        public Notice(String error) {
            super(NOTICE, error);
        }
    }

}
