package gitlet;

import java.io.File;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author David Oh
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        try {
            checkArgsFormat(args);
            checkInitialized(args[0]);
            switch (args[0]) {
            case "init":
                WorkingDirectory.init();
                break;
            case "add":
                StagingArea stage = StagingArea.readStage();
                stage.add(args[1]);
                break;
            case "commit":
                new Commit(args[1]);
                break;
            case "rm":
                WorkingDirectory.rm(args[1]);
                break;
            case "log":
                Commit.log(Commit.readCommit(Branch.readBranch(WorkingDirectory
                        .readHead().getBranchPointer().getName())));
                break;
            case "global-log":
                Commit.globalLog();
                break;
            case "find":
                Commit.find(args[1]);
                break;
            case "status":
                WorkingDirectory.status();
                break;
            case "checkout":
                if (args.length == 3) {
                    WorkingDirectory.checkout(args);
                } else if (args.length == 4) {
                    WorkingDirectory.checkout(args[1], args[3]);
                } else if (args.length == 2) {
                    WorkingDirectory.checkout(args[1]);
                }
                break;
            case "branch":
                WorkingDirectory.createBranch(args[1]);
                break;
            case "rm-branch":
                WorkingDirectory.deleteBranch(args[1]);
                break;
            case "reset":
                WorkingDirectory.reset(args[1]);
                break;
            case "merge":
                WorkingDirectory.merge(args[1]);
                break;
            default:
                throw new GitletException("No command with that name exists.");
            }
        } catch (GitletException g) {
            System.out.println(g.getMessage());
            System.exit(0);
        }
    }

    /** Check to see that the user input follows the correct number
     *  of arguments and format.
     *  @param args : array of Strings the user inputs. */
    static void checkArgsFormat(String[] args) {
        if (args.length < 1) {
            throw new GitletException("Please enter a command.");
        }
        String command = args[0];
        if (command.equals("init") || command.equals("log")
                || command.equals("global-log") || command.equals("status")) {
            if (args.length > 1) {
                throw new GitletException("Incorrect operands.");
            }
        } else if (command.equals("add") || command.equals("commit")
                || command.equals("branch") || command.equals("rm-branch")
                || command.equals("rm") || command.equals("reset")
                || command.equals("find") || command.equals("merge")) {
            if (args.length != 2) {
                throw new GitletException("Incorrect operands.");
            }
            if (args[0].equals("commit")) {
                if (args[1].equals("")) {
                    throw new GitletException("Please enter a commit message.");
                }
            }
        } else if (command.equals("checkout")) {
            if (args.length < 2 || args.length > 4) {
                throw new GitletException("Incorrect operands.");
            } else {
                if (args.length == 3) {
                    if (!args[1].equals("--")) {
                        throw new GitletException("Incorrect operands.");
                    }
                } else if (args.length == 4) {
                    if (!args[2].equals("--")) {
                        throw new GitletException("Incorrect operands.");
                    }
                }
            }
        }
    }

    /** Check to see if .gitlet directory is initialized before running
     *  this command.
     *  @param command : args[0], the first string the user inputs. */
    static void checkInitialized(String command) {
        if (!command.equals("init")) {
            File gitletDir = Utils.join(WorkingDirectory.CWD, ".gitlet");
            if (!gitletDir.exists()) {
                throw new GitletException("Not in an initialized Gitlet "
                        + "directory.");
            }
        }
    }
}
