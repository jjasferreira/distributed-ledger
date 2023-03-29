### TODO:

```
DISTLEDGER/REPLICAMANAGER?:
    private int replicaId;
    private List<Integer> replicaTS;
    private List<Integer> valueTS; // need to instantiate lists on constructor
    private List<Operation> log;
    // nedd to, upon reveiving prevTS = {}, to notice it is the client's first request
    
    READS:
        if (IsReplicaUpToDateWith(prevTS)) {
            // send response + valueTS
        }
        else {
            // wait for gossip and then
            // send response + valueTS
        }

    private boolean IsReplicaUpToDateWith(List<Integer> prevTS) {
        sizeTS = prevTS.size();
        for (int i = 0; i < sizeTS; i++) {
            if (prevTS[i] > this.valueTS[i])
                return false;
        }
        return true;
    }

    UPDATES:
        // check if it is repeated, in which case it is ignored (look for the prevTS id?)
        // increment index of corresponding replicaId in replicaTS (+1)
        // create new modifiedTS (prevTS but with new value above, so that it is unique)
        // append operation to log and send the new TS to client (the whole vector or simply the update ID?)
        
        // do this check periodically?:
        if (IsReplicaUpToDateWith(prevTS)) {
            // do the update locally, add to ledger
            // update valueTS (for each i, if replicaTS[i] > valueTS[i] , update it
            // when a new update is made, we check
            // if we can do pending ones? (create function)
        }

    GOSSIP:
        sizeTS = receivedTS.size();
        for (int i = 0; i < sizeTS; i++) {
            n = receivedTS[i] - replicaTS[i];
            if (n <= 0)
                continue;
            else {
                // ask for last n operations from replica i's log
            }
        }
```

---	

### QUESTIONS:

- Q: Replica Manager é uma nova classe?
- A: Não, o stub em si é o frontend para as replicas
- Q: Como definir que réplica assume certo índice nos timestamps? Ir ao NamingServer e calcular número de servers existentes?
- A: 
- Q: É suposto termos vários métodos de operações de escrita em execução à espera que o valueTS seja atualizado para acabarem de correr ou saímos da função e fazemos essa verificação a cada update? O blockingStub tem alguma coisa a ver com isto?
- A: monitors threads que esperam
- Q: Ao fazer gossip, temos que enviar, para além do vector clock, também o log sempre? Não podemos esperar por uma resposta da outra replica a dizer quantas operações quer e apenas mandar essas? E como sabemos a ordem pela qual foram efetuadas, ou isso não é importante?
- A: temos uma lista de gossips recebidos para cada server e só mandamos o que estimamos que ele ainda não tenha
- Q: Como é que verificamos que um update é repetido? Pelo timestamp que ele tem guardado em si?
- A: para saber se podemos fazer um update, compara com o value timestamp

#### add new questions here:
- Q: Com que frequência é que se vê o log de updates pendentes? Ao receber um pedido novo?
- A: Primeiro implementação ingénua (espera ativa), depois com tempo implementar monitors.
- Q: Qual a diferença entre o replica timestamp e o value timestamp
- A: Replica timestamp representa updates recebidas por front-ends diretamente para o índice da réplica, e updates propagadas por outras réplicas para os outros índices. Value timestamp representa as operações feitas, no mesmo esquema. 
- Q: Com que frequência é que se faz gossip? De x em x tempo? Podemos ter um bit a dizer se entretanto houve alguma alteração?
- A: Faz-se na linha de comandos, pelo admin
- Q: Ao receber um pedido cujo prevTS seja inferior, podemos invocar a função do state diretamente ou devemos adicionar à queue e o state verifica de forma independente?
- A:
- Q: É preciso ir apagando operações do log à medida que se for sabendo que todas as outras réplicas já os fizeram? (790 coulouris, ponto 3)

- A: replicaTS sempre é atualizada quando se recebem gossips