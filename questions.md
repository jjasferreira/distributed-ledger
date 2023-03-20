# Questions

- Operação lookup pode estar no mesmo ficheiro que a oepração register ou como a conexão é
  entre cliente e naming server em vez de ser entre naming server e server, deve estar num ficheiro novo?
- Como os servers podem fazer lookup mas os users não podem registar-se e remover-se, seria
  apropriado definirmos um common file?

- Mapa de ServiceEntry com keys a serem o nome, mas a própria ServiceEntry tem um nome

- No guia do lab 5, onde diz para criarmos NamingServer, não querem dizer NamingServerState?

- É suposto haver debug ou possibilidade de haver inatividade no NamingServer?

- Precisamos de sincronização no NamingServer?

- Qualificador (ex: "A" ou "B", como está no enunciado). Qual o melhor nome em inglês para isto?

- Criamos uma classe nova que interaja com o naming server a partir do servidor ou não? E quando um servidor
  é executado, liga-se automaticamente ao naming server com register e antes de parar, faz delete?

- O servidor primário deve fazer lookup quando há uma operação de escrita no seu estado, se não tiver já associado um servidor secundário, certo?

- Quando dizem que o lookup deve retornar uma lista de servidores, isto quer dizer um mapping address:port?

- Mantemos só um service implementation apenas para o namingserver ou é melhor dividir em 2?-

- Temos que definir um lookupRequest diferente no proto? (Porque role pode não ser dado)