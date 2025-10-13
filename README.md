# ♕ BYU CS 240 Chess

This project demonstrates mastery of proper software design, client/server architecture, networking using HTTP and WebSocket, database persistence, unit testing, serialization, and security.

## 10k Architecture Overview

The application implements a multiplayer chess server and a command line chess client.

[![Sequence Diagram](10k-architecture.png)](https://sequencediagram.org/index.html#initialData=C4S2BsFMAIGEAtIGckCh0AcCGAnUBjEbAO2DnBElIEZVs8RCSzYKrgAmO3AorU6AGVIOAG4jUAEyzAsAIyxIYAERnzFkdKgrFIuaKlaUa0ALQA+ISPE4AXNABWAexDFoAcywBbTcLEizS1VZBSVbbVc9HGgnADNYiN19QzZSDkCrfztHFzdPH1Q-Gwzg9TDEqJj4iuSjdmoMopF7LywAaxgvJ3FC6wCLaFLQyHCdSriEseSm6NMBurT7AFcMaWAYOSdcSRTjTka+7NaO6C6emZK1YdHI-Qma6N6ss3nU4Gpl1ZkNrZwdhfeByy9hwyBA7mIT2KAyGGhuSWi9wuc0sAI49nyMG6ElQQA)

## Modules

The application has three modules.

- **Client**: The command line program used to play a game of chess over the network.
- **Server**: The command line program that listens for network requests from the client and manages users and games.
- **Shared**: Code that is used by both the client and the server. This includes the rules of chess and tracking the state of a game.

## Starter Code

As you create your chess application you will move through specific phases of development. This starts with implementing the moves of chess and finishes with sending game moves over the network between your client and server. You will start each phase by copying course provided [starter-code](starter-code/) for that phase into the source code of the project. Do not copy a phases' starter code before you are ready to begin work on that phase.

## IntelliJ Support

Open the project directory in IntelliJ in order to develop, run, and debug your code using an IDE.

## Maven Support

You can use the following commands to build, test, package, and run your code.

| Command                    | Description                                     |
| -------------------------- | ----------------------------------------------- |
| `mvn compile`              | Builds the code                                 |
| `mvn package`              | Run the tests and build an Uber jar file        |
| `mvn package -DskipTests`  | Build an Uber jar file                          |
| `mvn install`              | Installs the packages into the local repository |
| `mvn test`                 | Run all the tests                               |
| `mvn -pl shared test`      | Run all the shared tests                        |
| `mvn -pl client exec:java` | Build and run the client `Main`                 |
| `mvn -pl server exec:java` | Build and run the server `Main`                 |

These commands are configured by the `pom.xml` (Project Object Model) files. There is a POM file in the root of the project, and one in each of the modules. The root POM defines any global dependencies and references the module POM files.

## Running the program using Java

Once you have compiled your project into an uber jar, you can execute it with the following command.

```sh
java -jar client/target/client-jar-with-dependencies.jar

♕ 240 Chess Client: chess.ChessPiece@7852e922
```

Phase 2:

https://sequencediagram.org/index.html?presentationMode=readOnly&shrinkToFit=true#initialData=IYYwLg9gTgBAwgGwJYFMB2YBQAHYUxIhK4YwDKKUAbpTngUSWDABLBoAmCtu+hx7ZhWqEUdPo0EwAIsDDAAgiBAoAzqswc5wAEbBVKGBx2ZM6MFACeq3ETQBzGAAYAdAE5M9qBACu2AMQALADMABwATG4gMP7I9gAWYDoIPoYASij2SKoWckgQaJiIqKQAtAB85JQ0UABcMADaAAoA8mQAKgC6MAD0PgZQADpoAN4ARP2UaMAAtihjtWMwYwA0y7jqAO7QHAtLq8soM8BICHvLAL6YwjUwFazsXJT145NQ03PnB2MbqttQu0WyzWYyOJzOQLGVzYnG4sHuN1E9SgmWyYEoAAoMlkcpQMgBHVI5ACU12qojulVk8iUKnU9XsKDAAFUBhi3h8UKTqYplGpVJSjDpagAxJCcGCsyg8mA6SwwDmzMQ6FHAADWkoGME2SDA8QVA05MGACFVHHlKAAHmiNDzafy7gjySp6lKoDyySIVI7KjdnjAFKaUMBze11egAKKWlTYAgFT23Ur3YrmeqBJzBYbjObqYCMhbLCNQbx1A1TJXGoMh+XyNXoKFmTiYO189Q+qpelD1NA+BAIBMU+4tumqWogVXot3sgY87nae1t+7GWoKDgcTXS7QD71D+et0fj4PohQ+PUY4Cn+Kz5t7keC5er9cnvUexE7+4wp6l7FovFqXtYJ+cLtn6pavIaSpLPU+wgheertBAdZoFByyXAmlDtimGD1OEThOFmEwQZ8MDQcCyxwfECFISh+xXOgHCmF4vgBNA7CMjEIpwBG0hwAoMAADIQFkhRYcwTrUP6zRtF0vQGOo+RoFmipzGsvz-BwVygYKQH+iMykoKp+h-Ds0KPMB4lUEiMAIEJ4oYoJwkEkSYCkm+hi7jS+4MkyU76XOnl3kuwowGKEpujKcplu8So3gFDpJr6zpdhu7pbm57Y6aWDnihkqgAZgmUgdUumEeWnyQrBl5UfWkKacV8LJsgqYwLh+GjKV0XlTB5FVYhNW0Y2DGeN4fj+F4KDoDEcSJONk0Ob4WCiYKoH1A00gRvxEbtBG3Q9HJqgKcMFHVYUWkfmZ-rHX1hSFRZVk2fYC32UJC1OWoLnbu5VK3vy3ksmyfmxbygWVMuIXiuu4XaLK8r6UDC4CglHaWS6KWvklgrDr9RgoNwx6Xuel7Xlji6g8F0i40yhjPleaUY0jmX1PNp65flt0WbpdUSQ1lSiTheHDDAIx0U2w3Mf4KLrv42Dihq-FojAADiSoaEtHOlg0itbbt9hKkdvVIeh8KVIzxoG+gBUXcbyP3Wiys5vZdsq29JKfZjP30jAjJgPbKCEy+2j+cD8Vk-UitMkrSoRfKV2GyTiN3ajvvo52GVW0zTs5qzCCAVbRXcy8Yy6zmBYNOMxcoAAktINFkWMmzxLqKBupytffMkoBqi3kEkah3wVwAcj3pFQp0XOJo1JRgPzbVC0XKul+XSrV23IIN033fESPIIdyAXdEfMvfdfPcxD1vfcXGPg2MSNAQcAA7G4TgoE4MQRsEcBcQAbPAE6GL7MAihNTKIlAujRWgdB1nraY5tFKjEHkqceGFzqwkurAtYIwEFzFMqg62bl6iHjkCgX2GI4B-19i7D66UPLB09t7EhFFiYe0RqHJWEdk7Q0irHC28d84o2SgAlO-DlpJXqHAeIKA94wCQAAMyMNDbIMAex9jdjQhGBDyFKgxFgrk8N9z3mCo+SOcxo5m3gtdPRgVE4CKjnTVODN06-yPMQpUlDLa4L4c8JecwV4jyQTzeAwDp4tQFmgOeFcV5HzQvRG+4tLC4xspsKaSAEhgHiX2CASSABSEBxTGMMP4XeaogFTxEWA5ozIZI9ArvrcxSEszYAQMAeJUA4AQBslANYETpD+O0o47haAMGNOaZQNpHSunLx6e4r8ZT+H1AAFa5LQCQnJ4oKEoEJO9Vy9NvpxToUyBhRNA6WJDkKMO7DbHyBhmYyiFjeFI3wfkmQdjhHWLERIqRsiYDDJaWM6A0iBTKP7NQ3ZtDRwKmwFodEJCK5ML2Sws5kpIVEKikaGR-yfkYRAO06AqiTaOKBdM8yoCagvH8ZPZqrVBbC2vmLUaXhmnJNSQy+UiBgywGANgRphA8gFEAWrElkl1qbW2rtYwRs+m4PqOK26JKrIcEpuiHkegDAYm2fY0F6icZ4xQNIHQaqTmk0RRTHVzz5AqrEPct5SjezAvpigr83ZbVEuQerMlEqkZ8xCW1GlMSgA