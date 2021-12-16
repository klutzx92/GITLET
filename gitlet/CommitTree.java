package gitlet;

import java.io.Serializable;
import java.util.HashSet;

/** The CommitTree class keeps track of all commits ever created using a
 *  HashSet.
 *  @author David Oh*/
public class CommitTree implements Serializable {

    /** Construct a new instance of the commitTree. */
    CommitTree() {
        allCommits = new HashSet<>();
    }

    /** Get all the commits ever created.
     *  @return : The HashSet that holds all of the commits. */
    HashSet<String> getAllCommits() {
        return allCommits;
    }

    /** A Hashset of all commits in the commit tree. */
    private HashSet<String> allCommits;
}
