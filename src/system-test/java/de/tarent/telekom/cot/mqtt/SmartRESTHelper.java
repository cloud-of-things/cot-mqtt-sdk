package de.tarent.telekom.cot.mqtt;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class SmartRESTHelper {

    String name = "Temperature";
    String unit = "Celsius";
    String type = "c8y_TemperatureMeasurement";
    float value = 8.77f;

    Logger logger = LoggerFactory.getLogger(SmartRESTHelper.class);


    private String doubleToString(double value) {
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setDecimalSeparator('.');
        DecimalFormat decimalFormat = new DecimalFormat("0.#####", decimalFormatSymbols);
        return decimalFormat.format(value);
    }

    private String formatDate(long currentTimeMillis) {
        return ZonedDateTime.ofInstant(Instant.ofEpochMilli(currentTimeMillis), ZoneId.of("Europe/Berlin")).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    String getPayloadMeasurement(String source, String xid, String templateId) {

        String timeStamp = formatDate(System.currentTimeMillis());

        String payload = "15," + xid + "\n" + templateId + "," + name
            + ",T," + doubleToString(value) + "," + unit + "," + timeStamp + "," + source + "," + type;

        logger.info("Measurement payload: " + payload);

        return payload;
    }

}
