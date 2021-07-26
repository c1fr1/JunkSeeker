
## About
JunkSeeker is a command-line tool that helps identify what files and folders are taking up the most space in a given
directory.

The tool creates a virtual file tree that stores the total size of a directory with the directory object, so the 
hierarchy can be traversed without having to re-calculate directory sizes. It uses both the logical file size, and an
estimate of the physical file size. The estimation works decently well on exFAT (slight underestimate), and works OK for
NTFS. Make sure to set the cluster size with the `size` command. 

Uses the Windows API so its windows only atm, unless the C interop does something magical.

## Commands

### always available
|command| function|
|---|---|
| `help` / `man` | brings up this message |
| `exit` / `q` | exits the program |

### available before the file tree has been created
|command| function|
|---|---|
| `size [new size]`          | sets the cluster size used for estimating physical file size |
| `interval [file interval]` | sets how frequently the number of files discovered is printed while creating the virtual file tree |
| `start`                    | starts creating the virtual file tree |

### available after the file tree has been created
|command| function|
|---|---|
| `cd [folder name]` | changes directory |
| `rm [file or folder name]` | deletes the file or folder in the file system and the virtual file tree |
| `root` | changes the "working directory" to the root of the virtual file tree (true working directory) |