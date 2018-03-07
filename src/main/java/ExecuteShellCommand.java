import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ExecuteShellCommand {

public String executeCommand(String command) {

    StringBuffer output = new StringBuffer();

    Process p;
    try {
        p = Runtime.getRuntime().exec(command);
        p.waitFor();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(p.getInputStream()));

        BufferedReader errorReader =
                new BufferedReader(new InputStreamReader(p.getErrorStream()));


        String line = "";
        while ((line = reader.readLine())!= null) {
            output.append(line + "\n");
        }

        while ((line = errorReader.readLine())!= null) {
            output.append(line + "\n");
        }

    } catch (Exception e) {
        e.printStackTrace();
    }

    return output.toString();

}

}