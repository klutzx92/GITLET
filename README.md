# GITLET
GITLET: Miniaturized version of Git!

Gitlet Design Document
----------------------

Author: David Oh

**HOW TO USE:**

Make any local directory version controlled by copy and pasting the gitlet directory into it.
Then cd into your directory using the command line and type 'java gitlet.Main init' to initialize the repo.
Now you can use any of the following commands using 'java gitlet.Main <command> <args>'.
Enjoy!

Classes and Data Structures
---------------------------

**** WorkingDirectory ****

This class is the main source of structure for all files in the working directory, the .gitlet repository,
and the staging area. Contains most of the methods that deal with commands and moving around files from one
directory to another.

** Fields **

1. File CWD : The current directory in which all commands are called. This is the working directory.
2. File REPO : The .gitlet repository holds everything that gitlet keeps track of.
3. File REFS : A directory in .gitlet that holds all of the branches.
4. File COMMITS : A directory in .gitlet that holds all of the commits.

**** StagingArea ****

This class represents the staging area. The data structure is a HashMap<String, String> that have file names
for keys and the blob's SHA-1 hash for values. An instance of this class is created, written, and read every
time the user wishes to add or remove a file for the next commit.

** Fields **

1. HashMap<String, String> filesInStage : Keeps track of all the files that are to be staged for the next commit.
2. HashSet<String> removedFiles : Keeps track of all the files that are to be removed from the next commit.
3. String name : The name of this file.

**** Commit ****

This class represents the commit. A commit objects holds the snapshot of the files in the working directory at the
time of the commit. Only files that are staged are added. If a commit does not contain a file but it is in the working
directory, that file is considered to be untracked.

** Fields **

1. HashMap<String, String> snapshot : The snapshot of files that were staged to be committed. The keys are blob's file
names and the values are the blob's Sha-1 hash.

2. String message : The user inputted commit message.

3. String timeStamp : The time at which this commit was created. In standard pacific time zone format.

4. String sha1 : The Sha-1 hash of this commit.

5. String previousCommit : The Sha-1 hash of this commit's parent.

6. CommitTree tree : The commit tree is a record of all the commits ever created.


**** MergeCommit ****

This class is a subclass of the Commit class. It is a special commit created when merging two branches. Different from
a normal commit in that it has two parents instead of one. It inherits all fields from the Commit class and has one
extra field.

** Fields **

1. String previousCommit2 : The Sha-1 hash of the second parent. This parent is the commit that was merged in.


**** Branch ****

This class represents all of the branches, which are pointers to either other branches, in the case for the HEAD, or
pointers to commits.

** Fields **

1. String commitID : Instance variable of the commit id that this branch points to.

2. String name : The name of this branch given by user when created (except for HEAD and MASTER).

3. Branch branchPointer : This field only used by the HEAD pointer, which points to the current branch.

**** CommitTree ****

This class represents the commit tree, a record of all commits ever created. Used by the global-log command.

** Fields **

1. HashSet<String> allCommits : A HashSet that holds all of the commits' Sha-1 hash id.

**** Main ****

The Main class parses out the commands inputted by the user for the gitlet program. Contains checks for argument length
and formatting. Uses a switch statement.

** Fields **


Algorithms
----------

*** WorkingDirectory ***
1. init() : Initializes the .gitlet repository by creating a directory in the current working directory. Automatically
creates an initial commit, a master branch, and the HEAD pointer that points to the master branch. Also initializes the
staging area by constructing a new staging area.

2. createBranch(String branchName) : Creates a new branch by making a file in the REFS directory with the user inputted
name. The newly created branch points to the current commit. User cannot create a branch if the name already exists.

3. rm(String fileName) : This removes the file with the given name from the staging area. It also marks the file
for removal so that it is not to be included in the next commit. It does a restricted delete of the file from the working
directory. If the file is not already tracked or in the staging area, no reason to remove file.

4. status() : This method displays all the branches created, the files that are staged for addition, files that are
marked for removal, files that have been modified but not staged, and all untracked files in the working directory.
- First print branches by reading in as strings from the REFS file. This does so lexicographically.
- Then get the files in the staging area and put them in an array and sort. If a file is modified or missing in the working
directory, add it to the modNotStagedForCommit list. Then print.
- Then get the files in the staging area that are marked for removal and sort. Then print.
- Go through the current snapshot of the current commit and see if any files are modified. If so, add it to the
modNotStagedForCommit list. Then sort list and print.
- Finally, read in the files from the current working directory and check if each file is tracked by the current commit.
If not, add it to the untracked list. Then sort and print.

5. checkout(String[] fileName) : Used to checkout the file in the head commit and put it in the working directory.

6. checkout(String commitID, String fileName) : Used to checkout a file the commit with the given Sha-1 hash id.
The commit is found by using the record of all commits in the commit tree. Commit is read, file is looked up, and placed
in the working directory.

7. checkout(String branchName) : This method checks out the commit that is pointed to by the branch with the given
branch name. The commit is read in using the commit ID found by the branch. Then the files in that commit is compared
against the current commit and the working directory. If a file is there, it is overwritten. If a file is not, it is
removed. If a file with the same name as the file in the checked out commit exists in the working directory but not in
the current commit, warn the user that an untracked file is and to fix it before checking out the branch. Then update
the HEAD pointer to point to this branch.

8. reset(String commitID) : Effectively checks out all the files in the commit with the given Sha-1 hash id. Similar
to checkout(String branchName).

9. merge(String branchName) : Merges the branch with the given branch name into the current branch.
- First check for merge failures using the helper function checkMergeFailures(); this checks that the given branch
exists, is not the current branch, and that there are no files in the staging area.
- Then find the split point commit of the current branch and the given branch. This is done through a couple helper
functions. The algorithm is: Find all the ancestors of the current commit, storing them in a HashMap with values that
correspond to the distance they are from the current commit. Then traverse through the ancestors of the given commit,
stopping when a commit is an ancestor of the current commit. This updates the split point using the minimum of the
values found in the current commit's ancestors. This effectively finds the latest common ancestor that is the closest
to the current commit.
- Update the staging area based on the specs, overwriting files and deleting them from the working directory depending
on the cases.
- Check if there is a merge conflict. If so, replace the contents of the file with the contents of both version. This
is done by the function in Utils readContentsAsString() and concatenating them with the headers. Then write this file
to disk.
- Create a new MergeCommit instance with its parents as the two commits used in the merge.
- Set HEAD branch to point to this new MergeCommit.

*** Commit ***
1. setUp() : Initializes the newly created commit by computing the time stamp using computeTimeStamp(), setting the
commit message, and copying the files from the previous commit, updating that with the files that are staged and marked
for removal in the staging area, and setting the HEAD branch to point to this new commit.

2. log() : Displays all commit information starting from the current commit to the initial commit. Done recursively by
passing in each commit's parent commit. If a commit is a MergeCommit, has a separate line that displays both parent's
abbreviated Sha-1 hash id.

3. global-log() : Displays all commit information of all commits ever created. Done by iterating through the
commit tree.

4. find(String commitMessage) : Displays the commit information of the commit with the given commit message. Done by
searching through the commit tree to find the commit and comparing commit messages.

*** StagingArea ***
1. add(String fileName) : This method adds the file with the given file name in the working directory into the staging
area. Done by reading in the file with the given file name, computing the file's Sha-1 hash, and storing it into a
HashMap<String, String> where the keys are the file names and the values are the Sha-1 hash ids. If a file is already
in the staging area and not modified, does nothing. If modified, overwrite it with the new Sha-1 id. If the file has
not been modified since the most recent commit, remove it from the staging area. If a file is marked for removal,
remove it from the staging area.

2. Also contains various methods for reading and writing files from the working directory and the .gitlet repository.

*** Branch ***
1. Contains various methods for reading and writing branches to and from the REFS directory in the .gitlet repo.

Persistence
-----------

1. In order to save files in the working directory and all the versions of files in the created commits, a .gitlet
repository is created in the working directory. If a .gitlet directory does not previously exist, a new one will be
created. This folder contains all of the files that are to persist through each successive commands.

2. Every time a command is executed, the HEAD pointer is read from the .gitlet repository. The HEAD pointer is used to
get the current branch, which is used to read in the current commit and all of its files in its snapshot. The staging
area is also read when needed. At the end of an executable command, each relevant object is saved by writing it onto
disk using various write methods in each object's class.
