## Answers

- R: no servidor primário podemos ter uma variável para o ultimo servidor com quem falei. Se ele responder (se não ver qual o tipo de erro que devolve), não preciso de ir ao naming server fazer lookup a cada propagate
- ativação e desativação de servers liga/desliga do naming server? catch sinal de paragem de execução e remover do namingserver

## Dúvidas para colocar ao nosso prof:
SE ALGUEM FOR ABAIXO, NÃO VAI ACONTECER ESSE VOLTAR A SER EXECUTADO

- R: X tempo retries para propagar para o secundário, se ele ainda não estiver executado (usar timeout e extender para os outros métodos)
- Podemos assumir que o endereço do servidor é localhost ou devemos ir buscá-lo de outra forma?
- R: arg da linha de comandos?? prof ainda vai falar com docentes

- 
## TODO:
- Primário propaga operação ao secundário antes de responder ao cliente
- Se server primário off, pedido de escrita ao primário devolve ERRO
- Pedidos de escrita ao secundário devolvem sempre ERRO
- Se secundário off, pedido de escrita ao primário devolve ERRO (não consegue propagar estado)
- A partir da fase 2, programas clientes não têm argumentos (preciso remover arg parsing)
- Devemos fazer lookup antes de fazermos propagateState
- mudar o README.md para incorporar as alterações (-Ddebug="debug")