package com.myodov.unicherrypicker

object LoggingConfigurator {

  def configure(): Unit = {
    import java.nio.file.Paths

    import ch.qos.logback.classic.encoder.PatternLayoutEncoder
    import ch.qos.logback.classic.spi.ILoggingEvent
    import ch.qos.logback.classic.{Level, LoggerContext}
    import ch.qos.logback.core.encoder.Encoder
    import ch.qos.logback.core.{Appender, FileAppender, Layout}
    import com.github.serioussam.syslogappender.{Protocol, SyslogAppender, SyslogConfig}
    import org.slf4j.LoggerFactory

    val ctx: LoggerContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]
    val rootLogger = ctx.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME)

    lazy val ple = {
      val e = new PatternLayoutEncoder
      e.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n")
      e.setContext(ctx)
      e.start
      e
    }

    lazy val pleSyslog = {
      val e = new PatternLayoutEncoder
      //      e.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n")
      //      e.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %replace(%msg){'\n', '#012'}%n")
      e.setPattern("%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %replace(%msg){'\\r', ''}%n")
      e.setContext(ctx)
      e.start
      e
    }

    val logFilePath = Paths.get(System.getProperty("user.dir"), "unicherrypicker-dump.log").toAbsolutePath.toString
    val logFilePath2 = Paths.get(System.getProperty("user.dir"), "unicherrypicker-dump2.log").toAbsolutePath.toString

    //    val appender: SyslogAppender[ILoggingEvent] = new SyslogAppender()[ILoggingEvent]
    lazy val fileAppender = {
      val a = new FileAppender
      a.setFile(logFilePath)
      a.setEncoder(ple.asInstanceOf[Encoder[Nothing]])
      a.setContext(ctx)
      a.start
      a
    }

    lazy val fileAppender2 = {
      val a = new FileAppender
      a.setFile(logFilePath2)
      a.setEncoder(ple.asInstanceOf[Encoder[Nothing]])
      a.setContext(ctx)
      a.start
      a
    }

    lazy val syslogAppender = {
      val a = new SyslogAppender
      val layout = pleSyslog.getLayout
      a.setLayout(layout.asInstanceOf[Layout[Nothing]])
      a.setSyslogConfig(
        {
          val cfg = new SyslogConfig()
          cfg.setHost("localhost")
          cfg.setPort(5140)
          cfg.setProgramName("UniCherrypicker")
          cfg.setProtocol(Protocol.TCP)
          cfg
        }
      )
      a.setContext(ctx)
      a.start
      a
    }

    //    val appender: SyslogAppender[ILoggingEvent] = new SyslogAppender()[ILoggingEvent]
    //    lazy val syslogAppender = {
    //      val a = new SyslogAppender
    //      a.setSyslogHost("localhost")
    //    }

    //    val fileAppender2 = new FileAppender
    //
    //    fileAppender2.setFile(Paths.get(System.getProperty("user.dir"), "unicherrypicker-dump2.log").toAbsolutePath.toString)
    //    fileAppender2.setEncoder(ple.asInstanceOf[Encoder[Nothing]])
    //    fileAppender2.setContext(ctx)
    //    fileAppender2.start

    rootLogger.addAppender(fileAppender.asInstanceOf[Appender[ILoggingEvent]])
//    rootLogger.addAppender(syslogAppender.asInstanceOf[Appender[ILoggingEvent]])
    rootLogger.setLevel(Level.DEBUG)

    //    rootLogger.setAdditive(false)
  }
}
