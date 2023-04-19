# DistLedger - Report

### Implementation notes regarding the 3rd delivery

We follow the gossip architecture as explained in the course book, with some omissions, namely the executed operation table and the unique client identifier. We also do not delete operations from the ledger. We reject client queries where prevTS > valueTS. Some further notes:
- Upon registration, the naming server replies with an index corresponding to the position in the timestamps of the server that registered.
- Our solution supports 3 servers, although this value can be easily changed (it's defined in a few macros across the files). The naming server rejects the 4th registree.
- We created a class "Ledger" that also has two subledgers, unstable and stable. Insertion into the ledger ensures that between each entry of the ledger and subledgers and its successor the happensBefore or concurrent relationship is verified.
- We defined a VectorClock class to aid in operations. The user does not use this class, as its full functionality is unneeded. As far as the user is concerned, a vector clock is a list of integers.
- The admin gossip command only takes one argument, the propagating server, which propagates its state to all the other replicas.
- In the user folder we created a BalanceInfo class to account for the returning of two arguments in the balance function in the UserService.
- Respecting the project specifications, we removed all logic related to the delete operation.

Note: Implementation notes regarding the 1st and 2nd delivery are available [here](README.md).

### Made by:

- 92424 - [André Azevedo](https://github.com/andremazevedo>)
- 99251 - [João Cardoso](https://github.com/joaoncardoso>)
- 99259 - [José João Ferreira](https://github.com/jjasferreira>)