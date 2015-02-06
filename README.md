Introduction
============

The LoginTC Java client is a complete LoginTC [REST API][rest-api] client to
manage LoginTC organizations, users, domains, tokens and to create login
sessions.

Installation
============

Compile a JAR using [Maven](http://maven.apache.org/) and add it to your build
path:

    git clone https://github.com/logintc/logintc-java.git
    cd logintc-java
    mvn install

You can also compile a stand-alone JAR with all dependencies using
`mvn assembly:single`. The compiled JAR files will be located in the `target`
directory.
  
You can also download prebuilt JARs:

  * [logintc-java-1.1.0.jar](https://www.logintc.com/downloads/logintc-java-1.1.0.jar)
  * [logintc-java-1.1.0-jar-with-dependencies.jar](https://www.logintc.com/downloads/logintc-java-1.1.0-jar-with-dependencies.jar)

Example
=======

The following example will create an authentication session and wait 60 seconds
for the user to approve or deny:

```java
String apiKey = "zoqWOJIeNNsQUPnPtiRjbpb2tm9jV9M1vHCMGImt22SV4lMLvuuIkl4giwRKZcZN";
String domainId = "5340154b751da210542facd75ef8f2a6ba6dc305";

LoginTC client = new LoginTC(apiKey);

Session session = client.createSessionWithUsername(domainId, "john.doe", null);

long time = System.currentTimeMillis();
long timeout = 60 * 1000;

login: while (System.currentTimeMillis() - time < timeout) {
    session = client.getSession(domainId, session.getId());

    switch (session.getState()) {
        case APPROVED:
            System.out.println("Approved!");
            break login;
        case DENIED:
            System.out.println("Denied!");
            break login;
        case PENDING:
        default:
            System.out.println("Waiting...");
            break;
    }

    Thread.sleep(1000L);
}
```

Documentation
=============

See <https://www.logintc.com/docs>

Help
====

Email: <support@cyphercor.com>

<https://www.logintc.com>

[rest-api]: https://www.logintc.com/docs/rest-api
