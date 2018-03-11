package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;

public class SmartREST {

    public static String getPayloadCheckManagedObject(String xid, String iccid) {
        return "15," + xid + "\n600," + iccid;
    }

    public static String getPayloadSelfCreationRequest(String xid, String iccid, String deviceName) {
        return "15," + xid + "\n602," + iccid + "," + deviceName;
    }

    public static String getPayloadRegisterICCIDasExternalId(String xid, String ManagedObjectId, String iccid) {
        return "15," + xid + "\n604," + ManagedObjectId + "," + iccid;
    }

    /*public static String getPayloadUpdateDeviceData(String managedObjectId) {

        String payload = "15,"+DEVICE_XID+ "\n605,"+managedObjectId+","+DEVICE_MODEL+","+
            DEVICE_REVISION_NUMBER+","+
            DEVICE_HARDWARE_SERIAL+","+DEVICE_APP_VERSION+","+DEVICE_BL_VERSION+","+
            DEVICE_BT_VERSION+","+DEVICE_MOD_VERSION+","+
            DEVICE_IMEI_NUMBER+","+DEVICE_ICCID+","+DEVICE_IMSI_NUMBER;
        LOGGER.debug("Update device data payload: " + payload);

        return payload;
    }*/

    public static String getPayloadUpdateOperations(String xid, String managedObjectId) {
        return "15,"+ xid +"\n606,"+managedObjectId;
    }

    public static String[] parseResponsePayload(Buffer payload){

        String[] payloadLines = new String(payload.getBytes()).split("\n");

        return payloadLines[1].split(",");

    }

}
