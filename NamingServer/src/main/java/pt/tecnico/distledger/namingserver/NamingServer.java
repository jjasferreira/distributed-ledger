package pt.tecnico.distledger.namingserver;

public class NamingServer {

    public static void main(String[] args) {

        private static final String DEBUG_FLAG = "-debug";
        
        private static final Integer HOST_PORT = 5001;

        //TODO: The naming server listens for connections on port 5001

        public static void main(String[] args) {
            System.out.println(ServerMain.class.getSimpleName());

            boolean debug = false;

            // Receive and print arguments
            System.out.printf("Received %d arguments%n", args.length);
            for (int i = 0; i < args.length; i++) {
                System.out.printf("arg[%d] = %s%n", i, args[i]);
            }

            // Check arguments
            if (args.length > 0) {
                if (args.length == 1 && args[0].equals(DEBUG_FLAG)) {
                    debug = true;
                } else {
                    System.err.println("Too many arguments!");
                    System.err.printf("Usage:java %s", ServerMain.class.getName());
                    return;
                }
            }


        }



    }

}
