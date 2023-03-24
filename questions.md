## Answers

- R: no servidor primário podemos ter uma variável para o ultimo servidor com quem falei. Se ele responder (se não ver qual o tipo de erro que devolve), não preciso de ir ao naming server fazer lookup a cada propagate
- ativação e desativação de servers liga/desliga do naming server? catch sinal de paragem de execução e remover do namingserver


## TODO:
- Se primário falhar a propagação da operação ao secundário (por estar off) antes de responder ao cliente, devolve erro para o seu terminal e para o do cliente
- Se server primário off, pedido de escrita ao primário devolve ERRO
- Pedidos de escrita ao secundário devolvem sempre ERRO
-
- Por OK no debug do server secundário
- mudar o README.md para incorporar algumas decisões tomadas (caches nos processos, debug, dupla criação do broker, etc.)