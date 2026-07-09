package org.example.config;

import java.io.File;
import java.time.ZoneId;
import java.util.List;

public final class Constants {

    private Constants() {}

    public static final class Config {
        private Config() {}
        public static final String APP_VERSION = "1.4.1";
        public static final String LOG_DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
        public static final int MAX_SEARCH_PARAMS = 4;
        public static final String QCONFIG_QMGR_LIST_NAME = "qmgrList";
        public static final String QMCONFIG_ENV_NAME = "environment";
        public static final String SEARCH_DT_FORMAT = "yyyyMMddHHmm";
        public static final String TEST_QM_NAME = "QM1";
        public static final String TEST_Q_NAME = "DEV.QUEUE.1";
        public static final ZoneId ZONE_ID = ZoneId.of("Europe/Berlin");
    }

    public static final class Directories {
        private Directories() {}
        public static final String WORKING_DIR = System.getProperty("user.home") + File.separator + "MQSearchData";
    }

    public static final class Environments {
        private Environments() {}
        public static final String DEFAULT_ENV = "DEV";
        public static final List<String> ENVIRONMENT_LIST = List.of("DEV");
    }

    public static final class Extensions {
        private Extensions() {}
        public static final String JSON = ".json";
        public static final String XML = ".xml";
        public static final String TXT = ".txt";
    }

    public static final class Filenames {
        private Filenames() {}
        public static final String QUEUE_CONFIG = "qconfig.json";
        public static final String QUEUE_MANAGER_CONFIG = "qmconfig.json";
        public static final String TEST_MESSAGE_JSON = "TestMessage1.json";
        public static final String TEST_MESSAGE_XML = "TestMessage2.xml";
        public static final String TEST_MESSAGE_TXT = "TestMessage3.txt";
    }

    public static final class Messages {
        private Messages() {}
        public static final String ERROR_IO_EXCEPTION = "[ERROR] An I/O exception has occurred:";
        public static final String ERROR_MQ_EXCEPTION = "[ERROR] An MQ exception has occurred:";
    }
}
