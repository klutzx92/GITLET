package gitlet;

import java.io.File;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.time.format.DateTimeFormatter;

/** A commit is a snapshot of files. Keeps track of the version of each file
 *  when the commit was created.
 *  @author David Oh */
public class Commit implements Serializable {

    /** Initial Commit constructor. */
    Commit() {
        snapshot = null;
        message = null;
        timeStamp = null;
        previousCommit = null;
        initialize();
    }

    /** Create new commit.
     *  @param msg : The commit message inputted by the user. */
    Commit(String msg) {
        message = msg;
        timeStamp = computeTime();
        setUp();
    }

    /** Method copies snapshot from most recent commit if it exists and updates
     *  it with the contents of the staging area. */
    protected void setUp() {
        StagingArea stage = StagingArea.readStage();
        if (stage.getFilesInStage().isEmpty()
                && stage.getRemovedFiles().isEmpty()) {
            throw new GitletException("No changes added to the commit.");
        }
        Branch head = WorkingDirectory.readHead();
        Branch branch = head.getBranchPointer();
        Commit mostRecentCommit = readCommit(branch);
        this.setPreviousCommit(mostRecentCommit);
        if (mostRecentCommit.snapshot == null) {
            HashMap<String, String> copy =
                    new HashMap<String, String>(stage.getFilesInStage());
            this.snapshot = copy;
        } else {
            HashMap<String, String> copy =
                    new HashMap<String, String>(mostRecentCommit.snapshot);
            this.snapshot = copy;
        }
        Set<String> keys = stage.getFilesInStage().keySet();
        Iterator<String> iter = keys.iterator();
        while (iter.hasNext()) {
            String key = iter.next();
            if (!snapshot.containsKey(key)) {
                snapshot.put(key, stage.getFilesInStage().get(key));
            } else {
                if (!snapshot.get(key).equals(
                        stage.getFilesInStage().get(key))) {
                    snapshot.replace(key, snapshot.get(key),
                            stage.getFilesInStage().get(key));
                }
            }
        }
        for (String removedFile : stage.getRemovedFiles()) {
            if (snapshot.containsKey(removedFile)) {
                snapshot.remove(removedFile);
            }
        }
        saveCommit();
        branch.setCommitID(this.sha1);
        head.setBranchPointer(branch);
        WorkingDirectory.saveHead(head);
        WorkingDirectory.saveBranch(branch);
        stage.getFilesInStage().clear();
        stage.getRemovedFiles().clear();
        stage.saveStage();
    }

    /** Starting at the head commit, display information about each
     *  commit backwards along the commit tree until the initial commit.
     *  @param commit : The head commit. */
    static void log(Commit commit) {
        if (commit != null) {

            System.out.println("===");
            System.out.println("commit " + commit.getSha1());
            if (commit instanceof MergeCommit) {
                String mergeParents = "Merge: "
                        + commit.getPreviousCommit().substring(0, 7) + " "
                        + ((MergeCommit) commit).getPreviousCommit2().
                        substring(0, 7);
                System.out.println(mergeParents);
            }
            System.out.println("Date: " + commit.getTimeStamp());
            System.out.println(commit.getMessage());

            if (commit.getPreviousCommit() != null) {
                System.out.println();
                log(readCommit(commit.getPreviousCommit()));
            }
        }
    }

    /** Method will display the history of all commits ever created. */
    static void globalLog() {
        File commitTreeFile = Utils.join(WorkingDirectory.REPO,
                "commitTree");
        CommitTree commitTree = Utils.readObject(commitTreeFile,
                CommitTree.class);
        Iterator iter = commitTree.getAllCommits().iterator();
        while (iter.hasNext()) {
            String commitSha = (String) iter.next();
            Commit commit = readCommit(commitSha);
            if (commit instanceof MergeCommit) {
                String mergeParents = "Merge: "
                        + commit.getPreviousCommit().substring(0, 7) + " "
                        + ((MergeCommit) commit).getPreviousCommit2().
                        substring(0, 7);
                System.out.println(mergeParents);
            }
            System.out.println("===");
            System.out.println("commit " + commit.getSha1());
            System.out.println("Date: " + commit.getTimeStamp());
            System.out.print(commit.getMessage());
            if (iter.hasNext()) {
                System.out.println();
                System.out.println();
            }
        }
        System.out.println();
    }

    /** Prints out the commit ID's of all commits that have the given
     *  commit message, one per line.
     *  @param commitMessage : The commitMessage inputted by the user. */
    static void find(String commitMessage) {
        File commitTreeFile = Utils.join(WorkingDirectory.REPO,
                "commitTree");
        CommitTree commitTree = Utils.readObject(commitTreeFile,
                CommitTree.class);
        Iterator iter = commitTree.getAllCommits().iterator();
        boolean msgFound = false;
        while (iter.hasNext()) {
            String commitSha = (String) iter.next();
            Commit commit = readCommit(commitSha);
            if (commit.message.equals(commitMessage)) {
                msgFound = true;
                System.out.println(commitSha);
            }
        }
        if (!msgFound) {
            throw new GitletException("Found no commit with that message.");
        }
    }

    /** Get the snapshot of files of this commit.
     *  @return : The snapshot of this commit. */
    HashMap<String, String> getSnapshot() {
        if (snapshot == null) {
            return new HashMap<String, String>();
        } else {
            return snapshot;
        }
    }

    /** Method used to set up the initial commit at startup of the .gitlet
     *  repository. All other commits will have this initial commit as
     *  a common ancestor. */
    private void initialize() {
        setMessage("initial commit");
        setTimeStamp("Wed Dec 31 16:00:00 1969 -0800");
        tree = new CommitTree();
    }

    /** Save this commit to a file. */
    void saveCommit() {
        sha1 = computeSha1();
        updateCommitSet(sha1);
        Utils.writeObject(Utils.join(WorkingDirectory.COMMITS, sha1),
                this);
    }

    /** Update the commit set with this commit.
     *  @param sha1 : The given commit's Sha-1 Hash. */
    static void updateCommitSet(String sha1) {
        File commitTreeFile = Utils.join(WorkingDirectory.REPO,
                "commitTree");
        if (commitTreeFile.exists()) {
            tree = Utils.readObject(commitTreeFile, CommitTree.class);
        }
        tree.getAllCommits().add(sha1);
        Utils.writeObject(Utils.join(WorkingDirectory.REPO,
                "commitTree"), tree);
    }

    /** Read the commit pointed to by given branch from disk
     *  and return it.
     *  @param branch : The given branch.
     *  @return : The commit read from the disk. */
    static Commit readCommit(Branch branch) {
        File commitFile = Utils.join(WorkingDirectory.COMMITS,
                branch.getCommitID());
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Read the commit with the given ID from disk and return it.
     *  @param sha1 : The Sha-1 Hash of the given commit.
     *  @return : The commit read from the disk. */
    static Commit readCommit(String sha1) {
        File commitFile = Utils.join(WorkingDirectory.COMMITS, sha1);
        return Utils.readObject(commitFile, Commit.class);
    }

    /** Get the SHA-1 hash that represents this commit.
     *  @return : This commit's Sha-1 Hash. */
    String getSha1() {
        return sha1;
    }

    /** Compute this commit's SHA-1 hash.
     *  @return : The computed Sha-1 of this commit. */
    String computeSha1() {
        sha1 = Utils.sha1("commit", Utils.serialize(this));
        return sha1;
    }

    /** Compute the time stamp of when this commit was created.
     *  @return : The string representation of the time. */
    String computeTime() {
        DateTimeFormatter dtf =
                DateTimeFormatter.ofPattern("E MMM dd HH:mm:ss yyyy -0800");
        LocalDateTime time = LocalDateTime.now();
        String s = dtf.format(time);
        return s;
    }

    /** Get the commit's message.
     *  @return : This commit's message. */
    String getMessage() {
        return message;
    }

    /** Set the commit message.
     *  @param commitMessage : A string message for this commit. */
    final void setMessage(String commitMessage) {
        this.message = commitMessage;
    }

    /** Get the commit's time stamp.
     *  @return : This commit's time stamp. */
    String getTimeStamp() {
        return timeStamp;
    }

    /** Set the time stamp of this commit.
     *  @param time : The time stamp. */
    final void setTimeStamp(String time) {
        this.timeStamp = time;
    }

    /** Get the commit that directly precedes this commit.
     *  @return : The Sha-1 Hash of this commit's parent. */
    String getPreviousCommit() {
        return this.previousCommit;
    }

    /** Sets the parent of this commit to PREV. All changes are
     *  final because commit trees are immutable.
     *  @param prev : This commit's parent commit. */
    final void setPreviousCommit(Commit prev) {
        previousCommit = prev.getSha1();
    }

    /** Check if this.commit is the same as given commit.
     *  @param commit : The commit being compared to this commit.
     *  @return : A boolean. True if this commit is equal to given commit. */
    boolean equals(Commit commit) {
        return this.getSha1().equals(commit.getSha1());
    }

    /** The hashMap contained in this commit that represents the
     *  snapshot of project taken when commit was created. Keys are
     *  file names and values are SHA-1 hash strings. */
    protected HashMap<String, String> snapshot;

    /** A Commit message. */
    protected String message;

    /** A Commit's Timestamp. */
    protected String timeStamp;

    /** SHA-1 hash code that represents this commit and all of its contents.
     *  The commit's SHA-1 is used for the name of the file where the commit
     *  object is written. */
    protected String sha1;

    /** The SHA-1 hash id of the commit that directly came before this commit
     *  (its parent). */
    protected String previousCommit;

    /** The commit tree that I belong to. */
    protected static CommitTree tree;

}
