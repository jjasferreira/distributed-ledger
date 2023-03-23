## Answers

- R: no servidor primário podemos ter uma variável para o ultimo servidor com quem falei. Se ele responder (se não ver qual o tipo de erro que devolve), não preciso de ir ao naming server fazer lookup a cada propagate
- ativação e desativação de servers liga/desliga do naming server? catch sinal de paragem de execução e remover do namingserver


SE ALGUEM FOR ABAIXO, NÃO VAI ACONTECER ESSE VOLTAR A SER EXECUTADO

## TODO:
- Se primário falhar a propagação da operação ao secundário (por estar off) antes de responder ao cliente, devolve erro para o seu terminal e para o do cliente
- Se server primário off, pedido de escrita ao primário devolve ERRO
- Pedidos de escrita ao secundário devolvem sempre ERRO
- Como dar unbind do port ao parar execução dos processos?
- Por OK no debug do server secundário
- Implementar debug do cliente
- Fazer o admin ligar-se ao Naming Server
- mudar o README.md para incorporar algumas decisões tomadas (caches nos processos, debug, dupla criação do broker, etc.)

## Dúvidas para colocar ao nosso prof:
- Como permitir que secundário execute operação de escrita quando advém de propagação, mas não permitir quando vem de cliente?
- Podemos assumir que o endereço do servidor é localhost ou devemos ir buscá-lo de outra forma?
- R: arg da linha de comandos?? prof ainda vai falar com docentes
- (Andre) chamar shutdown() ao channel nos serviços no server, cliente... ou server.shutdown() é suficiente? namingServerService.shutdown.