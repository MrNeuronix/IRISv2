SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

--
-- База данных: `iris`
--

-- --------------------------------------------------------

--
-- Структура таблицы `devices`
--

CREATE TABLE IF NOT EXISTS `devices` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `source` varchar(255) NOT NULL,
  `uuid` varchar(255) NOT NULL,
  `type` varchar(255) CHARACTER SET latin1 NOT NULL,
  `manufname` varchar(255) NOT NULL,
  `node` int(2) NOT NULL,
  `status` varchar(50) NOT NULL,
  `name` varchar(255) NOT NULL,
  `zone` int(2) NOT NULL,
  `internaltype` varchar(255) NOT NULL,
  `productname` varchar(255) NOT NULL,
  `internalname` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `devicesvalues`
--

CREATE TABLE IF NOT EXISTS `devicesvalues` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `uuid` varchar(150) NOT NULL,
  `label` varchar(255) NOT NULL,
  `value` varchar(255) NOT NULL,
  `type` varchar(50) NOT NULL,
  `units` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `log`
--

CREATE TABLE IF NOT EXISTS `log` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `level` enum('DEBUG','INFO','WARN','ERROR') NOT NULL,
  `message` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `messages`
--

CREATE TABLE IF NOT EXISTS `messages` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `subject` varchar(255) NOT NULL,
  `sender` varchar(255) NOT NULL,
  `class` varchar(255) NOT NULL,
  `json` text NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `modulestatus`
--

CREATE TABLE IF NOT EXISTS `modulestatus` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(100) NOT NULL,
  `lastseen` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `state` varchar(50) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

-- --------------------------------------------------------

--
-- Структура таблицы `scheduler`
--

CREATE TABLE IF NOT EXISTS `scheduler` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `class` varchar(255) NOT NULL,
  `command` varchar(255) NOT NULL,
  `type` int(1) NOT NULL,
  `validto` timestamp NOT NULL DEFAULT '0000-00-00 00:00:00',
  `interval` varchar(100) NOT NULL,
  `enabled` int(1) NOT NULL,
  `language` varchar(20) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=2 ;

INSERT INTO `scheduler` (`id`, `date`, `class`, `command`, `type`, `validto`, `interval`, `enabled`, `language`) VALUES
  (1, '2013-11-27 03:00:00', 'Say', 'Доброе утро!', 1, '0000-00-00 00:00:00', '0 0 7 ? * MON,TUE,WED,THU,FRI *', 1, 'ru');

-- --------------------------------------------------------

--
-- Структура таблицы `speaks`
--

CREATE TABLE IF NOT EXISTS `speaks` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `text` varchar(255) NOT NULL,
  `confidence` double NOT NULL,
  `device` varchar(255) NOT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB  DEFAULT CHARSET=utf8 AUTO_INCREMENT=1 ;

--
-- Структура таблицы `events`
--

CREATE TABLE IF NOT EXISTS `events` (
  `id` int(11) NOT NULL,
  `subject` varchar(255) NOT NULL,
  `script` varchar(255) NOT NULL,
  `isEnabled` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Дамп данных таблицы `events`
--

INSERT INTO `events` (`id`, `subject`, `script`, `isEnabled`) VALUES
  (1, 'event.devices.zwave.value.changed', 'dimmerChange.js', b'1');

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;

