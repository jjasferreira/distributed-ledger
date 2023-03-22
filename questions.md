## Answers

- R: no servidor primário podemos ter uma variável para o ultimo servidor com quem falei. Se ele responder (se não ver qual o tipo de erro que devolve), não preciso de ir ao naming server fazer lookup a cada propagate
- ativação e desativação de servers liga/desliga do naming server? catch sinal de paragem de execução e remover do namingserver

## Dúvidas para colocar ao nosso prof:
SE ALGUEM FOR ABAIXO, NÃO VAI ACONTECER ESSE VOLTAR A SER EXECUTADO
TODO: LEDEGR COPY EM VEZ DE MANDAR A REFERENCIA
todo: catch blablaexception, catch blablablaexception, catch exception

- Broker só pode ser criado uma vez (no server primario)?
- R: X tempo retries para propagar para o secundário, se ele ainda não estiver executado (usar timeout e extender para os outros métodos)
- Se primário estiver off/inactive, o secundário responde na mesma a pedidos de leitura? E se sim, ao detetar que o primário está off, vai ao secundário?
- R: Leitura responde sempre. Não vai ao secundário
- Podemos assumir que o endereço do servidor é localhost ou devemos ir buscá-lo de outra forma?
- R: arg da linha de comandos?? prof ainda vai falar com docentes
- O userService contém 2 stubs para cada server ou criamos dois userservice?
- R: user faz lookup sempre antes de qualquer operação ou guardar numa estrutura de dados alguns stubs, se der erro, fazer lookup silencioso
- Como é que vemos se o server em cache do lado do user ainda está ativo? Ping?
- R: não vale a pena fazer ping, mais vale mandar a ledger e se acontecer erro, lookuop
- Clonar o ledgerstate antes de propagar ou fazemos rollback em caso de erro? remover ultimo da lista?
- R: fazer lista com 1 elemento e enviar, fazer rollback em caso de erro
- O localhost:5001 (port do namingserver) deve ser guardado como variável de cada processo ou deve-se ir buscar sempre ao pom.xml?
- R: HARD CODED NO CÓDIGO
- 
## TODO:
- Primário propaga operação ao secundário antes de responder ao cliente
- Se server primário off, pedido de escrita ao primário devolve ERRO
- Pedidos de escrita ao secundário devolvem sempre ERRO
- Se secundário off, pedido de escrita ao primário devolve ERRO (não consegue propagar estado)
- A partir da fase 2, programas clientes não têm argumentos (preciso remover arg parsing)
- Devemos fazer lookup antes de fazermos propagateState
- mudar o README.md para incorporar as alterações (-Ddebug="debug")