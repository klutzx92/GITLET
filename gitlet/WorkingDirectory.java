package gitlet;

import java.io.File;
import java.util.HashMap;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

/** The WorkingDirectory class represents the file structure system.
 *  @author David Oh */
public abstract class WorkingDirectory {

    /** Initializes a new Gitlet version-control system in the current
     *  repository. Creates a directory called .gitlet in which all the
     *  necessary Gitlet files and directories will be stored.*/
    public static void init() {
        if (!REPO.exists()) {
            if (!REFS.exists()) {
                REFS.mkdirs();
            }
            if (!COMMITS.exists()) {
                COMMITS.mkdir();
            }
            StagingArea stage = new StagingArea();
            stage.saveStage();
            Commit initialCommit = new Commit();
            initialCommit.saveCommit();
            Branch master = new Branch("master", initialCommit);
            Branch head = new Branch("HEAD", master);
            saveHead(head);
            saveBranch(master);
            saveHead(head);
        } else {
            throw new GitletException("A Gitlet version-control system already "
                    + "exists in the current directory.");
        }
    }

    /** Save the head pointer to disk in the .gitlet directory.
     *  @param headPointer : The branch that head points to. */
    static void saveHead(Branch headPointer) {
        Utils.writeObject(Utils.join(WorkingDirectory.REPO,
                headPointer.getName()), headPointer);
    }

    /** Read the head pointer from disk located in the .gitlet directory.
     *  @return : The branch that the head pointer points at. */
    static Branch readHead() {
        File headFile = Utils.join(WorkingDirectory.REPO, "HEAD");
        Branch head = Utils.readObject(headFile, Branch.class);
        return head;
    }

    /** Save the branch to disk in the refs directory.
     *  @param branch : The branch to save. */
    static void saveBranch(Branch branch) {
        Utils.writeObject(Utils.join(WorkingDirectory.REFS, branch.getName()),
                branch);
    }

    /** Creates a new branch with the given name and points it at the
     *  current head node. Used for the branch [branch name] command.
     *  @param branchName : The name of branch user inputs. */
    static void createBranch(String branchName) {
        Branch head = WorkingDirectory.readHead();
        Branch headBranch = head.getBranchPointer();
        File branchFile = Utils.join(WorkingDirectory.REFS, branchName);
        if (branchFile.exists()) {
            throw new GitletException("A branch with that name already"
                    + " exists.");
        }
        Commit currentCommit = Commit.readCommit(headBranch.getCommitID());
        Branch newBranch = new Branch(branchName, currentCommit);
        saveHead(head);
        saveBranch(newBranch);
    }

    /** Delete the branch with the given name.
     *  @param branchName : The name of the branch. */
    static void deleteBranch(String branchName) {
        Branch head = WorkingDirectory.readHead();
        Branch branchHead = Branch.readBranch(
                head.getBranchPointer().getName());
        File branchFile = Utils.join(WorkingDirectory.REFS, branchName);
        if (!branchFile.exists()) {
            throw new GitletException("A branch with that name does not"
                    + " exist.");
        } else {
            if (branchHead.getName().equals(branchName)) {
                throw new GitletException("Cannot remove the current "
                        + "branch.");
            } else {
                saveHead(head);
                branchFile.delete();
            }
        }
    }

    /** Method removes the given file from the staging area. If the file is
     *  tracked in the current commit (snapshot), mark it to indicate that
     *  it is not to be included in the next commit and remove it from the
     *  Working Directory.
     *  @param fileName : The name of the file user wishes to remove. */
    static void rm(String fileName) {
        StagingArea stage = StagingArea.readStage();
        Branch head = WorkingDirectory.readHead();
        Branch branch = Branch.readBranch(head.getBranchPointer().getName());
        Commit currentCommit = Commit.readCommit(branch.getCommitID());
        HashMap<String, String> currentSnap = currentCommit.getSnapshot();
        if (currentSnap == null) {
            throw new GitletException("No reason to remove the file.");
        }
        if (!stage.getFilesInStage().containsKey(fileName)
                && !currentSnap.containsKey(fileName)) {
            throw new GitletException("No reason to remove the file.");
        } else {
            if (stage.getFilesInStage().containsKey(fileName)) {
                stage.getFilesInStage().remove(fileName);
            }
            if (currentSnap != null && currentSnap.containsKey(fileName)) {
                Utils.restrictedDelete(fileName);
                stage.getRemovedFiles().add(fileName);
            }
        }
        stage.saveStage();
    }

    /** Display untracked files in status.
     *  @param stagedBlobs : A set of all staged blobs.
     *  @param currentSnapshot : A HashMap of all blobs in the current
     *  commit.*/
    static void printUntracked(Set<String> stagedBlobs,
                               HashMap<String, String> currentSnapshot) {
        ArrayList<String> untracked = new ArrayList<String>();
        List<String> filesInCWD = Utils.plainFilenamesIn(WorkingDirectory.CWD);
        for (String fileName : filesInCWD) {
            File file = Utils.join(WorkingDirectory.CWD, fileName);
            if (!file.isDirectory()) {
                if (!stagedBlobs.contains(fileName)) {
                    if (currentSnapshot == null) {
                        untracked.add(fileName);
                    } else {
                        Set<String> blobsInCommit = currentSnapshot.keySet();
                        if (!blobsInCommit.contains(fileName)) {
                            untracked.add(fileName);
                        }
                    }
                }
            }
        }
        Collections.sort(untracked);
        for (String untrackedFile : untracked) {
            System.out.println(untrackedFile);
        }
    }

    /** Method displays what branches currently exist, with an asterisk
     *  to indicate which is the current branch pointed to by HEAD, and
     *  what files have been staged, removed, modified, and are untracked. */
    static void status() {
        ArrayList<String> modNotStagedForCommit = new ArrayList<String>();
        Branch head = readHead();
        Branch branch = Branch.readBranch(head.getBranchPointer().getName());
        Commit currentCommit = Commit.readCommit(branch);
        HashMap<String, String> currentSnapshot = currentCommit.getSnapshot();
        StagingArea stage = StagingArea.readStage();
        Set<String> stagedBlobs = printBranchesStagingRemoved(branch,
                stage, modNotStagedForCommit);
        System.out.println("=== Modifications Not Staged For Commit ===");
        if (currentSnapshot != null) {
            Set<String> blobsInCommit = currentSnapshot.keySet();
            Iterator blobInCommitIter = blobsInCommit.iterator();
            while (blobInCommitIter.hasNext()) {
                String blobName = (String) blobInCommitIter.next();
                if (!stagedBlobs.contains(blobName)) {
                    File blobFile = Utils.join(WorkingDirectory.CWD, blobName);
                    if (blobFile.exists()) {
                        byte[] blob = Utils.readContents(blobFile);
                        String blobSHA = Utils.sha1(blob);
                        if (!blobSHA.equals(currentSnapshot.get(blobName))) {
                            modNotStagedForCommit.add(blobName + " (modified)");
                        }
                    } else if (!stage.getRemovedFiles().contains(blobName)
                            && !blobFile.exists()) {
                        modNotStagedForCommit.add(blobName + " (deleted)");
                    }
                }
            }
        }
        Collections.sort(modNotStagedForCommit);
        Iterator modNotStagedIter = modNotStagedForCommit.iterator();
        while (modNotStagedIter.hasNext()) {
            System.out.println(modNotStagedIter.next());
        }
        System.out.println();
        System.out.println("=== Untracked Files ===");
        printUntracked(stagedBlobs, currentSnapshot);
        System.out.println();
        stage.saveStage();
    }

    /** Helper function prints the branches, removed files, and staging area.
     *  @param branch : The current branch.
     *  @param stage : The staging area.
     *  @param modNotStagedForCommit : A Set of file names that are modified
     *  but not staged for commit.
     *  @return : A set of blobs. */
    static Set<String> printBranchesStagingRemoved(Branch branch,
            StagingArea stage, ArrayList<String> modNotStagedForCommit) {
        System.out.println("=== Branches ===");
        List<String> branches = Utils.plainFilenamesIn(WorkingDirectory.REFS);
        Iterator branchIter = branches.iterator();
        while (branchIter.hasNext()) {
            String branchName = (String) branchIter.next();
            if (branch.getName().equals(branchName)) {
                System.out.println("*" + branchName);
            } else {
                System.out.println(branchName);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files ===");
        Set<String> stagedBlobs = stage.getFilesInStage().keySet();
        Object[] stagedArray = stage.getFilesInStage().keySet().toArray();
        Arrays.sort(stagedArray);
        for (Object blob : stagedArray) {
            String blobName = (String) blob;
            System.out.println(blobName);
            File blobFile = Utils.join(WorkingDirectory.CWD, blobName);
            if (blobFile.exists()) {
                byte[] blobContent = Utils.readContents(blobFile);
                String blobInWorkingDirSha = Utils.sha1(blobContent);
                if (!blobInWorkingDirSha.equals(
                        stage.getFilesInStage().get(blobName))) {
                    modNotStagedForCommit.add(blobName + " (modified)");
                }
            } else {
                modNotStagedForCommit.add(blobName + " (deleted)");
            }
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        Iterator removedIter = stage.getRemovedFiles().iterator();
        while (removedIter.hasNext()) {
            System.out.println((String) removedIter.next());
        }
        System.out.println();
        return stagedBlobs;
    }

    /** Checkout the file in head commit and puts it in the Working Directory,
     *  overwriting the version of the file that's already there if there is
     *  one.
     *  @param fileName : the arguments of checkout command.*/
    static void checkout(String[] fileName) {
        String name = fileName[2];
        Branch head = readHead();
        Branch branch = head.getBranchPointer();
        Commit currentCommit = Commit.readCommit(branch.getCommitID());
        HashMap<String, String> currentSnap = currentCommit.getSnapshot();
        if (!currentSnap.containsKey(name)) {
            throw new GitletException("File does not exist in that commit.");
        } else {
            String blobSha = currentSnap.get(name);
            byte[] blob = readBlobFromRepo(blobSha);
            saveBlobToCWD(name, blob);
        }
    }

    /** Checkout version of file in given commit id.
     *  @param commitID : The Sha-1 hash of the commit.
     *  @param fileName : The file name in the given commit's snapshot. */
    static void checkout(String commitID, String fileName) {
        File commitTreeFile = Utils.join(WorkingDirectory.REPO,
                "commitTree");
        CommitTree commitTree = Utils.readObject(commitTreeFile,
                CommitTree.class);
        Iterator iter = commitTree.getAllCommits().iterator();
        boolean commitIDFound = false;
        boolean keepLooking = true;
        while (iter.hasNext() && keepLooking) {
            String commitSha = (String) iter.next();
            Commit commit = Commit.readCommit(commitSha);
            if (commit.getSha1().contains(commitID)) {
                commitIDFound = true;
                keepLooking = false;
                HashMap<String, String> currentSnap = commit.getSnapshot();
                if (!currentSnap.containsKey(fileName)) {
                    throw new GitletException("File does not exist in that"
                            + " commit.");
                } else {
                    String blobSha = currentSnap.get(fileName);
                    byte[] blob = readBlobFromRepo(blobSha);
                    saveBlobToCWD(fileName, blob);
                }
            }
        }
        if (!commitIDFound) {
            throw new GitletException("No commit with that id exists.");
        }
    }

    /** Checkout the commit pointed to by the given branch.
     *  @param branchName : The name of the branch user wishes to
     *  checkout. */
    static void checkout(String branchName) {
        Branch branch = Branch.readBranch(branchName);
        Branch head = readHead();
        Branch headBranch = head.getBranchPointer();
        if (headBranch.getName().equals(branchName)) {
            throw new GitletException("No need to checkout the current"
                    + " branch.");
        }
        Commit currentCommit = Commit.readCommit(headBranch.getCommitID());
        HashMap<String, String> currentSnap = currentCommit.getSnapshot();
        if (currentSnap == null) {
            currentSnap = new HashMap<String, String>();
        }
        Commit checkedOutCommit = Commit.readCommit(branch.getCommitID());
        HashMap<String, String> checkedOutSnap = checkedOutCommit.getSnapshot();

        Set<String> currentBlobs = currentSnap.keySet();
        if (checkedOutSnap == null) {
            for (String trackedBlob : currentBlobs) {
                File trackedBlobFile = Utils.join(WorkingDirectory.CWD,
                        trackedBlob);
                Utils.restrictedDelete(trackedBlobFile);
            }
        } else {
            Set<String> checkedOutBlobs = checkedOutSnap.keySet();
            for (String checkedOutBlob : checkedOutBlobs) {
                File workingFile = Utils.join(WorkingDirectory.CWD,
                        checkedOutBlob);
                if (workingFile.exists()
                        && !currentSnap.containsKey(checkedOutBlob)) {
                    throw new GitletException("There is an untracked file"
                            + " in the way; delete it or add it first.");
                }
            }
            for (String trackedBlob : currentBlobs) {
                if (!checkedOutSnap.containsKey(trackedBlob)) {
                    File trackedBlobFile = Utils.join(WorkingDirectory.CWD,
                            trackedBlob);
                    Utils.restrictedDelete(trackedBlobFile);
                }
            }
            Iterator blobIter = checkedOutSnap.keySet().iterator();
            while (blobIter.hasNext()) {
                String blobName = (String) blobIter.next();
                String blobSha = checkedOutSnap.get(blobName);
                byte[] blob = readBlobFromRepo(blobSha);
                saveBlobToCWD(blobName, blob);
            }
        }
        head.setBranchPointer(branch);
        saveHead(head);
        saveBranch(branch);
        StagingArea stage = StagingArea.readStage();
        stage.getFilesInStage().clear();
        stage.getRemovedFiles().clear();
        stage.saveStage();
    }

    /** Checks out all the files tracked by the given commit.
     *  @param commitID : The Sha-1 hash of the commit user wishes to
     *  checkout. */
    static void reset(String commitID) {
        CommitTree commitTree = Utils.readObject(Utils.join(REPO,
                "commitTree"),
                CommitTree.class);
        boolean commitFound = false;
        Commit commit = null;
        for (String commitSHA : commitTree.getAllCommits()) {
            if (commitSHA.contains(commitID)) {
                commitFound = true;
                commit = Commit.readCommit(commitSHA);
            }
        }
        if (!commitFound) {
            throw new GitletException("No commit with that id exists.");
        }
        Branch head = WorkingDirectory.readHead();
        Branch branchHead = Branch.readBranch(
                head.getBranchPointer().getName());
        Commit currentCommit = Commit.readCommit(branchHead.getCommitID());
        HashMap<String, String> currentSnap = currentCommit.getSnapshot();
        HashMap<String, String> commitSnap = commit.getSnapshot();
        if (currentSnap == null) {
            currentSnap = new HashMap<String, String>();
        } else if (commitSnap == null) {
            commitSnap = new HashMap<String, String>();
        }
        Set<String> checkedOutBlobs = commitSnap.keySet();
        for (String checkedOutBlob : checkedOutBlobs) {
            File workingFile = Utils.join(WorkingDirectory.CWD,
                    checkedOutBlob);
            if (workingFile.exists()
                    && !currentSnap.containsKey(checkedOutBlob)) {
                throw new GitletException("There is an untracked file"
                        + " in the way; delete it or add it first.");
            }
        }
        Set<String> currentBlobs = currentSnap.keySet();
        for (String trackedBlob : currentBlobs) {
            if (!commitSnap.containsKey(trackedBlob)) {
                File trackedBlobFile = Utils.join(WorkingDirectory.CWD,
                        trackedBlob);
                Utils.restrictedDelete(trackedBlobFile);
            }
        }
        Iterator blobIter = commitSnap.keySet().iterator();
        while (blobIter.hasNext()) {
            String blobName = (String) blobIter.next();
            String blobSha = commitSnap.get(blobName);
            byte[] blob = readBlobFromRepo(blobSha);
            saveBlobToCWD(blobName, blob);
        }
        branchHead.setCommitID(commit.getSha1());
        head.setBranchPointer(branchHead);
        saveBranch(branchHead);
        saveHead(head);
        StagingArea stage = StagingArea.readStage();
        stage.getFilesInStage().clear();
        stage.getRemovedFiles().clear();
        stage.saveStage();
    }

    /** Check for failure cases during merge command.
     *  @param branchName : The name of branch user wishes to merge in. */
    static void checkMergeFailures(String branchName) {
        StagingArea stage = StagingArea.readStage();
        if (!stage.getFilesInStage().isEmpty()
                || !stage.getRemovedFiles().isEmpty()) {
            throw new GitletException("You have uncommitted changes.");
        }
        File branchFile = Utils.join(WorkingDirectory.REFS, branchName);
        if (!branchFile.exists()) {
            throw new GitletException("A branch with that name does not"
                    + " exist.");
        }
        Branch givenBranch = Utils.readObject(branchFile, Branch.class);
        Branch head = readHead();
        Branch currentBranch = head.getBranchPointer();
        if (givenBranch.getCommitID().equals(currentBranch.getCommitID())) {
            throw new GitletException("Cannot merge a branch with itself.");
        }
    }

    /** Check for merge exceptions.
     *  @param head : The head pointer.
     *  @param currentBranch : The current branch.
     *  @param splitPoint : The split point commit.
     *  @param mergedInCommit : The commit to merge in.
     *  @param currentCommit : The current commit getting merged into. */
    static void checkMergeExceptions(Branch head, Branch currentBranch,
                              Commit splitPoint, Commit mergedInCommit,
                              Commit currentCommit) {
        if (splitPoint.equals(mergedInCommit)) {
            throw new GitletException("Given branch is an ancestor of the"
                    + " current branch.");
        }
        if (splitPoint.equals(currentCommit)) {
            currentBranch.setBranch(mergedInCommit);
            head.setHead(currentBranch);
            saveBranch(currentBranch);
            saveHead(head);
            for (String trackedBlob : currentCommit.getSnapshot().keySet()) {
                if (!mergedInCommit.getSnapshot().containsKey(trackedBlob)) {
                    File trackedBlobFile = Utils.join(WorkingDirectory.CWD,
                            trackedBlob);
                    Utils.restrictedDelete(trackedBlobFile);
                }
            }
            throw new GitletException("Current branch fast-forwarded.");
        }
    }

    /** Cases where blob is in all snapshots.
     *  @param blobName : Name of the blob.
     *  @param splitPointSnapshot : The snapshot of the split point.
     *  @param currentSnapshot : The snapshot of the split point.
     *  @param givenSnapshot : The snapshot of the split point.
     *  @param stage : The staging area. */
    static void fileInAll(String blobName,
                          HashMap<String, String> splitPointSnapshot,
                          HashMap<String, String> currentSnapshot,
                          HashMap<String, String> givenSnapshot,
                          StagingArea stage) {
        if (splitPointSnapshot.containsKey(blobName)
                && currentSnapshot.containsKey(blobName)
                && givenSnapshot.containsKey(blobName)) {
            if (!givenSnapshot.get(blobName).equals(
                    splitPointSnapshot.get(blobName))
                    && splitPointSnapshot.get(blobName).equals(
                    currentSnapshot.get(blobName))) {
                String blobSha = givenSnapshot.get(blobName);
                byte[] blob = readBlobFromRepo(blobSha);
                saveBlobToCWD(blobName, blob);
                stage.getFilesInStage().put(blobName, blobSha);
            }
        }
    }

    /** Cases where blob is in all but one of the snapshots.
     *  @param blobName : Name of the blob.
     *  @param splitPointSnapshot : The snapshot of the split point.
     *  @param currentSnapshot : The snapshot of the split point.
     *  @param givenSnapshot : The snapshot of the split point.
     *  @param stage : The staging area. */
    static void fileNotInOne(String blobName,
                             HashMap<String, String> splitPointSnapshot,
                             HashMap<String, String> currentSnapshot,
                             HashMap<String, String> givenSnapshot,
                             StagingArea stage) {
        if (splitPointSnapshot.containsKey(blobName)
                && currentSnapshot.containsKey(blobName)
                && !givenSnapshot.containsKey(blobName)) {
            if (currentSnapshot.get(blobName).equals(
                    splitPointSnapshot.get(blobName))) {
                Utils.restrictedDelete(blobName);
                stage.getRemovedFiles().add(blobName);
            }
        }
        if (splitPointSnapshot.containsKey(blobName)
                && !currentSnapshot.containsKey(blobName)
                && givenSnapshot.containsKey(blobName)) {
            if (givenSnapshot.get(blobName).equals(
                    splitPointSnapshot.get(blobName))) {
                Utils.restrictedDelete(blobName);
            }
        }
    }

    /** Create the set of all blob names found in all three snapshots.
     *  @param splitPointSnapshot : The snapshot of the split point.
     *  @param currentSnapshot : The snapshot of the split point.
     *  @param givenSnapshot : The snapshot of the split point.
     *  @return An iterator that goes through all the names. */
    static Iterator makeSet(HashMap<String, String> splitPointSnapshot,
                            HashMap<String, String> currentSnapshot,
                            HashMap<String, String> givenSnapshot) {
        HashSet<String> allBlobs = new HashSet<String>(givenSnapshot.keySet());
        allBlobs.addAll(currentSnapshot.keySet());
        allBlobs.addAll(splitPointSnapshot.keySet());
        Iterator iter = allBlobs.iterator();
        return iter;
    }

    /** Check if there is an untracked file in the way.
     *  @param blobName : Name of the blob.
     *  @param currentSnapshot : The snapshot of the split point.
     *  @param givenSnapshot : The snapshot of the split point.*/
    static void checkUntracked(String blobName,
                               HashMap<String, String> currentSnapshot,
                               HashMap<String, String> givenSnapshot) {
        File workingFile = Utils.join(WorkingDirectory.CWD, blobName);
        if (givenSnapshot.containsKey(blobName)) {
            if (workingFile.exists() && !currentSnapshot.containsKey(
                    blobName)) {
                throw new GitletException("There is an untracked file"
                        + " in the way; delete it or add it first.");
            }
        }
    }

    /** Merges files from the given branch into the current branch.
     *  @param branchName : The name of branch user wishes to merge in. */
    static void merge(String branchName) {
        checkMergeFailures(branchName);
        StagingArea stage = StagingArea.readStage();
        File branchFile = Utils.join(WorkingDirectory.REFS, branchName);
        Branch givenBranch = Utils.readObject(branchFile, Branch.class);
        Commit mergedInCommit = Commit.readCommit(givenBranch.getCommitID());
        Branch head = readHead();
        Branch currentBranch = readHead().getBranchPointer();
        Commit currentCommit = Commit.readCommit(currentBranch.getCommitID());
        HashMap<String, Integer> ancestors = new HashMap<String, Integer>();
        findCurrentAncestors(currentCommit.getSha1(), ancestors, 0);
        int[] min = {Integer.MAX_VALUE};
        String[] result = {mergedInCommit.getSha1()};
        findSplitPoints(mergedInCommit.getSha1(), ancestors, min, result);
        Commit splitPoint = Commit.readCommit(result[0]);
        checkMergeExceptions(head, currentBranch,
                splitPoint, mergedInCommit, currentCommit);
        HashMap<String, String> givenSnapshot = mergedInCommit.getSnapshot();
        HashMap<String, String> currentSnapshot = currentCommit.getSnapshot();
        HashMap<String, String> splitPointSnapshot = splitPoint.getSnapshot();
        boolean encounteredMergeConflict = false;
        Iterator allBlobsIter = makeSet(splitPointSnapshot,
                currentSnapshot, givenSnapshot);
        while (allBlobsIter.hasNext()) {
            String blobName = (String) allBlobsIter.next();
            checkUntracked(blobName, currentSnapshot, givenSnapshot);
            fileInAll(blobName, splitPointSnapshot,
                    currentSnapshot, givenSnapshot, stage);
            if (!splitPointSnapshot.containsKey(blobName)
                    && givenSnapshot.containsKey((blobName))) {
                String blobSha = givenSnapshot.get(blobName);
                byte[] blob = readBlobFromRepo(blobSha);
                saveBlobToCWD(blobName, blob);
                stage.getFilesInStage().put(blobName, blobSha);
            }
            fileNotInOne(blobName, splitPointSnapshot,
                    currentSnapshot, givenSnapshot, stage);
            if (modifiedInDiffWays(blobName, splitPointSnapshot,
                    currentSnapshot, givenSnapshot)) {
                encounteredMergeConflict = true;
                String newSHA1 = replaceContents(blobName,
                        currentSnapshot, givenSnapshot);
                stage.getFilesInStage().put(blobName, newSHA1);
            }
        }
        stage.saveStage();
        String message = "Merged " + givenBranch.getName()
                + " into " + currentBranch.getName() + ".";
        new MergeCommit(message, currentCommit.getSha1(),
                mergedInCommit.getSha1());
        if (encounteredMergeConflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Returns true if files are modified in different ways.
     *  @param blobName : The name of the blob to check if modified.
     *  @param splitPointSnapshot : The snapshot of the split point commit.
     *  @param currentSnapshot : The snapshot of the current commit.
     *  @param givenSnapshot : The snapshot of the given commit.
     *  @return : A boolean. True if modified in different ways. */
    static boolean modifiedInDiffWays(String blobName,
                                      HashMap<String, String>
                                              splitPointSnapshot,
                                      HashMap<String, String>
                                              currentSnapshot,
                                      HashMap<String, String>
                                              givenSnapshot) {
        boolean bothChanged = false;
        boolean bothDifferent = false;
        boolean oneChangedAndOneDeleted = false;
        boolean absentAtSplitAndDifferent = false;
        if (currentSnapshot.containsKey(blobName)
                && givenSnapshot.containsKey(blobName)) {
            if (!currentSnapshot.get(blobName).equals(
                    givenSnapshot.get(blobName))) {
                bothDifferent = true;
            }
            if (splitPointSnapshot.containsKey(blobName)) {
                if (!splitPointSnapshot.get(blobName).equals(
                        givenSnapshot.get(blobName))
                        && !splitPointSnapshot.get(blobName).equals(
                                currentSnapshot.get(blobName))) {
                    bothChanged = true;
                }
            } else if (!splitPointSnapshot.containsKey(blobName)
                    && bothDifferent) {
                absentAtSplitAndDifferent = true;
            }
        }
        if (splitPointSnapshot.containsKey(blobName)) {
            if (currentSnapshot.containsKey(blobName)
                    && !givenSnapshot.containsKey(blobName)) {
                if (!currentSnapshot.get(blobName).equals(
                        splitPointSnapshot.get(blobName))) {
                    oneChangedAndOneDeleted = true;
                }
            } else if (!currentSnapshot.containsKey(blobName)
                    && givenSnapshot.containsKey(blobName)) {
                if (!givenSnapshot.get(blobName).equals(
                        splitPointSnapshot.get(blobName))) {
                    oneChangedAndOneDeleted = true;
                }
            }
        }
        return (bothChanged && bothDifferent)
                || oneChangedAndOneDeleted || absentAtSplitAndDifferent;
    }

    /** Replace contents of conflicted file with contents of current file
     *  and given file. Returns the new file's SHA-1.
     *  @param blobName : The name of the blob file to change the contents of.
     *  @param currentSnapshot : The snapshot of the current commit.
     *  @param givenSnapshot : The snapshot of the given commit.
     *  @return The new Sha-1 Hash of the blob. */
    static String replaceContents(String blobName,
                                  HashMap<String, String> currentSnapshot,
                                  HashMap<String, String> givenSnapshot) {
        String beginning = "<<<<<<< HEAD" + System.lineSeparator();
        String middle = "=======" + System.lineSeparator();
        String end = ">>>>>>>" + System.lineSeparator();
        String contentsOfCurrent = "";
        String contentsOfGiven = "";
        if (currentSnapshot.containsKey(blobName)) {
            String currentBlobSha = currentSnapshot.get(blobName);
            File currentBlobFile = Utils.join(WorkingDirectory.REPO,
                    currentBlobSha);
            contentsOfCurrent = Utils.readContentsAsString(currentBlobFile);
        }
        if (givenSnapshot.containsKey(blobName)) {
            String givenBlobSha = givenSnapshot.get(blobName);
            File givenBlobFile = Utils.join(WorkingDirectory.REPO,
                    givenBlobSha);
            contentsOfGiven = Utils.readContentsAsString(givenBlobFile);
        }
        String replaced = beginning + contentsOfCurrent + middle
                + contentsOfGiven + end;
        File replacedFile = Utils.join(WorkingDirectory.CWD, blobName);
        Utils.writeContents(replacedFile, replaced);
        byte[] blob = StagingArea.readBlobFromCWD(blobName);
        String blobSHA = Utils.sha1(blob);
        return blobSHA;
    }

    /** Find all the ancestors of the current commit. Used in the process of
     *  finding the split point. Recursive function.
     *  @param commitID : The Sha-1 Hash of the commit.
     *  @param ancestors : HashMap of ancestors updated with each node
     *  visited. Keys are the Sha-1, Values are the length from the head of
     *  branch.
     *  @param path : Length from the head of branch. */
    static void findCurrentAncestors(String commitID,
                                   HashMap<String, Integer> ancestors,
                                   int path) {

        if (commitID != null || ancestors.containsKey(commitID)) {
            ancestors.put(commitID, path);
            Commit commit = Commit.readCommit(commitID);
            findCurrentAncestors(commit.getPreviousCommit(), ancestors,
                    path + 1);
            if (commit instanceof MergeCommit) {
                findCurrentAncestors(((MergeCommit) commit).
                                getPreviousCommit2(),
                        ancestors, path + 1);
            }
        }
    }

    /** Find the split point using the ancestors of the current branch and the
     *  given branch. Recursive function.
     *  Returns the SHA-1 id of the commit that is the split point.
     *  @param commitID : The Sha-1 Hash of a commit. Starts at current head.
     *  @param ancestors : The HashMap of ancestors found from
     *  findGivenAncestors().
     *  @param min : An int[] of length == 1. Used to keep track of the
     *  smallest path.
     *  @param result : A String[] of length == 1.
     *  The actual split point. Updated until one with the shortest
     *  path from the current is found. */
    static void findSplitPoints(String commitID,
                                HashMap<String, Integer> ancestors,
                                int[] min, String[] result) {
        Commit commit = Commit.readCommit(commitID);
        if (ancestors.containsKey(commitID)) {
            if (ancestors.get(commitID) < min[0]) {
                min[0] = ancestors.get(commitID);
                result[0] = commitID;
            }
        } else {
            if (commit.getPreviousCommit() != null) {
                findSplitPoints(commit.getPreviousCommit(), ancestors,
                        min, result);
                if (commit instanceof MergeCommit) {
                    findSplitPoints(((MergeCommit) commit).getPreviousCommit2(),
                            ancestors, min, result);
                }
            }
        }
    }


    /** Read the blob with the given file name from the repo directory
     *  and return its contents.
     *  @param blobSHA : The SHa-1 Hash of the blob to be read.
     *  @return : The byte array that is the blob's contents. */
    static byte[] readBlobFromRepo(String blobSHA) {
        File blobFile = Utils.join(WorkingDirectory.REPO, blobSHA);
        if (!blobFile.exists()) {
            throw new GitletException("File does not exist.");
        } else {
            return Utils.readContents(blobFile);
        }
    }

    /** Write the blob into the Working Directory.
     *  @param blobName : The file name of the blob.
     *  @param byteContent : The contents of the blob. */
    private static void saveBlobToCWD(String blobName, byte[] byteContent) {
        File blobFile = Utils.join(WorkingDirectory.CWD, blobName);
        Utils.writeContents(blobFile, byteContent);
    }

    /** Current working directory. */
    static final File CWD = new File(".");

    /** Our .gitlet repository. */
    static final File REPO = Utils.join(CWD, ".gitlet");

    /** A folder for all the references that point to commits. */
    static final File REFS = Utils.join(REPO, "refs");

    /** A folder for all commits inside the .gitlet repo. */
    static final File COMMITS = Utils.join(REPO, "commits");

}
