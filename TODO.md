### TODO:

```
DISTLEDGER/REPLICAMANAGER?:

    REGISTER FUNCTION:
    when registering to naming server
    for each replica found
        counter++
        cria stub, guarda numa lista indexada pelo replicaID respetivo
    
    this.replicaID = counter
    
    UPDATE FUNCTION:
    lookup
    se não tiver crossServerService para algum, adiciona-o


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

### IDEIAS DE IMPLEMENTAÇÃO:

- Cada vez que um servidor se regista no naming server, mada uma mensagem "greet" a todos os outros, para que possam atualizar os timestamps.
  - Alternativamente, o naming server manda uma notificação a todos os outros servidores quando um novo servidor se regista.
  - O naming server manda também no RegisterReply o número de registo (índice no timestamp) do servidor que se registou.

---	

### TODO:
- Naming server rejeita quarto servidor a registar

### QUESTIONS:

- Q: Replica Manager é uma nova classe?
- A: Não, o stub em si é o frontend para as replicas

- Q: É suposto termos vários métodos de operações de escrita em execução à espera que o valueTS seja atualizado para acabarem de correr ou saímos da função e fazemos essa verificação a cada update? O blockingStub tem alguma coisa a ver com isto?
- A: Primeiro implementação ingénua (espera ativa), depois com tempo implementar monitors. (stor). Diz no livro que é quando recebemos uma propagação de estado que vamos verificar se as updates no log podem ser feitas. 

- Q: Ao fazer gossip, temos que enviar, para além do vector clock, também o log sempre? Não podemos esperar por uma resposta da outra replica a dizer quantas operações quer e apenas mandar essas? E como sabemos a ordem pela qual foram efetuadas, ou isso não é importante?
- A: temos uma lista de gossips recebidos para cada server e só mandamos o que estimamos que ele ainda não tenha.

- Q: Como é que verificamos que um update é repetido? Pelo timestamp que ele tem guardado em si?
- A: para saber se podemos fazer um update, compara com o value timestamp
- A (melhor): O front-end manda um unique identifier, que é registado na executed-operation table. Se o front-end receber um pedido com um unique identifier que já está na executed-operation table, ele ignora o pedido. 

- Q: Qual a diferença entre o replica timestamp e o value timestamp
- A: Replica timestamp representa updates recebidas por front-ends diretamente para o índice da réplica, e updates propagadas por outras réplicas para os outros índices. Value timestamp representa as operações feitas, no mesmo esquema. Value timestamp é merged quando uma update é aplicada.

- Q: Com que frequência é que se faz gossip? De x em x tempo?
- A: Faz-se na linha de comandos, pelo admin

- Q: É preciso ir apagando operações do log à medida que se for sabendo que todas as outras réplicas já os fizeram? (790 coulouris, ponto 3)
- A: replicaTS sempre é atualizada quando se recebem gossips (o stor não respondeu bem a isto lol). Vamos assumir que sim.

- Q: Ao receber um pedido cujo prevTS seja inferior, podemos invocar a função do state diretamente ou devemos adicionar à ledger e o state verifica de forma independente?
- A: Tanto faz. Pode ter uma thread diferente para ver a ledger e fazer as operações.

- Q: Admin não precisa de timestamps, certo?
- A: Não.

- Q: O que é que acontece quando fazemos gossip de um server para um outro que esteja inativo?
- A: Dá algum erro (igual à 2a entrega)

- Q: Nós removemos completamente tudo o que estava relacionado com a operação delete. Fizemos bem?
- A: Sim, é indiferente

- Q: Como definir que réplica assume certo índice nos timestamps? Ir ao NamingServer e calcular número de servers existentes?
- A: NamingServer responde com o índice do server que se registou

- Q: Gossip funciona quando servidor está desativado?
- A: 

Falta:
- createAccount e transferTo
	- atualiza ReplicaTs
	- ao guardar na ledger, guarda ```(índice_replica, ts_update, operation_type, prevTS)```, em que prevTS é o timestamp que o cliente manda, e ts_update é igual mas com o valor na posição índice_replica incrementado (#TODO mudar classe operation)
	- se prevTS <= valueTS então executa e põe na ledger de operações estáveis
		- atualiza valueTS
	- cc. coloca na das operações instáveis

- propagateState - A manda a sua ledger a B
	- lado do A
		- A encontra o replica timestamp do B na sua timestamp table
		- percorre as suas ledgers, primeiro a estável e depois a instável
		- põe numa ledger temporária as operações por ordem, se 
	- lado do B
		- B recebe ledger e replica timestamp da A
		- B atualiza timestamp table com o timestamp da A que acabou de receber
		- faz merge do replica timestamp do B com o do A
		- percorre a ledger da A e adiciona tudo ao log de operações instáveis (por ordem, usar o comparator que o vector clock implementa, não adiciona uma operação se a lista de operações já tiver o update_ts)
		- percorre de novo a ledger e compara prevTS com o seu valueTS. Se prevTS <= valueTS, pode executar, e faz merge de valueTS com o updateTS.
		- manda confirmação

# TODO
- Adicionar ao state ledgers de operações estáveis e instáveis
- Testar num ficheiro à parte se o comparator do vector state está a funcionar e listas ordenadas com base nisso. - João
- implementar createAccount 
	 - [X] corrigir UserServiceImplementation
	 - [X] lógica no serverState
- implementar transferTo
	 - [X] corrigir UserServiceImplementation
	 - [X] lógica no serverState
- implementar o propagateState
	- [ ] crossServerServiceImplementation
	- [ ] crossServerService
	- [ ] função gossip
- Testar tudo :)

- Double check de verificação de exceções no state. Devem vir antes ou depois de lógica de timestamps/ledgers e afins?

- E se uma réplica for apagada/desativada? O que acontece às operações que já existiam na ledger?

- Porque é que precisamos de separar em duas ledger as unstable operations e stable operations? Parece não nos oferecer nada de jeito
