package com.ibm.geds.hdfs

import org.junit.jupiter.api._
import org.junit.jupiter.api.Assertions._

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs._

class TestGEDSHDFS {

  val fs = TestGEDSHDFS.fs

  @Test
  def createFilePrefix(): Unit = {
    val stream = fs.create(new Path(f"/test/createFilePrefix/file"))
    stream.close()
    var ls = fs.listStatus(new Path("/test/createFilePrefix"))
    assertEquals(1, ls.length)
    println(ls(0).getPath().getName())
    assertEquals("file", ls(0).getPath().getName())

    ls = fs.listStatus(new Path("/test/createFilePrefix/"))
    assertEquals(1, ls.length)
    println(ls(0).getPath().getName())
    assertEquals("file", ls(0).getPath().getName())
  }

  @Test
  def createFile(): Unit = {
    val stream = fs.create(new Path("test/createFile/file"))
    stream.close()
    var ls = fs.listStatus(new Path("test/createFile"))
    assertEquals(1, ls.length)
    println(ls(0).getPath().getName())
    assertEquals("file", ls(0).getPath().getName())
    ls = fs.listStatus(new Path("test/createFile/"))
    assertEquals(1, ls.length)
    println(ls(0).getPath().getName())
    assertEquals("file", ls(0).getPath().getName())
  }

  @Test
  def append(): Unit = {
    val path = new Path("test/append")
    var pos = 0
    // Create empty
    var stream = fs.create(path)
    stream.close()
    var fstatus = fs.getFileStatus(path)
    assertEquals(pos, fstatus.getLen())
    assertFalse(fstatus.isDirectory())

    // Append + write 1 byte
    stream = fs.append(path)
    stream.write(1)
    pos += 1
    assertEquals(pos, stream.getPos())
    stream.flush()
    stream.close()

    // Ensure 1 byte got written
    fstatus = fs.getFileStatus(path)
    assertEquals(pos, fstatus.getLen())

    // Append + write 1 byte
    stream = fs.append(path)
    stream.writeByte(1)
    pos += 1
    assertEquals(pos, stream.getPos())
    stream.close()

    // Ensure 1 byte got written (total length: 2)
    fstatus = fs.getFileStatus(path)
    assertEquals(pos, fstatus.getLen())
    assertFalse(fstatus.isDirectory())
  }
}

object TestGEDSHDFS {
  var fs: FileSystem = _;

  @BeforeAll
  def setup(): Unit = {
    val bucket = sys.env.get("TEST_BUCKET").getOrElse("test")
    val conf = new Configuration()
    conf.set("fs.default.name", "geds")
    conf.set("fs.default.fs", "geds")
    conf.set("fs.geds.impl", "com.ibm.geds.hdfs.GEDSHadoopFileSystem")
    conf.set("fs.geds.path", "/tmp/geds")
    conf.set(
      f"fs.geds.${bucket}.accessKey",
      sys.env.get("AWS_ACCESS_KEY_ID").getOrElse("minioadmin")
    )
    conf.set(
      f"fs.geds.${bucket}.secretKey",
      sys.env.get("AWS_SECRET_ACCESS_KEY").getOrElse("minioadmin")
    )
    conf.set(
      f"fs.geds.${bucket}.endpoint",
      sys.env.get("AWS_ENDPOINT_URL").getOrElse("http://localhost:9000")
    )
    fs = FileSystem.get(new java.net.URI(f"geds://${bucket}"), conf)
    System.out.println(
      "@BeforeAll - executes once before all test methods in this class"
    )
  }
}
