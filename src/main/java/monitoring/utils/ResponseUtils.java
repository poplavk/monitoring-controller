package monitoring.utils;

import org.apache.logging.log4j.Logger;
import spark.Response;

/**
 * @author Ilya Sapachev <a href="mailto:sid-hugo@yandex-team.ru"></a>
 * @date 31.10.16
 */
public class ResponseUtils {
    private ResponseUtils() { }

    public static String getError(String error, int errorCode, Response response, Logger logger) {
        logger.error(error);
        response.status(errorCode);
        return error;
    }

    public static String getOk(String msg, int httpCode, Response response, Logger logger) {
        logger.info(msg);
        response.status(httpCode);
        return msg;
    }
}
