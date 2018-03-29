## API to service access to the MQTT broker for bootstrapping and CoT interaction

Das NBIOT-MQTT SDK ermöglicht es Device-Entwicklern den Bootstrap-Vorgang gegen die **Cumulocity** 
und anschliessendes Subscriben und Publishen am **MQTT Broker** durchzuführen ohne diese selbst zu implementieren. 
Das SDK baut auf **Vert.x** auf.

### Das SDK in Projekten Einbinden
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

#### Das Repository befindet sich hier:
https://infinity-wbench.wesp.telekom.net/gitlab/nbiot-connector/nbiot-mqtt/

bzw. unter

https://github.com/cloud-of-things/cot-mqtt-sdk

#### JavaDocs

The API docs are located in https://cloud-of-things.github.io/cot-mqtt-sdk/


### Anleitung
Nachdem man das SDK in seinem Projekt eingebunden hat, kann man über die Methode `getInstance()` das SDK initialisieren.
_Im Falle, dass man selbst eine Vert.x-Anwendung entwickelt sollte man dieser die vertx Instanz mitgeben._

#### Verfügbare Methoden:
**registerDevice** - Handelt den Bootstrapping Vorgang ab und liefert das Passwort für die IoT-Cloud zurück, sowie die ID, des generierten 
Managed Objects in der Cloud. Bekommt die **DeviceID(ICCID)** übergeben 
und ein **Property-Objekt** vom Typ java.util.Properties mit entsprechenden Values für 
_initialPassword, initialUser, brokerPort, brokerURI, QoS (optional) und der xID_ für der SmartREST Templates im CoT-Tenant.
Die Methode erstellt weiterhin, nach erfolgreichem Erhalt der Credentials, das ManagedObject für das Device in der CoT hinzu und fügt diesem 
die notwendigen Fragmente zum Erhalt von Commands hinzu. Damit der Bootstrap-Vorgang abgehandelt werden kann, ist es zwingend erforderlich, dass die in der
MOCreationTemplates gespeicherten SmartREST-Templates zusätzlich zu den eigenen SmartREST-Templates in den CoT-Tenant eingepflegt sind.

**subscribeToTopic** - Erstellt eine Subscription am gewünschten Broker für das Device. Bekommt die **DeviceID(ICCID)** übergeben 
und ein **Property-Objekt** vom Typ java.util.Properties mit entsprechenden Values für _password, user, brokerPort, brokerURI, QoS (optional)_.

**publishMessage** - Veröffentlicht auf dem für das Device entsprechenden Kanal eine **Message**, die als Parameter übergeben wird. 
Bekommt weiterhin die **DeviceID(ICCID)** und ein **Property-Objekt** vom Typ java.util.Properties mit entsprechenden Values für _password, user, brokerPort, 
brokerURI, QoS (optional)_ übergeben.

#### Sonstiges:
_**SSL**_ kann ausgeschaltet werden indem Man eine Property "ssl", "false" eingibt (Default ist true). Wenn man aber SSL verwenden will, muss eine Directory "certificates" angeleget werden, wo das jar liegt, und eine client.jsk mit beide Zertifikaten angelegt werden mit folgenden Passwort: kVJEgEVwn3TB9BPA
Alternativ kann eine selbst angelegten Keystore und Passwort verwendet werden mit folgende Properties: "keyStorePath", <pathToKeystore>; "keyStorePassword", <keyStorePassword>

_**QoS**_ muss einer von folgende Werte haben: **0** (at most once), **1** (at least once), **2** (exactly once), oder leer gelassen werden, wenn es egal ist. _Default ist immer **1** (at least once)_. 

Weiterhin muss allen Methoden ein **Callback** übergeben werden, über das die Ergebnisse der Methoden zurückgegeben werden.
