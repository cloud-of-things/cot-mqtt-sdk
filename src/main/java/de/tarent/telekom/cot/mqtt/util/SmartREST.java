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

    public static String getPayloadUpdateOperations(String xid, String managedObjectId) {
        return "15,"+ xid +"\n606,"+managedObjectId;
    }

    public static String[] parseResponsePayload(Buffer payload){

        String[] payloadLines = new String(payload.getBytes()).split("\n");

        return payloadLines[1].split(",");

    }

}
