package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

/** Files added to the staging area through the add command are
 *  staged for the next commit.
 *  @author David Oh */
public class StagingArea implements Serializable {

    /** Constructor for Staging Area. */
    StagingArea() {
        name = "stage";
        filesInStage = new HashMap<>();
        removedFiles = new HashSet<String>();
    }

    /** Save the stage file. */
    void saveStage() {
        Utils.writeObject(Utils.join(WorkingDirectory.REPO, this.name),
                this);
    }

    /** Read the staging area file from the .gitlet repository and return the
     *  staging area object to update during add.
     *  @return : The staging area file from disk stored in .gitlet director. */
    static StagingArea readStage() {
        File stageFile = Utils.join(WorkingDirectory.REPO, "stage");
        return Utils.readObject(stageFile, StagingArea.class);
    }

    /** Read the blob with the given file name from the working directory
     *  and return its contents (String / SHA ?).
     *  @param fileName : The name of Blob to read.
     *  @return : The contents of the read blob. */
    static byte[] readBlobFromCWD(String fileName) {
        File blobFile = Utils.join(WorkingDirectory.CWD, fileName);
        if (!blobFile.exists()) {
            throw new GitletException("File does not exist.");
        } else {
            return Utils.readContents(blobFile);
        }
    }

    /** Write the blob to the .gitlet repository.
     *  @param blobID : The Sha-1 Hash of the blob to write.
     *  @param byteContent : The contents of the blob. */
    private static void saveBlobToRepo(String blobID, byte[] byteContent) {
        File blobFile = Utils.join(WorkingDirectory.REPO, blobID);
        Utils.writeContents(blobFile, byteContent);
    }

    /** Add a copy of the file with the given name from the working
     *  directory to the staging area. Method reads the blob, saves it
     *  to disk, reads the staging area, adds the blob to the staging area,
     *  then writes the updated staging area back to disk.
     *  @param fileName : The name of the file user wishes to add. */
    void add(String fileName) {
        byte[] blob = readBlobFromCWD(fileName);
        String blobSHA = Utils.sha1(blob);
        saveBlobToRepo(blobSHA, blob);
        Branch head = WorkingDirectory.readHead();
        Branch branch = Branch.readBranch(head.getBranchPointer().getName());
        Commit currentCommit = Commit.readCommit(branch);
        HashMap<String, String> currentSnap = currentCommit.getSnapshot();
        if (alreadyStaged(fileName)) {
            String stagedBlobSha = filesInStage.get(fileName);
            filesInStage.replace(fileName, stagedBlobSha, blobSHA);
        } else {
            filesInStage.put(fileName, blobSHA);
        }
        if (currentSnap != null && currentSnap.containsKey(fileName)) {
            if (currentSnap.get(fileName).equals(blobSHA)) {
                filesInStage.remove(fileName);
            }
        }
        if (removedFiles.contains(fileName)) {
            removedFiles.remove(fileName);
        }
        saveStage();
    }

    /** Check if the file user wishes to add is already staged. Returns
     *  true if file is already staged. False otherwise.
     *  @param fileName : The name of the file to check.
     *  @return : A boolean. True if file is in the staging area. */
    boolean alreadyStaged(String fileName) {
        return filesInStage.containsKey(fileName);
    }

    /** Get the files in the staging area.
     *  @return : The HashMap that contains the files in the staging area. */
    HashMap<String, String> getFilesInStage() {
        return filesInStage;
    }

    /** Get the files marked for removal..
     *  @return : The HashSet that contains the files marked for removal. */
    HashSet<String> getRemovedFiles() {
        return removedFiles;
    }

    /** A HashMap between file names and their contents that hold
     *  all the files that the user wishes to add to the staging area.
     *  Contents of the blobs is the SHA-1 hash string. <name, SHA-1>. */
    private HashMap<String, String> filesInStage;

    /** A list that keeps track of all files that are staged to be removed. */
    private HashSet<String> removedFiles;

    /** Staging area file name. */
    private String name;
}
