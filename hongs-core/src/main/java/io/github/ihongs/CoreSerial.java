package io.github.ihongs;

import io.github.ihongs.util.reflex.Block;
import io.github.ihongs.util.reflex.Block.Larder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * 本地缓存工具
 *
 * <p>
 * 不是 transient,static,final 的属性可被持久化存储;
 * 特殊情况可重载 load(Object) 及 save() 自定义处理.
 * </p>
 *
 * <h3>特别注意:</h3>
 * <pre>
 * entrySet(),keySet(),values() 返回的对象无法被序列化,
 * 请 new HashSet(x.entrySet()) 预处理后再存入当前对象.
 * 详见: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4756277
 * </pre>
 *
 * <h3>异常代码:</h3>
 * <pre>
 * 区间: 0x10d0~0x10df
 * 0x10d0 创建临时文件失败
 * 0x10d2 写入临时文件失败
 * 0x10d4 读取临时文件失败
 * 0x10d6 找不到临时文件
 * 0x10d8 找不到对应的类
 * 0x10da 无法读取属性
 * 0x10dc 无法写入属性
 * </pre>
 *
 * @author Hongs
 */
public abstract class CoreSerial
  implements Serializable
{

  /**
   * 从外部获取属性写入当前对象
   * @throws io.github.ihongs.HongsException
   */
  abstract protected void imports()
    throws HongsException;

  /**
   * 加载或引入数据
   * @param name
   * @throws io.github.ihongs.HongsException
   */
  protected final void init(String name)
    throws HongsException
  {
    init(new File(Core.DATA_PATH + File.separator + "serial" + name + ".ser"));
  }

  /**
   * 加载或引入数据
   * @param file
   * @throws io.github.ihongs.HongsException
   */
  protected final void init(File file)
    throws HongsException
  {
    Larder lock = Block.getLarder(CoreSerial.class.getName() + ":" + file.getAbsolutePath());
    byte   code ;

    lock.lockr();
    try {
      code = read(file);
      if (code > 0 ) {
        return;
      }
    } finally {
      lock.unlockr();
    }

    lock.lockw();
    try {
      if (code < 0 ) {
      if (file.exists()) {
          file.delete();
      } return;
      }
      imports ();
      save(file);
    } finally {
      lock.unlockw();
    }
  }

  /**
   * 从文件读取数据
   * @param file
   * @return 0 重载, 1 有效, -1 无效(删除文件)
   * @throws io.github.ihongs.HongsException
   */
  protected byte read(File file)
    throws HongsException
  {
    if (file.exists()) {
      load(file);
      return 1;
    } else {
      return 0;
    }
  }

  /**
   * 从文件加载对象
   * @param file
   * @throws io.github.ihongs.HongsException
   */
  protected final void load(File file)
    throws HongsException
  {
    try
    {
        FileInputStream fis = null;
      ObjectInputStream ois = null;
      try
      {
        fis = new   FileInputStream(file);
        ois = new ObjectInputStream(fis );
        load(ois.readObject());
      }
      finally
      {
        if (ois != null) ois.close();
        if (fis != null) fis.close();
      }
    }
    catch (ClassNotFoundException ex)
    {
      throw new HongsException(0x10d8, ex);
    }
    catch (FileNotFoundException ex)
    {
      throw new HongsException(0x10d6, ex);
    }
    catch (IOException ex)
    {
      throw new HongsException(0x10d4, ex);
    }
  }

  /**
   * 将对象存入文件
   * @param file
   * @throws io.github.ihongs.HongsException
   */
  protected final void save(File file)
    throws HongsException
  {
    // 文件不存在则创建
    if (!file.exists()) {
      File dn = file.getParentFile(  );
      if (!dn.exists()) {
           dn.mkdirs();
      }
      try {
        file.createNewFile( );
      } catch (IOException e) {
        throw new HongsException(0x10d0,e);
      }
    }

    try
    {
        FileOutputStream fos = null;
      ObjectOutputStream oos = null;
      try
      {
        fos = new   FileOutputStream(file);
        oos = new ObjectOutputStream(fos );
        oos.writeObject(save());
        oos.flush();
      }
      finally
      {
        if (oos != null) oos.close();
        if (fos != null) fos.close();
      }
    }
    catch (FileNotFoundException ex)
    {
      throw new HongsException(0x10d6, ex);
    }
    catch (IOException ex)
    {
      throw new HongsException(0x10d2, ex);
    }
  }

  /**
   * 从缓存获取属性写入当前对象
   * @param obj
   * @throws io.github.ihongs.HongsException
   */
  protected void load(Object obj)
    throws HongsException
  {
    Map     map = ( Map ) obj ;
    Class   clazz = getClass();
    Field[] fields;

    // 设置所有公共字段
    fields = clazz.getFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();

      try
      {
        field.set(this, map.get(name));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10da, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10da, e);
      }
    }

    // 设置所有非公字段
    fields = clazz.getDeclaredFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isPublic(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();
      field.setAccessible  ( true );

      try
      {
        field.set(this, map.get(name));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10da, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10da, e);
      }
    }
  }

  /**
   * 从当前对象获取属性写入缓存
   * @return
   * @throws io.github.ihongs.HongsException
   */
  protected Object save()
    throws HongsException
  {
    Map     map =new HashMap();
    Class   clazz = getClass();
    Field[] fields;

    // 提取所有公共字段
    fields = clazz.getFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();

      try
      {
        map.put(name, field.get(this));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10da, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10da, e);
      }
    }

    // 提取所有非公字段
    fields = clazz.getDeclaredFields();
    for (Field field : fields)
    {
      int ms = field.getModifiers();
      if (Modifier.isTransient(ms )
      ||  Modifier.isPublic(ms )
      ||  Modifier.isStatic(ms )
      ||  Modifier.isFinal (ms))
      {
        continue;
      }

      String name = field.getName();
      field.setAccessible  ( true );

      try
      {
        map.put(name, field.get(this));
      }
      catch (IllegalAccessException e)
      {
        throw new HongsException(0x10dc, e);
      }
      catch (IllegalArgumentException e)
      {
        throw new HongsException(0x10dc, e);
      }
    }

    return map;
  }

  /**
   * 缓存最后更新时间
   */
  public static interface LastModified
  {

    public long lastModified();

  }

}
