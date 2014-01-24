SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT = @@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS = @@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION = @@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

-- --------------------------------------------------------

--
-- Table structure for table `devices`
--

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
  `date`         TIMESTAMP            NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `message`      VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

--
-- Dumping data for table `devices`
--

-- --------------------------------------------------------

--
-- Table structure for table `devicesvalues`
--

CREATE TABLE IF NOT EXISTS `devicesvalues` (
  `id`         INT(10)      NOT NULL AUTO_INCREMENT,
  `uuid`       VARCHAR(150) NOT NULL,
  `label`      VARCHAR(255) NOT NULL,
  `value`      VARCHAR(255) NOT NULL,
  `type`       VARCHAR(50)  NOT NULL,
  `units`      VARCHAR(50)  NOT NULL,
  `isReadonly` BIT(1)       NOT NULL DEFAULT b'0',
  `date`       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `level`      VARCHAR(255) DEFAULT NULL,
  `message`    VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

--
-- Dumping data for table `devicesvalues`
--

-- --------------------------------------------------------

--
-- Table structure for table `events`
--

CREATE TABLE IF NOT EXISTS `events` (
  `id`        INT(11)      NOT NULL,
  `subject`   VARCHAR(255) NOT NULL,
  `script`    VARCHAR(255) NOT NULL,
  `isEnabled` BIT(1)       NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8;

--
-- Dumping data for table `events`
--

INSERT INTO `events` (`id`, `subject`, `script`, `isEnabled`) VALUES
  (1, 'event.devices.zwave.value.changed', 'dimmerChange.js', b'1'),
  (2, 'event.ai.response.object.light', 'testAI.js', b'1');

-- --------------------------------------------------------

--
-- Table structure for table `log`
--

CREATE TABLE IF NOT EXISTS `log` (
  `id`      BIGINT(20)   NOT NULL AUTO_INCREMENT,
  `date`    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `level`   VARCHAR(255) DEFAULT NULL,
  `message` VARCHAR(255) DEFAULT NULL,
  `event`   VARCHAR(255) NOT NULL,
  `uuid`    VARCHAR(255) NOT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

-- --------------------------------------------------------

--
-- Table structure for table `messages`
--

CREATE TABLE IF NOT EXISTS `messages` (
  `id`      BIGINT(20) NOT NULL AUTO_INCREMENT,
  `class`   VARCHAR(255) DEFAULT NULL,
  `json`    TEXT,
  `sender`  VARCHAR(255) DEFAULT NULL,
  `subject` VARCHAR(255) DEFAULT NULL,
  `time`    TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =1;

--
-- Dumping data for table `messages`
--

-- --------------------------------------------------------

--
-- Table structure for table `modulestatus`
--

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

--
-- Dumping data for table `modulestatus`
--


-- --------------------------------------------------------

--
-- Table structure for table `scheduler`
--

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
  AUTO_INCREMENT =2;

--
-- Dumping data for table `scheduler`
--

INSERT INTO `scheduler` (`id`, `date`, `class`, `command`, `type`, `validto`, `interval`, `enabled`, `language`) VALUES
  (1, '2014-01-27 03:00:00', 'Say', 'Доброе утро!', 1, '0000-00-00 00:00:00', '0 0 7 ? * MON,TUE,WED,THU,FRI *', 1,
   'ru');

-- --------------------------------------------------------

--
-- Table structure for table `speaks`
--

CREATE TABLE IF NOT EXISTS `speaks` (
  `id`         BIGINT(20) NOT NULL AUTO_INCREMENT,
  `confidence` DOUBLE DEFAULT NULL,
  `date`       TIMESTAMP  NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `device`     VARCHAR(255) DEFAULT NULL,
  `isActive`   BIT(1)     NOT NULL,
  `text`       TEXT,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =127;

--
-- Dumping data for table `speaks`
--


-- --------------------------------------------------------

--
-- Table structure for table `user`
--

CREATE TABLE IF NOT EXISTS `user` (
  `id`       BIGINT(20) NOT NULL AUTO_INCREMENT,
  `email`    VARCHAR(255) DEFAULT NULL,
  `name`     VARCHAR(255) DEFAULT NULL,
  `password` VARCHAR(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
)
  ENGINE =InnoDB
  DEFAULT CHARSET =utf8
  AUTO_INCREMENT =2;

--
-- Dumping data for table `user`
--

INSERT INTO `user` (`id`, `email`, `name`, `password`) VALUES
  (1, 'bob@gmail.com', 'Bob', 'secret');

/*!40101 SET CHARACTER_SET_CLIENT = @OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS = @OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION = @OLD_COLLATION_CONNECTION */;
