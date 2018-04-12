package de.tarent.telekom.cot.mqtt.util;

import io.vertx.core.buffer.Buffer;

public class SmartREST {

    private SmartREST() {
        // Private constructor to prevent instantiation.
    }

    public static String getPayloadCheckManagedObject(final String xid, final String iccid) {
        return "15," + xid + "\n600," + iccid;
    }

    public static String getPayloadSelfCreationRequest(final String xid, final String iccid, final String deviceName) {
        return "15," + xid + "\n602," + iccid + "," + deviceName;
    }

    public static String getPayloadRegisterICCIDasExternalId(final String xid, final String managedObjectId, final String iccid) {
        return "15," + xid + "\n604," + managedObjectId + "," + iccid;
    }

    public static String getPayloadUpdateOperations(final String xid, final String managedObjectId) {
        return "15," + xid + "\n606," + managedObjectId;
    }

    public static String[] parseResponsePayload(final Buffer payload) {
        String[] payloadLines = new String(payload.getBytes()).split("\n");

        return payloadLines[1].split(",");
    }
}
