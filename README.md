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
and the _Admin_. The definition of messages and services is in the _Contract_. The naming server
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

2. Then, to generate the code for all stubs:

```s
cd Contract
mvn install
```

3. To compile and run the Naming Server:

```s
cd NamingServer
mvn compile exec:java [-Ddebug=true]"
```

The `-debug` flag is optional and prints all interactions related to the Naming Server to the terminal.

4. To compile and run the Server with the primary role (A):

```s
cd DistLedgerServer
mvn compile exec:java [-Ddebug=true]"
```

The primary Server, in this delivery, is, by default, using the address `localhost:2001`.
Again, the `-debug` flag is optional and prints all Server interactions to the terminal.

5. To compile and run the Server with the secondary role (B):

```s
cd DistLedgerServer
mvn compile exec:java -Dexec.args="2002 B" [-Ddebug=true]"
```

You can change the port to any other available port, but some value must be provided.

6. Then, you can finally execute multiple instances of admins and/or users by opening new terminals and running:

```s
cd Admin
mvn compile exec:java
```

```s
cd User
mvn compile exec:java
```

### Implementation details

- We have decided to implement the debug functionality only on the Server, as we felt it would be pointless to have it on the User and Admin sides.
- Since there is the risk of an admin setting the server state to inactive in the middle of the execution of another client's operation, we figured it was important to guarantee the atomicity of every operation. However, the use of `synchronized` blocks would make it so that any two operations cannot be executed simultaneously, as they all require confirmation that the server is active and that it will remain so until the end of the operation. In other words, the `active` attribute is accessed for "reads" much more often than for "writes". Therefore, we decided to use a read/write lock, which allows for multiple operations to be executed at the same time when the server is active, yet prevents admins from being able to change server state in the middle of an operation, guaranteeing atomicity. For operations which access the `accounts` hash map, we use `synchronized` blocks with `accounts` as the synchronization object, in order to ensure that no two processes read or write to the hash map at any given time. Any access to the `ledger` attribute is also nested inside of one of these blocks, which also guarantees that it is thread-safe.
- In the `ServerState.java` class, we throw exceptions for a number of common mistakes, and also a few less obvious ones, which we felt allowed us to provide a better user experience. Some of these error cases include the creation or deletion of a broker account, the attempt at a transfer where the sender and receiver are the same, among others. The exceptions are propagated to the "ServiceImpl" classes, and then identified and redirected to the command parsers, following the implementation logic outlined in the laboratory guides.

## Built With

- [Maven](https://maven.apache.org/) - Build and dependency management tool;
- [gRPC](https://grpc.io/) - RPC framework.
