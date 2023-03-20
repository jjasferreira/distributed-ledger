# Questions

- R: não há problema em estarem no mesmo ficheiro, proto e seviço do namingserver tudo junto sempre
- R: no servidor primário podemos ter uma variável para o ultimo servidor com quem falei. Se ele responder (se não ver qual o tipo de erro que devolve), não preciso de ir ao naming server fazer lookup a cada propagate
- R: converter ServerEntry para string para enviar no repeated, não é só o endereço-
- ativação e desativação de servers liga/desliga do naming server? catch sinal de paragem de execução e remover do namingserver

## Nosso prof:
- Mapa de ServiceEntry com keys a serem o nome, mas a própria ServiceEntry tem um nome
- Qualificador (ex: "A" ou "B", como está no enunciado). Nome Role está bem?

- Um address:porto pode aparecer repetido para o mesmo serviço? E o qualificador? NÃO
