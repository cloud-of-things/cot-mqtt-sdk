## API to service access to the MQTT broker for bootstrapping and CoT interaction

The nbIoT-MQTT API allows device developers to bootstrap their devices against
**Cumulocity** and allows subscribing and publishing with the **MQTT-Broker**
without having to implement it themselves. The API is based on **Vert.x**.

### Include the API in a project
**Maven**:
```xml
<dependency>
    <groupId>de.tarent.telekom.cot</groupId>
    <artifactId>nbiot-mqtt</artifactId>
    <version>0.5.2-SNAPSHOT</version>    
</dependency>
```
 
**Gradle:**
```groovy
compile "de.tarent.telekom.cot:nbiot-mqtt:0.5.2-SNAPSHOT"
```

#### You can find the repository here:
https://infinity-wbench.wesp.telekom.net/gitlab/nbiot-connector/nbiot-mqtt/

bzw. unter

https://github.com/cloud-of-things/cot-mqtt-sdk

#### JavaDocs

The API docs are located in https://cloud-of-things.github.io/cot-mqtt-sdk/


### Instructions
After the API is included in your Project, you can use the method `getInstance()` to
initialise the API. _In case you implemented your own Vert.x instance, you
should pass your instance in the aforementioned mentioned method (`getInstance(Vertx)`)._

#### Available methods:
**registerDevice** - Handles the bootstrapping process and returns the encoded password
created for the IoT cloud plus the ID of the generated managed objects in the cloud.
The method requires the **DeviceId(ICCID)** and a **Property** object of the type
`java.util.Properties` as parameters. The Property object should hold the following
values: _initialPassword, initialUser, brokerPort, brokerURI, QoS (optional) and
the xID_ for the SmartREST templates found on the CoT tenant. After successfully 
receiving the credentials, the method creates the ManagedObject for the device in
the CoT and adds the necessary fragments for receiving commands. So that the bootstrap
process can be properly processed, it's important that the SmartREST templates
stored in the MOCReationTemplates are added to the CoT tenant on top of your
own SmartREST templates.

**subscribeToTopic** - Creates a subscription on the desired broker for the device.
The method requires the **DeviceId(ICCID)** and a **Property** object of the type
`java.util.Properties` as parameters. The Property object should hold the following
values: _password, user, brokerPort, brokerURI, QoS (optional)_.

**publishTopic** - Publishes a given **message** parameter on the Device's channel.
The method requires the **DeviceId(ICCID)** and a **Property** object of the type
`java.util.Properties` as parameters. The Property object should hold the following
values: _password, user, brokerPort, brokerURI, QoS (optional)_.

#### Other important information:
_**SSL**_ This value can be turned off if the property `"ssl", "false"` ist set in
the Property object given in the above methods. If you want to use SSL, then you have
to either set the following Properties: `"keyStorePath", <pathToKeystore>`;
`"keyStorePassword", <keyStorePassword>` or create a Directory where the jar
file exists called "certificates" and place a client.jks file that contains both
certificates and uses the password "kVJEgEVwn3TB9BPA" within.

_**QoS**_ Needs to have one of the following values: **0** (at most once), **1** 
(at least once), **2** (exactly once), or left empty if it doesn't matter. _The
default is always **1** (at least once)_.

Furthermore, all methods require a **Callback** so that the method results can
be returned.

###Release notes
####Version 0.5.2-SNAPSHOT
- *bootstrapping* - nbIoT-devices can now be bootstrapped and receives the
credentials from CoT, persists device data in the nbIoT environment and creates
managed Objects over SmartREST for shell access.
- *messaging* - API helps the devices with subscribing on topics to receive messages
from the MQTT-Broker and helps with sending SmartREST messages to the CoT.