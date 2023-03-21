## Answers

- R: no servidor primário podemos ter uma variável para o ultimo servidor com quem falei. Se ele responder (se não ver qual o tipo de erro que devolve), não preciso de ir ao naming server fazer lookup a cada propagate
- ativação e desativação de servers liga/desliga do naming server? catch sinal de paragem de execução e remover do namingserver

## Dúvidas para colocar ao nosso prof:
- Mapa de ServiceEntry com keys a serem o nome, mas a própria ServiceEntry tem um nome
- Qualificador (ex: "A" ou "B", como está no enunciado). Nome Role está bem?
- Um address:porto pode aparecer repetido para o mesmo serviço? E o qualificador? NÃO
- Como é que os clientes consultam o naming server? Ao ser executados? E procuram servidor primário ou secundário?
- Dúvidas devem ter mesmo o nome "Not possible to remove the server" como está no lab 5 ou pode ser algo mais específico, ocmo temos?
- O userService contém 2 stubs para cada server ou criamos dois userservice?
- Se primário estiver off/inactive, o secundário responde na mesma a pedidos de leitura? E se sim, ao detetar que o primário está off, vai ao secundário?
- Como é que colocamos o método lookup apenas num sitio em vez de estar nas 3 implementações de serviço? Ou isto não vai ser valorizado? (lookup do User, Admin e DistLedgerServer é semelhante)
- Como é que vemos se o server em cache do lado do user ainda está ativo? Ping?
- O localhost:5001 (port do namingserver) deve ser guardado como variável de cada processo ou deve-se ir buscar sempre ao pom.xml?
- Podemos assumir que o endereço do servidor é localhost ou devemos ir buscá-lo de outra forma?
- Clonar o ledgerstate antes de propagar ou fazemos rollback em caso de erro? remover ultimo da lista?
- Como garantir que métodos pda ServerMain funcionam para ambos os roles, mas que pedidos de escrita ao secundário por parte do cliente retornem sempre erro? Verificamos se o servidor é primário no UserServiceImpl, está correto?

## TODO:
- Broker só pode ser criado uma vez (no server primario)
- Primário propaga operação ao secundário antes de responder ao cliente
- Se server primário off, pedido de escrita ao primário devolve ERRO
- Pedidos de escrita ao secundário devolvem sempre ERRO
- Se secundário off, pedido de escrita ao primário devolve ERRO (não consegue propagar estado)
- A partir da fase 2, programas clientes não têm argumentos (preciso remover arg parsing)
- Devemos fazer lookup antes de fazermos propagateState
- mudar o README.md para incorporar as alterações (-Ddebug="debug")