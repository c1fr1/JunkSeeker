import Settings.CLUSTER_SIZE
import Settings.INTERVAL
import platform.posix.*

fun main(args : Array<String>) {
	val path = if (args.isEmpty()) { "." } else { args[0] }
	var inputString : String


	do {
		print(" > ")
		inputString = readLine() ?: "exit"

		if (inputString.startsWith("size")) {
			if (!inputString.startsWith("size ")) {
				println("cluster size is currently $CLUSTER_SIZE")
				continue
			}
			val newClusterSize = inputString.removePrefix("size ").toIntOrNull()
			if (newClusterSize == null) {
				println("invalid number, cluster size is currently $CLUSTER_SIZE")
			} else {
				CLUSTER_SIZE = newClusterSize
				println("cluster size is now $CLUSTER_SIZE")
			}
		} else if (inputString.startsWith("interval")) {
			if (!inputString.startsWith("interval ")) {
				println("interval is currently $INTERVAL")
				continue
			}
			val newInterval = inputString.removePrefix("interval ").toIntOrNull()
			if (newInterval == null) {
				println("invalid number, interval is currently $INTERVAL")
			} else {
				INTERVAL = newInterval
				println("interval is now $INTERVAL")
			}
		} else if (inputString == "q" || inputString == "exit") {
			return
		} else if (inputString == "help" || inputString == "man") {
			printHelpMessage()
		} else if (inputString != "" && inputString != "start") {
			println("unknown command, `help` can be used to display a list of commands")
		}
	} while (inputString != "start")

	val root = FileTree.start(path)
	root.printFiles()
	var currentNode = root

	do {
		print(" > ")
		inputString = readLine() ?: "exit"
		if (inputString == "help" || inputString == "man") {
			printHelpMessage()
		} else if (inputString.startsWith("cd ")) {
			val dirName = inputString.removePrefix("cd ")
			if (dirName == "..") {
				currentNode = currentNode.parent ?: run {
					println("already at the root node.")
					currentNode
				}
				currentNode.printFiles()
				continue
			}
			val nextNode = currentNode.dirs.find { it.name.startsWith(dirName) }
			if (nextNode == null) {
				println("no directory found.")
			} else {
				currentNode = nextNode
				currentNode.printFiles()
			}
		} else if (inputString.startsWith("rm ")) {
			val fileName = inputString.removePrefix("rm ")
			if (currentNode.dirs.any { fileName == it.name }) {
				for (i in currentNode.dirs.indices) {
					if (currentNode.dirs[i].name == fileName) {
						if (currentNode.deleteFolder(i)) {
							println("$fileName/ deleted successfully")
						} else {
							println("an error occurred while trying to delete $fileName/")
						}
						break
					}
				}
			} else if (currentNode.files.any { it.name == fileName }) {
				for (i in currentNode.files.indices) {
					if (currentNode.files[i].name == fileName) {
						if (currentNode.deleteFile(i)) {
							println("$fileName deleted successfully")
						} else {
							println("an error occurred while trying to delete $fileName")
						}
						break
					}
				}
			} else {
				println("no file or directory found.")
			}
		} else if (inputString == "root") {
			currentNode = root
		} else if (inputString != "" && !inputString.startsWith("exit") && inputString != "q") {
			println("unknown command, `help` can be used to display a list of commands")
		}
	} while (!inputString.startsWith("exit") && inputString != "q")
}

fun printHelpMessage() {
	println("""
		//always available
		help / man               := brings up this message
		exit / q                 := exits the program
		
		//available before the file tree has been created
		size <new size>          := sets the cluster size used for estimating physical file size
		interval <file interval> := sets how frequently the number of files discovered is printed while creating the virtual file tree
		start                    := starts creating the virtual file tree
		
		//available after the file tree has been created
		cd <folder name>         := changes directory 
		rm <file or folder name> := deletes the file or folder in the file system and the virtual file tree
		root                     := changes the "working directory" to the root of the virtual file tree (true working directory)
		
	""".trimIndent())
}