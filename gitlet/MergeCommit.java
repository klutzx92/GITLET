package gitlet;

/** MergeCommit is a special type of commit formed from merging two branches.
 *  @author David Oh */
public class MergeCommit extends Commit {

    /** Creates a new commit that results from merging two branches.
     *  In Gitlet, we limit our merges to those than only involve two parents:
     *  The snapshot we merge INTO and the snapshot that I merge IN.
     *  @param message : The commit message.
     *  @param cInto : The first parent that was the current head commit.
     *  @param cIn : The second parent that was the given commit. */
    MergeCommit(String message, String cInto, String cIn) {
        this.message = message;
        previousCommit = cInto;
        previousCommit2 = cIn;
        timeStamp = computeTime();
        setUp();
    }

    /** Get the second parent of this merge commit.
     *  @return : The Sha-1 Hash of the second parent commit. */
    String getPreviousCommit2() {
        return previousCommit2;
    }

    /** SHA-1 ID of the second parent of new commit is the snapshot
     *  created by merging parent2 IN. i.e., the commit on the
     *  branch I am NOT currently in. */
    private String previousCommit2;

}
