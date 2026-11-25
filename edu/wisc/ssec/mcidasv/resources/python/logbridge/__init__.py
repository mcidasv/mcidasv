import logging
from org.slf4j import Logger, LoggerFactory


__all__ = ["SLF4JHandler"]


class SLF4JHandler(logging.Handler):

    def __init__(self, logger=None):
        logging.Handler.__init__(self)
        if logger is None:
            logger = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME)
        self.logger = logger

    # No need for lock management of the interaction with SLF4J
    # itself, given that SLF4J ensures appropriate serialization

    def createLock(self):
        pass

    def acquire(self):
        pass

    def release(self):
        pass

    # Nor a need to flush - we are working with a logging system!

    def flush(self):
        pass

    # Avoid formatting messages if possible; doing this switch style lookup
    # is probably the least expensive way to do this
    def emit(self, record):
        try:
            level = record.levelno
            if self.logger.isErrorEnabled() and level >= logging.ERROR:
                msg = self.format(record)
                self.logger.error(msg)
            elif self.logger.isWarnEnabled() and level >= logging.WARNING:
                msg = self.format(record)
                self.logger.warn(msg)
            elif self.logger.isInfoEnabled() and level >= logging.INFO:
                msg = self.format(record)
                self.logger.info(msg)
            elif self.logger.isDebugEnabled() and level >= logging.DEBUG:
                msg = self.format(record)
                self.logger.debug(msg)
            elif self.logger.isTraceEnabled() and level >= logging.NOTSET:
                msg = self.format(record)
                self.logger.trace(msg)
        except (KeyboardInterrupt, SystemExit):
            raise
        except:
            self.handleError(record)
