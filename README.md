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

#### 1. To compile and install all modules:

```s
mvn clean install
```

#### 2. Then, to generate the code for all stubs:

```s
cd Contract
mvn install
```

#### 3. To compile and run the Naming Server:

```s
cd NamingServer
mvn compile exec:java [-Ddebug=true]
```

###### The `-Ddebug` flag is optional and prints all interactions related to the Naming Server to the terminal.

#### 4. To compile and run a Server with the primary role (`A`) and a Server with the secondary role (`B`):

```s
cd DistLedgerServer
mvn compile exec:java [-Ddebug=true]
```

```s
cd DistLedgerServer
mvn compile exec:java -Dexec.args="2002 B" [-Ddebug=true]
```

###### The Primary Server, in this delivery, is, by default, using the address `localhost:2001`. Again, the `-Ddebug` flag is optional and prints all Server interactions to the terminal. You can change the port to any other available number, but some value must be provided.

#### 5. Then, you can finally execute multiple instances of admins and/or users by opening new terminals and running:

```s
cd Admin
mvn compile exec:java
```

```s
cd User
mvn compile exec:java
```

### Implementation details

- We had implemented the `debug` functionality only on the Server for the 1st delivery, as we felt it would be pointless to have it on the User and Admin sides. On the 2nd delivery, we have added this functionality to the User, Admin and Naming Server, as we felt it would be useful to have it on all processes.
- Since there is the risk of an admin setting the server state to inactive in the middle of the execution of another client's operation, we figured it was important to guarantee the atomicity of every operation. However, the use of `synchronized` blocks would make it so that any two operations cannot be executed simultaneously, as they all require confirmation that the server is active and that it will remain so until the end of the operation. In other words, the `active` attribute is accessed for "reads" much more often than for "writes". Therefore, we decided to use a read/write lock, which allows for multiple operations to be executed at the same time when the server is active, yet prevents admins from being able to change server state in the middle of an operation, guaranteeing atomicity. For operations which access the `accounts` hash map, we use `synchronized` blocks with `accounts` as the synchronization object, in order to ensure that no two processes read or write to the hash map at any given time. Any access to the `ledger` attribute is also nested inside of one of these blocks, which also guarantees that it is thread-safe.
- In the `ServerState.java` class, we throw exceptions for a number of common mistakes, and also a few less obvious ones, which we felt allowed us to provide a better user experience. Some of these error cases include the creation or deletion of a broker account, the attempt at a transfer where the sender and receiver are the same, among others. The exceptions are propagated to the "ServiceImpl" classes, and then identified and redirected to the command parsers, following the implementation logic outlined in the laboratory guides. The same logic applies to the `NamingServerState.java` class, which we implemented only on the 2nd delivery.
- We have changed exception handling for the 2nd delivery; we now have a separate class for each exception, which allows us to have a more detailed error message for each exception and, therefore, provide a better understanding of what went wrong.
- When propagating the ledger state from a primary server to a secondary one, we decided to propagate a list containing only the last operation, as this is more efficient than propagating the entire ledger. We can do this, given that, in the 2nd delivery, for evaluation purposes, servers will never be re-executed after they have been turned off.
- We also implemented a rollback mechanism in case the propagation fails, and we perform the operation before the propagation to ensure that, in case the primary server goes down in the middle of the propagation, its state is changed and the operation is not lost.
- The `ServerState.java` class is perhaps the most crucial part of the whole system, as it is used everytime a server is executed and registered to the Naming Server. To ensure that the logic of the primary and secondary servers is somewhat separated, we have defined the method receivePropagatedState, which executes the newly received operation without interfering with the methods that execute that same operation when running on the primary server. This and the fact that the primary server cannot do any "write" operation before the creation of a secondary server allows us to simply add the broker account with its initial balance to the system on both servers.
- We have implemented a sort of "cache" for each User/Admin process, which stores the last primary and secondary servers with which it communicated. This allows us to avoid unnecessary lookups, which could be costly. If the stored servers do not respond in 2 seconds, we perform a new lookup on the Naming Server.
- This same idea came up when developing the primary-backup distributed system. This sort of "cache" that is stored in the code itself, was also used to store the last secondary server to which the primary server communicated. In case the secondary server does not respond when trying to propagate the new operation, we perform a new lookup.
- When we execute each server, we automatically try to register it to the Naming Server, so that it is accessible to clients. When it comes to the primary server, we also immediately look for a secondary server, so that possibly we never have to perform a lookup again.

## Built With

- [Maven](https://maven.apache.org/) - Build and dependency management tool;
- [gRPC](https://grpc.io/) - RPC framework.
