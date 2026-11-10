/**
 * CSC201 Task 2 -- master class.
 *
 * Runs every command in a command file:
 *     java -cp bin Subway                 (reads tests/commands.txt)
 *     java -cp bin Subway other.txt       (reads other.txt)
 */
public class Subway {
    public static void main(String[] args) {
        CommandRunner.run(CommandRunner.fileFrom(args), null);
    }
}
