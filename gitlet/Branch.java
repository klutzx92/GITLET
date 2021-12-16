package gitlet;

import java.io.File;
import java.io.Serializable;

/** Branches are pointers that point to commits. Branch stores the commit's
 *  Sha-1 Hash instead of storing an actual reference to the commit object.
 *  @author David Oh */
public class Branch implements Serializable {

    /** This constructor only used by head.
     *  @param headName : The name of the head pointer.
     *  @param branch : The initial branch that head points to. */
    Branch(String headName, Branch branch) {
        this.name = headName;
        branchPointer = branch;
    }

    /** Creates a new branch that points to given commit.
     *  @param branchName : The name of the branch inputted by the user.
     *  @param commit : The commit that this branch points to. */
    Branch(String branchName, Commit commit) {
        this.name = branchName;
        this.commitID = commit.getSha1();
    }

    /** Read branch with the given name.
     *  @param fileName : The name of the branch file.
     *  @return : The branch read from disk. */
    static Branch readBranch(String fileName) {
        File branchFile = Utils.join(WorkingDirectory.REFS, fileName);
        if (!branchFile.exists()) {
            throw new GitletException("No such branch exists.");
        }
        return Utils.readObject(branchFile, Branch.class);
    }

    /** Set this branch pointer to point to this commit.
     *  @param commit : The commit to set this branch to. */
    void setBranch(Commit commit) {
        commitID = commit.getSha1();
        WorkingDirectory.saveBranch(this);
    }


    /** Change head to point to this new branch.
     *  @param branch : The branch to set this head to. */
    void setHead(Branch branch) {
        branchPointer = branch;
        WorkingDirectory.saveHead(this);
    }

    /** Get the commit id of the commit this branch points to.
     *  @return : The Sha-1 Hash of this branch's commit. */
    String getCommitID() {
        return commitID;
    }

    /** Set this branch to point to the commit with this commitID.
     *  @param sha1 : The Sha-1 Hash of the commit this branch gets set to. */
    void setCommitID(String sha1) {
        commitID = sha1;
    }

    /** Get the name of this branch.
     *  @return : The name of this branch. */
    String getName() {
        return name;
    }

    /** Get the branch that head points to.
     *  @return : The branch pointed to by head. */
    Branch getBranchPointer() {
        return branchPointer;
    }

    /** Set the branch that head points to.
     *  @param branch : The branch pointed to by head. */
    void setBranchPointer(Branch branch) {
        branchPointer = branch;
    }

    /** The id of the commit this branch points to. */
    private String commitID;

    /** Name of this branch pointer. */
    private String name;

    /** Head points to a branch. Branch pointer is effectively head. */
    private Branch branchPointer;
}
