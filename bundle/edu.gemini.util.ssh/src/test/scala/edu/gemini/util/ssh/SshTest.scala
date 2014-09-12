package edu.gemini.util.ssh

import java.io.File
import java.io.PrintWriter
import org.junit._
import org.junit.Assert._
import scala.io.Source

@Ignore
class SshTest {
  private val user = "software"
  private val password = ""
  private val host = "tcumming-lnx01"
  private val sshConfig = new DefaultSshConfig(host, user, password, SshConfig.DEFAULT_TIMEOUT)
  private val updirs = List(new File("./mydir0"), new File("./mydir0/mydir1"))
  private val downdirs = List(new File("./downdir0"), new File("./downdir0/downdir1"))
  private val upfiles = List((new File("./mydir0/myfile0"), "hello world0"), (new File("./mydir0/mydir1/myfile1"), "hello world1"))
  private val downfiles = List(new File("./downdir0/downfile0"), new File("./downdir0/downdir1/downfile1"))

  def createFile(file: File, text: String) {
    if (file.exists) file.delete
    val printWriter = new PrintWriter(file)
    printWriter.print(text)
    printWriter.close
  }

  def verifyFile(file: File, text: String): Boolean = {
    if (!(file.exists)) return false
    val line = Source.fromFile(file).mkString
    return line.trim.equals(text)
  }

  @Before
  def setupTestCase() {
    updirs.foreach{ _.mkdir }
    upfiles.foreach{ case (file, text) => createFile(file, text) }
    downdirs.foreach{ _.mkdir }
  }

  @After
  def cleanupTestCase() {
    upfiles.filter{ case (file,text) => file.exists }.foreach{ case (file, _) => file.delete }
    updirs.filter{ _.exists }.foreach{ _.delete }
    downfiles.filter{ _.exists }.foreach{ _.delete }
    downdirs.filter{ _.exists }.foreach{ _.delete }
    SshExecSession.execute(sshConfig, "rm -rf up0")
  }

  @Test def testSftpCopyGet() {
    val results = for {
      i1 <- SftpSession.copy(sshConfig, upfiles(0)._1, "/home/software/up0");
      i2 <- SftpSession.get(sshConfig, "/home/software/up0/" + upfiles(0)._1.getName, downfiles(0))
    } yield ()
    assert(results.isSuccess, "Could not copy / get")
    assert(verifyFile(downfiles(0), upfiles(0)._2))
  }

  @Test def testSftpMv() {
    val results = for {
      sftpSession <- SftpSession.connect(sshConfig);
      i1 <- sftpSession.remoteMkDir("/home/software/up0", createParentDirs = false);
      i2 <- sftpSession.copyLocalToRemote(upfiles(0)._1, "/home/software/up0", overwrite=false);
      i3 <- sftpSession.remoteCd("/home/software/up0");
      i4 <- sftpSession.remoteMv("myfile0", "tmpfile", overwrite=false);
      i5 <- sftpSession.copyRemoteToLocal("tmpfile", downfiles(0))
    } yield ()
    assert(results.isSuccess, "Could not copy / get")
    assert(verifyFile(downfiles(0), upfiles(0)._2))
  }
}