package pathfinder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TransportJsConverter {
    public static void main(String[] args) throws IOException {
        for (String transport : Files.readAllLines(Paths.get("src/main/resources/transports.txt"))) {
            if (transport.startsWith("#") || transport.isEmpty()) {
                continue;
            }

            transport = transport.split(" \"")[0];
            transport = transport.substring(0, transport.lastIndexOf(" "));
            String[] parts = transport.split(" ");

            String p1 = "new Position(" + parts[0] + ", " + parts[1] + ", " + parts[2] + ")";
            String p2 = "new Position(" + parts[3] + ", " + parts[4] + ", " + parts[5] + ")";
            StringBuilder rest = new StringBuilder();

            for (int i = 6; i < parts.length; i++) {
                rest.append(parts[i]);
                if (i < parts.length - 1) rest.append(" ");
            }

            System.out.println("this.addTransport(" + p1 + ", " + p2 + ", \"" + rest.toString() + "\");");
        }
    }
}
