package com.algowebsolve.webapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MyFormatterService {

    Logger logger = LoggerFactory.getLogger(MyFormatterService.class);

    MyFormatterService() {
        logger.info("FormatterService inited");
    }

    public String formatString(String format, Object... args) {
        return String.format(format, args);
    }

}
