## API to service access to the MQTT broker for bootstrapping and CoT interaction

Das NBIOT-MQTT SDK ermöglicht es Device-Entwicklern den Bootstrap-Vorgang gegen die **Cumulocity** 
und anschliessendes Subscriben und Publishen am **HiveMQ** durchzuführen ohne diese selbst zu implementieren. 
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


### Anleitung
Nachdem man das SDK in seinem Projekt eingebunden hat, kann man über die Methode `getInstance()` das SDK initialisieren.
_Im Falle, dass man selbst eine Vert.x-Anwendung entwickelt sollte man dieser die vertx Instanz mitgeben._

#### Verfügbare Methoden:
**registerDevice** - Handelt den Bootstrapping Vorgang ab und liefert das Passwort. Bekommt die **DeviceID(ICCID)** übergeben 
und ein **Property-Objekt** vom Typ java.util.Properties mit entsprechenden Values für _initialPassword, initialUser, brokerPort, brokerURI, QoS (optional)_.

**subscribeToTopic** - Erstellt eine Subscription am gewünschten Broker für das Device. Bekommt die **DeviceID(ICCID)** übergeben 
und ein **Property-Objekt** vom Typ java.util.Properties mit entsprechenden Values für _password, user, brokerPort, brokerURI, QoS (optional)_.

**publishMessage** - Veröffentlicht auf dem für das Device entsprechenden Kanal eine **Message**, die als Parameter übergeben wird. 
Bekommt weiterhin die **DeviceID(ICCID)** und ein **Property-Objekt** vom Typ java.util.Properties mit entsprechenden Values für _password, user, brokerPort, 
brokerURI, QoS (optional)_ übergeben.

#### Sonstiges:
_**QoS**_ muss einer von folgende Werte haben: **0** (at most once), **1** (at least once), **2** (exactly once), oder leer gelassen werden, wenn es egal ist. _Default ist immer **0** (at most once)_. 

Weiterhin muss allen Methoden ein **Callback** übergeben werden, über dass die Ergebnisse der Methoden zurückgegeben werden.
