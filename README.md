# DistLedger

Distributed Systems Project 2022/2023

## Authors

**Group A47**

### Team Members

| Number | Name               | User                               | Email                                        |
| ------ | ------------------ | ---------------------------------- | -------------------------------------------- |
| 92424  | André Azevedo      | <https://github.com/andremazevedo> | <mailto:andre.m.azevedo@tecnico.ulisboa.pt>  |
| 99251  | João Cardoso       | <https://github.com/joaoncardoso>  | <mailto:joao.n.m.cardoso@tecnico.ulisboa.pt> |
| 99259  | José João Ferreira | <https://github.com/jjasferreira>  | <mailto:josejoaoferreira@tecnico.ulisboa.pt> |

## Getting Started

The overall system is made up of several modules. The main server is the _DistLedgerServer_. The clients are the _User_
and the _Admin_. The definition of messages and services is in the _Contract_. The future naming server
is the _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/DistLedger) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation and running

1. To compile and install all modules:

```s
mvn clean install
```

2. Then, to generate the code for the client and server stubs:

```s
cd Contract
mvn install
```

3. To compile and run the server:

```s
cd DistLedgerServer
mvn compile exec:java Dexec.args="2001 A [-debug]"
```

The `-debug` flag is optional and prints to the terminal all user interactions

4. Then, you can finally execute multiple instances of admins and/or users by opening new terminals and running:

```s
cd Admin
mvn compile exec:java
```

```s
cd User
mvn compile exec:java
```

## Built With

- [Maven](https://maven.apache.org/) - Build and dependency management tool;
- [gRPC](https://grpc.io/) - RPC framework.
