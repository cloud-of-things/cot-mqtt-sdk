+++API to service access to MQTT broker for bootstrapping and cot interaction 

Das NBIOT-MQTT SDK ermöglicht es Device-Entwicklern den Bootstrap-Vorgang gegen die Cumulocity 
und anschliessendes Subscriben und Publishen am HiveMQ durchzuführen ohne diese selbst zu implementieren. 
Das SDK baut auf Vert.x auf.

Das SDK ist folgendermaßen per Maven einzubinden:
`<dependency>
     <groupId>de.tarent.telekom.cot</groupId>
      <artifactId>nbiot-mqtt</artifactId>
      <version>0.5.0-SNAPSHOT</version>
 </dependency>`
 
Oder alternativ per Gradle auf folgendem Weg:
`compile "de.tarent.telekom.cot:nbiot-mqtt:0.5.0-SNAPSHOT"`

Das Repository befindet sich hier:
https://infinity-wbench.wesp.telekom.net/gitlab/nbiot-connector/nbiot-mqtt/


Nachdem man das SDK in seinem Projekt eingebunden hat, kann man über die Methode getInstance() das SDK initialisieren.
Im Falle, dass man selbst eine Vert.x-Anwendung entwickelt sollte man dieser die vertx Instanz mitgeben.

Folgende Methoden stehen zur Verfügung:
registerDevice - Handelt den Bootstrapping Vorgang ab und liefert das Passwort. Bekommt die DeviceID(ICCID) übergeben 
und ein Property-Objekt vom Typ java.util.Properties mit entsprechenden Values für initialPassword,initialUser,brokerPort,brokerURI.

subscribeToTopic - Erstellt eine Subscription am gewünschten Broker für das Device. Bekommt die DeviceID(ICCID) übergeben 
und ein Property-Objekt vom Typ java.util.Properties mit entsprechenden Values für Password, User, brokerPort, brokerURI.

publishMessage - Veröffentlicht auf dem für das Device entsprechenden Kanal eine Message, die als Parameter übergeben wird. 
Bekommt weiterhin die DeviceID(ICCID) und ein Property-Objekt vom Typ java.util.Properties mit entsprechenden Values für Password, User, brokerPort, 
brokerURI übergeben.

Weiterhin muss allen Methoden ein Callback übergeben werden, über dass die Ergebnisse der Methoden zurückgegeben werden.
