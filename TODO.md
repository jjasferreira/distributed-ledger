### TODO:

- Testar Naming server rejeita quarto servidor a registar

### QUESTIONS:

- Q: É suposto termos vários métodos de operações de escrita em execução à espera que o valueTS seja atualizado para acabarem de correr ou saímos da função e fazemos essa verificação a cada update? O blockingStub tem alguma coisa a ver com isto?
- A: Primeiro implementação ingénua (espera ativa), depois com tempo implementar monitors. (stor). Diz no livro que é quando recebemos uma propagação de estado que vamos verificar se as updates no log podem ser feitas.

- Q: Como é que é com os pedidos de leitura? Sempre devolvemos apenas um erro quando op.prevTS > valueTS? Ou fazemos outro mecanismo de espera. wait/notify?
- A:

### ADD TO REPORT:

- NamingServer devolve o índice do servidor que se registou a ele mesmo e esse índice corresponde à entrada do VectorClock que corresponde ao servidor que se registou.
- Implementámos uma solução para 3 servidores (<_,_,\_>) e isso é uma MACRO que pode ser alterada em vários ficheiros
- O NamingServer não aceita registar mais do que 3 servidores
- Criamos uma classe Ledger que contém uma lista de ops instáveis e outra estáveis, em vez de ter um atributo (facilita a verificação das ops instávies). A inserção de ops na ledger respeita a relação happensBefore dos vectorClocks
- Criamos novas exceções e apagamos que outras que já não faziam sentido
- O Cliente não usa VectorClock (classe que definimos no lado do servidor) porque não precisa de realizar operações complexas sobre estes (é apenas uma lista de inteiros na perspetiva do cliente)
- O gossip do Admin recebe apenas um argumento e manda pedido a esse servidor para propagar para todos os outros que existam no momento
- Tivemos que criar a classe BalanceInfo para conter os dois argumentos de resposta que esta operaçao devolve (o valor e o timestamp(que corresponde ao vectorClock))
- A operação delete foi descontinuada porque não conseguimos garantir, na arquitetura Gossip, que respeite a causalidade. Iria requerir causalidade imediata.
- O tratamento de exceções relativas a argumentos que estariam sempre errados, aquando da criação de conta ou transferência de fundos, é realizada logo quando se recebe o pedido de operação. No entanto, a verificação de erros mais contextuais (como o caso em que transferimos de uma conta que ainda não existe, porque a sua criação foi feita noutra réplica), decorre apenas no momento em que a dada réplica recebe essa operação vinda de um gossip. Nos casos em que o op.prevTS > valueTS, estas operações são adicionadas à lista de ops instáveis ad ledger e não decorre a verificação de exceções precisamente por receio de que de facto os argumentos estejam afinal todos corretos, mas o servidor atual ainda desconheça as operações anteriores que garantem isso mesmo. Mesmo nos casos em que a operação não foi realizada porque lançou uma exceção, permanece na ledger (eventualmente de todas as réplicas) como uma operação instável.
