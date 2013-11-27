SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

CREATE TABLE IF NOT EXISTS `devices` (
  `id`           INT(10)              NOT NULL AUTO_INCREMENT,
  `source`       VARCHAR(255)         NOT NULL,
  `uuid`         VARCHAR(255)         NOT NULL,
  `type`         VARCHAR(255)
                 CHARACTER SET latin1 NOT NULL,
  `manufname`    VARCHAR(255)         NOT NULL,
  `node`         INT(2)               NOT NULL,
  `status`       VARCHAR(50)          NOT NULL,
  `name`         VARCHAR(255)         NOT NULL,
  `zone`         INT(2)               NOT NULL,
  `internaltype` VARCHAR(255)         NOT NULL,
  `productname`  VARCHAR(255)         NOT NULL,
  `internalname` VARCHAR(255)         NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

-- --------------------------------------------------------

--
-- Структура таблицы `log`
--

CREATE TABLE IF NOT EXISTS `log` (
  `id`      INT(10)                                NOT NULL AUTO_INCREMENT,
  `date`    TIMESTAMP                              NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `level`   ENUM('DEBUG', 'INFO', 'WARN', 'ERROR') NOT NULL,
  `message` VARCHAR(255)                           NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

-- --------------------------------------------------------

--
-- Структура таблицы `messages`
--

CREATE TABLE IF NOT EXISTS `messages` (
  `id`      INT(10)      NOT NULL AUTO_INCREMENT,
  `time`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `subject` VARCHAR(255) NOT NULL,
  `sender`  VARCHAR(255) NOT NULL,
  `class`   VARCHAR(255) NOT NULL,
  `json`    TEXT         NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

CREATE TABLE IF NOT EXISTS `modulestatus` (
  `id`       INT(10)      NOT NULL AUTO_INCREMENT,
  `name`     VARCHAR(100) NOT NULL,
  `lastseen` TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `state`    VARCHAR(50)  NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;


CREATE TABLE IF NOT EXISTS `scheduler` (
  `id`       INT(10)      NOT NULL AUTO_INCREMENT,
  `date`     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `class`    VARCHAR(255) NOT NULL,
  `command`  VARCHAR(255) NOT NULL,
  `type`     INT(1)       NOT NULL,
  `validto`  TIMESTAMP    NOT NULL DEFAULT '0000-00-00 00:00:00',
  `interval` VARCHAR(100) NOT NULL,
  `enabled`  INT(1)       NOT NULL,
  `language` VARCHAR(20)  NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;


INSERT INTO `scheduler` (`id`, `date`, `class`, `command`, `type`, `validto`, `interval`, `enabled`, `language`) VALUES
(1, '2013-11-27 03:00:00', 'Say', 'Доброе утро!', 1, '0000-00-00 00:00:00', '0 0 7 ? * MON,TUE,WED,THU,FRI *', 1, 'ru');

CREATE TABLE IF NOT EXISTS `devicesvalues` (
  `id`    INT(10)      NOT NULL AUTO_INCREMENT,
  `uuid`  VARCHAR(150) NOT NULL,
  `label` VARCHAR(255) NOT NULL,
  `value` VARCHAR(255) NOT NULL,
  `type`  VARCHAR(50)  NOT NULL,
  `units` VARCHAR(50)  NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
