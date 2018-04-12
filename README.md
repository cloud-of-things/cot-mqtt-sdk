## API to service access to the MQTT broker for bootstrapping and CoT interaction

The nbIoT-MQTT API allows device developers to bootstrap their devices against
**Cumulocity** and allows subscribing and publishing with the **MQTT-Broker**
without having to implement it themselves. The API is based on **Vert.x**.

### Including the API in a project
**Maven**:
```xml
<dependency>
    <groupId>de.tarent.telekom.cot</groupId>
    <artifactId>nbiot-mqtt</artifactId>
    <version>version-number</version>    
</dependency>
```
 
**Gradle:**
```groovy
compile "de.tarent.telekom.cot:nbiot-mqtt:version-number"
```

#### You can find the repository here:
https://infinity-wbench.wesp.telekom.net/gitlab/nbiot-connector/nbiot-mqtt/

or the released version here:

https://github.com/cloud-of-things/cot-mqtt-sdk

#### JavaDocs

The API docs are located under https://cloud-of-things.github.io/cot-mqtt-sdk/


### User Manual
After the API is included in your Project, you can use the method `getInstance()` to
initialise the API. _In case you implemented your own Vert.x instance, you
should pass your instance in the aforementioned mentioned method (`getInstance(Vertx)`)._

#### Available methods:
* registerDevice(String, Properties, Consumer<String>)
* subscribeToTopic(String, Properties, Consumer<Object>, Consumer<String>)
* publishTopic(String, String, Properties, Consumer<Boolean>)
* unsubscribeFromTopic(String, Properties, Consumer<Boolean>)

For more information regarding these methods (explanation, examples, etc.) please
see the asciidoc located at "asciidoc/README.adoc".

### Release notes
#### Version 0.5.2-SNAPSHOT
- *bootstrapping* - nbIoT-devices can now be bootstrapped and receives the
credentials from CoT, persists device data in the nbIoT environment and creates
managed Objects over SmartREST for shell access.
- *messaging* - API helps the devices with subscribing on topics to receive messages
from the MQTT-Broker and helps with sending SmartREST messages to the CoT.